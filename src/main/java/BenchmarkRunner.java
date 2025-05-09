import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.management.OperatingSystemMXBean;
import databases.Database;

import java.lang.runtime.ObjectMethods;
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
    private final boolean fixedRecordSize;
    private final int maxRecordSize;
    private final String workload;
    private final String outputPath;
    private List<SystemSnapshot> snapshots = new ArrayList<>();
    private double throughput = 0.0;
    AtomicLong opCount = new AtomicLong(0);
    private double runningTime = 0;
    private Queue<Long> latencies = new ConcurrentLinkedDeque<>();
    private List<Integer> documentSizes = new ArrayList<>();
    AtomicInteger currentDocID = new AtomicInteger(0);


    public BenchmarkRunner(Database database, int threadCount, int recordCount, boolean fixedRecordSize, int maxRecordSize, String workload, String outputPath) {
        this.database = database;
        this.threadCount = threadCount;
        this.recordCount = recordCount;
        this.fixedRecordSize = fixedRecordSize;
        this.maxRecordSize = maxRecordSize;
        this.workload = workload;
        this.outputPath = outputPath;
    }

    public void run() throws InterruptedException {
        // verwaltet asynchrone prozesse/threads. scheinbar best practice?
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean monitor = new AtomicBoolean(true);


        database.connect();

        //database.loadTestData(recordCount, fixedRecordSize, maxRecordSize); not needed?

        systemMonitoring(monitor);

        // jeder Thread stellt einen Nutzer dar, der Anfragen an die Datenbank schickt
        long startTime = System.nanoTime();
        for(int i = 0; i < threadCount; i++){
            executor.submit( () -> {
                for (int r = 0; r < recordCount / threadCount; r++) {
                    long opStart = System.nanoTime();
                    switch (workload.toLowerCase()) {
                        case "read" -> database.read();
                        case "write" -> database.write(generateDoc());
                        case "aggregate" -> database.aggregate();
                        case "mixed" -> {
                            // falls mixed workload, wähle zufällige operation
                            switch (new Random().nextInt(5)){
                                case 0 -> database.read();
                                case 1 -> database.write(generateDoc());
                                default -> database.aggregate();
                            }
                        }
                        default -> throw new IllegalArgumentException();
                    }
                    long latency = System.nanoTime() - opStart;
                    latencies.add(latency);
                    opCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        runningTime = (System.nanoTime() - startTime) / 1e9;
        monitor.set(false);
        throughput = opCount.get() / (runningTime / 1e9); // division durch 1e9 um auf Sekunden

        export();
    }

    private void systemMonitoring(AtomicBoolean running){
        Thread monitor = new Thread(() -> {
            OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            while(running.get()){
                double cpuLoad = os.getCpuLoad() * 100; // * 100 um auf prozent zu kommen
                double cpuLoadProcess = os.getProcessCpuLoad() * 100;
                long usedMemory = os.getTotalMemorySize() - os.getFreeMemorySize();
                long timestamp = System.currentTimeMillis();
                snapshots.add(new SystemSnapshot(timestamp, cpuLoad, cpuLoadProcess, usedMemory));
                try {
                    Thread.sleep(1000); // system nur jede sekunde checken
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        monitor.start();
    }

    private void export() {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("timestamp,cpu_load_percent,cpu_load_process_percent,used_memory_mb");
            for (SystemSnapshot snap : snapshots) {
                writer.printf("%d,%.2f,%.2f,%.2f%n",
                        snap.timestamp,
                        snap.cpuLoad,
                        snap.cpuProcessLoad,
                        snap.usedMemory / 1024.0 / 1024.0);
            }

            writer.println();
            writer.println("latency_ms");
            for (long latency : latencies) {
                writer.printf("%.3f%n", latency / 1e6);
            }

            writer.println();
            writer.println("summary");
            writer.println("operation_type,thread_count,total_throughput_ops_per_sec,running_time_sec,total_ops");
            writer.printf("%s,%d,%.2f,%.2f%n", workload, threadCount, throughput, runningTime);

            writer.flush();

            System.out.println("Metrics exported to " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map generateDoc(){
        Map<String, Object> document = new HashMap<>();
        Integer identifier = currentDocID.getAndIncrement();
        ObjectMapper objectMapper = new ObjectMapper();
        Random rand = new Random();
        // ein Feld mit ID um genaue Suche zu ermöglichen
        // ein Feld (immer selber Attributname) für aggregate operationen
        // ein Feld mit komplexerer Struktur
        // rest mit padding befüllt
        document.put("name", identifier);
        document.put("value", rand.nextInt());

        Map<String, Object> details = new HashMap<>();
        Object[] list = {"test".repeat(rand.nextInt(0,100))};
        details.put("ip", "192.168.123." +  rand.nextInt(0, 256));
        details.put("os", "windows");
        details.put("active", rand.nextBoolean());
        details.put("list", list);

        document.put("details", details);
        document.put("padding", null); // wird im vorhinein dazugetan um fehlende bytes korrekt zu berechnen
        // document auf gewünschte größe bringen
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(document);
            int difference = maxRecordSize * 1024 - jsonBytes.length; //convert maxrecordsize from Kb to b
            // füge padding hinzu um bei fixedRecordSize=true auf maxRecordSize zu kommen, oder beliebig viel bis fixedRecordSize=maxRecordSize
            List<Byte> padding = new ArrayList<>();
            while (difference > 0 && (fixedRecordSize || rand.nextBoolean())) {
                padding.add((byte) 0xa);
                difference -= 1;
            }
            document.put("padding", padding);
            return document;

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private record SystemSnapshot(long timestamp, double cpuLoad, double cpuProcessLoad, long usedMemory){};
}
