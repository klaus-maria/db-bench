import databases.Database;
import databases.TestDB;
import org.apache.commons.cli.*;
public class DBBench {
    public static void main(String[] args) throws Exception {

        // CLI Argumente
        Options options = new Options();
        options.addOption("db", true, "databases.Database type (e.g., mongo, couchbase, orientdb)");
        options.addOption("host", true, "databases.Database host");
        options.addOption("port", true, "databases.Database port");
        options.addOption("username", true, "databases.Database username");
        options.addOption("password", true, "databases.Database password");
        options.addOption("threads", true, "Number of threads");
        options.addOption("records", true, "Number of mock records");
        options.addOption("fixedSize", true, "If size of records is fixed or variable");
        options.addOption("maxSize", true, "Maximum record size in KB");
        options.addOption("workload", true, "Workload type (read, write, mixed)");
        options.addOption("output", true, "Benchmark Results File output path");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String dbType = cmd.getOptionValue("db");
        String host = cmd.getOptionValue("host", "localhost");
        int port = Integer.parseInt(cmd.getOptionValue("port", "27017"));
        int threads = Integer.parseInt(cmd.getOptionValue("threads", "1"));
        int records = Integer.parseInt(cmd.getOptionValue("records", "10000"));
        boolean fixed = Boolean.getBoolean(cmd.getOptionValue("fixedSize", "false"));
        int maxSize = Integer.parseInt(cmd.getOptionValue("maxSize", "10"));
        String workload = cmd.getOptionValue("workload", "mixed");
        String outputPath = cmd.getOptionValue("output", System.getProperty("user.home") + "\\Desktop\\DBBench\\benchmark.csv");


        Database db;
        // füge datenbanken hinzu -> erstellt neuen Driver
        switch (dbType.toLowerCase()) {
            case "test" -> db = new TestDB();
            default -> throw new IllegalArgumentException("Unsupported DB: " + dbType);
        }
        System.out.printf("Params: %s%n", outputPath);
        BenchmarkRunner benchmark = new BenchmarkRunner(db, threads, records, fixed, maxSize, workload, outputPath);
        benchmark.run();
    }
}
