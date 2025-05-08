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

    public void run(){

    }
}
