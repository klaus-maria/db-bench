import org.apache.commons.cli.*;
public class DBBench {
    public static void main(String[] args) throws Exception {

        // CLI Argumente
        Options options = new Options();
        options.addOption("db", true, "Database type (e.g., mongo, couchbase, orientdb)");
        options.addOption("host", true, "Database host");
        options.addOption("port", true, "Database port");
        options.addOption("username", true, "Database username");
        options.addOption("password", true, "Database password");
        options.addOption("threads", true, "Number of threads");
        options.addOption("records", true, "Number of mock records");
        options.addOption("workload", true, "Workload type (read, write, mixed)");
        options.addOption("output", true, "Benchmark Results File output path");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String dbType = cmd.getOptionValue("db");
        String host = cmd.getOptionValue("host", "localhost");
        int port = Integer.parseInt(cmd.getOptionValue("port", "27017"));
        int threads = Integer.parseInt(cmd.getOptionValue("threads", "1"));
        int records = Integer.parseInt(cmd.getOptionValue("records", "10000"));
        String workload = cmd.getOptionValue("workload", "mixed");
        String outputPath = cmd.getOptionValue("output", System.getProperty("user.home") + "/Desktop/DBBench");


        Database db;
        // füge datenbanken hinzu -> erstellt neuen Driver
        switch (dbType.toLowerCase()) {
            case "test" -> db = new TestDB();
            default -> throw new IllegalArgumentException("Unsupported DB: " + dbType);
        }

        BenchmarkRunner benchmark = new BenchmarkRunner(db, threads, records, workload, outputPath);
        benchmark.run();
    }
}
