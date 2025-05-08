import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        database.connect();
        database.loadTestData();

        for(int i = 0; i < threadCount; i++){
            executor.submit( () -> {
                for (int r = 0; r < recordCount / threadCount; r++) {
                    switch (workload.toLowerCase()) {
                        case "read" -> database.read();
                        case "write" -> database.write();
                        default -> {
                            if (new Random().nextBoolean()) database.read();
                            else database.write();
                        }
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
