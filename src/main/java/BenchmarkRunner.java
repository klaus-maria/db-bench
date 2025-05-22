import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.management.OperatingSystemMXBean;
import databases.Database;
import databases.QueryRecord;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BenchmarkRunner {

    private final Database database;
    private final int threadCount;
    private final int recordCount;
    private final int maxRecordSize;
    private final String workload;
    private final String outputPath;
    private List<SystemSnapshot> snapshots = new ArrayList<>();
    private double throughput = 0.0;
    AtomicLong opCount = new AtomicLong(0);
    private double runningTime = 0;
    private Queue<Long> latencies = new ConcurrentLinkedDeque<>();
    AtomicInteger currentDocID = new AtomicInteger(0);


    public BenchmarkRunner(Database database, int threadCount, int recordCount, int maxRecordSize, String workload, String outputPath) {
        this.database = database;
        this.threadCount = threadCount;
        this.recordCount = recordCount;
        this.maxRecordSize = maxRecordSize;
        this.workload = workload;
        this.outputPath = outputPath;
    }

    public void run() throws InterruptedException {
        // verwaltet asynchrone prozesse/threads. scheinbar best practice?
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean monitor = new AtomicBoolean(true);


        database.connect();
        Thread.sleep(1000);

        systemMonitoring(monitor);

        // jeder Thread stellt einen Nutzer dar, der Anfragen an die Datenbank schickt
        long startTime = System.nanoTime();
        for(int i = 0; i < threadCount; i++){
            executor.submit( () -> {
                for (int r = 0; r < recordCount / threadCount; r++) {
                    long opStart = System.nanoTime();
                    switch (workload.toLowerCase()) {
                        case "read" -> database.read(generateSearch());
                        case "write" -> database.write(generateDoc());
                        case "mixed" -> {
                            if ((new Random().nextInt(0, 2) == 1)) {
                                database.write(generateDoc());
                            } else {
                                database.read(generateSearch());
                            }
                        }
                        // falls mixed workload, wähle zufällige operation
                        default -> throw new IllegalArgumentException();
                    }
                    long latency = System.nanoTime() - opStart;
                    latencies.add(latency);
                    opCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.HOURS);

        runningTime = (System.nanoTime() - startTime) / 1e9;
        monitor.set(false);
        throughput = opCount.get() / (runningTime / 1e9); // division durch 1e9 um auf Sekunden

        export();
    }

    private record SystemSnapshot(long timestamp, double cpuLoad, long usedMemory){};

    private void systemMonitoring(AtomicBoolean running){
        Thread monitor = new Thread(() -> {
            System.out.println("start monitor");
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            while(running.get()){
                double cpuLoad = os.getCpuLoad() * 100; // * 100 um auf prozent zu kommen
                long usedMemory = os.getTotalMemorySize() - os.getFreeMemorySize();
                long timestamp = System.currentTimeMillis();
                snapshots.add(new SystemSnapshot(timestamp, cpuLoad, usedMemory));
                try {
                    Thread.sleep(500); // system nur jede halbe sekunde checken
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        monitor.start();
    }

    private void export() {
        try (PrintWriter writer = new PrintWriter(outputPath)) {

            writer.println("database, workload, thread_count, cpu_load_avg_percent, memory_avg_mb, latency_avg_ms, throughput_ops_per_sec, running_time_sec, total_ops");

            double avg_cpu = snapshots.stream().mapToDouble(SystemSnapshot::cpuLoad).average().orElse(0.0);
            double avg_mem = snapshots.stream().mapToDouble(SystemSnapshot::usedMemory).average().orElse(0.0) /1024.0 /1024.0; //convert to mb
            double avg_latency = latencies.stream().mapToDouble(Long::doubleValue).average().orElse(0.0) / 1e6;
            String db = database.getClass().toString();

            writer.printf("%s, %s, %d, %.2f, %.2f, %.2f, %.2f, %.2f, %d", db, workload, threadCount, avg_cpu, avg_mem, avg_latency, throughput, runningTime, opCount.get());
            writer.flush();

            System.out.println("Metrics exported to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> generateDoc(){
        Integer identifier = currentDocID.getAndIncrement();

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode document = objectMapper.createObjectNode();
        Random rand = new Random();
        // ein Feld mit ID um genaue Suche zu ermöglichen
        // ein Feld (immer selber Attributname) für aggregate operationen
        // ein Feld mit komplexerer Struktur
        // rest mit padding befüllt um document auf gewünschte größe bringen
        try {
            document.put("name", identifier);
            document.put("someValue", rand.nextInt());

            ObjectNode details = objectMapper.createObjectNode();
            String list = "[" + "\"test\",".repeat(rand.nextInt(50,100)) + " \"test\"]";
            JsonNode listJSON = objectMapper.readTree(list);
            details.put("ip", "192.168.123." +  rand.nextInt(0, 256));
            details.put("os", "windows");
            details.put("active", rand.nextBoolean());
            details.set("list", listJSON);

            document.set("details", details);
            document.set("padding", null);
            byte[] jsonBytes = objectMapper.writeValueAsBytes(document);
            int difference = (maxRecordSize * 1000) - jsonBytes.length; //convert maxrecordsize from Kb to b
            short[] padding = new short[difference/4];

            // füge padding hinzu um bei fixedRecordSize=true auf maxRecordSize zu kommen

            Arrays.fill(padding, (short) 0xa);
            document.put("padding", Arrays.toString(padding));

            return objectMapper.convertValue(document, new TypeReference<Map<String, Object>>(){});

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private QueryRecord generateSearch() {
        switch(new Random().nextInt(0, 3)){
            // finde bestimmten wert von dokument wo aussage zutrifft
            case 0 -> {
                return new QueryRecord(null, "name", " WHERE someValue < 1400 AND someValue > -567", "query");
            }
            // berechne wert wo zutrifft
            case 1 -> {
                return new QueryRecord(null, "details.list", " WHERE details.active = true", "aggregate");
            }
            // finde dokument
            default -> {
                int bound = workload.equalsIgnoreCase("read") ? maxRecordSize : currentDocID.get();
                if(bound == 0) return new QueryRecord("0", null, null, "find"); //if bound == 0 -> IllegalArgumentException by Random
                return new QueryRecord(Integer.toString(new Random().nextInt(0, bound)), null, null, "find");
            }
        }
    }
}
