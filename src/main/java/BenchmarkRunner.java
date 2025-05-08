import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class BenchmarkRunner {

    private final Database database;
    private final int threadCount;
    private final int recordCount;
    private final String workload;
    private final String outputPath;

    public BenchmarkRunner(Database database, int threadCount, int recordCount, String workload, String outputPath) {
        this.database = database;
        this.threadCount = threadCount;
        this.recordCount = recordCount;
        this.workload = workload;
        this.outputPath = outputPath;
    }

    public void run() throws InterruptedException {
        // verwaltet asynchrone prozesse/threads. scheinbar best practice?
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // Concurreny-Safe variablen
        AtomicLong opCount = new AtomicLong(0);
        Queue<Long> latencies = new ConcurrentLinkedDeque<>();
        double throughput = 0.0;

        database.connect();
        database.loadTestData();

        // jeder Thread stellt einen Nutzer dar, der Anfragen an die Datenbank schickt
        long startTime = System.nanoTime();
        for(int i = 0; i < threadCount; i++){
            executor.submit( () -> {
                for (int r = 0; r < recordCount / threadCount; r++) {
                    long opStart = System.nanoTime();
                    switch (workload.toLowerCase()) {
                        case "read" -> database.read();
                        case "write" -> database.write();
                        default -> {
                            if (new Random().nextBoolean()) database.read();
                            else database.write();
                        }
                    }
                    long latency = System.nanoTime() - opStart;
                    latencies.add(latency);
                    opCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        long runningTime = System.nanoTime() - startTime;
        throughput = opCount.get() / (runningTime / 1e9); // division durch 1e9 um auf Sekunden
    }
}
