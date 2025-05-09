import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import databases.Database;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
    double runningTime = 0;
    Queue<Long> latencies = new ConcurrentLinkedDeque<>();
    private final List<Integer> documentSizes = new ArrayList<>();


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
        database.loadTestData(recordCount, fixedRecordSize, maxRecordSize);

        systemMonitoring(monitor);

        // jeder Thread stellt einen Nutzer dar, der Anfragen an die Datenbank schickt
        long startTime = System.nanoTime();
        for(int i = 0; i < threadCount; i++){
            executor.submit( () -> {
                for (int r = 0; r < recordCount / threadCount; r++) {
                    long opStart = System.nanoTime();
                    switch (workload.toLowerCase()) {
                        case "read" -> database.read();
                        case "write" -> database.write(Generator.generateRecord());
                        case "aggregate" -> database.aggregate();
                        case "mixed" -> {
                            // falls mixed workload, wähle zufällige operation
                            switch (new Random().nextInt(5)){
                                case 0 -> database.read();
                                case 1 -> database.write(Generator.generateRecord());
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

    private record SystemSnapshot(long timestamp, double cpuLoad, double cpuProcessLoad, long usedMemory){};
}
