import databases.*;
import org.apache.commons.cli.*;
public class DBBench {
    public static void main(String[] args) throws Exception {

        // CLI Argumente
        Options options = new Options();
        options.addOption("db", true, "databases.Database type (e.g., mongo, couchbase, orientdb)");
        options.addOption("host", true, "databases.Database host");
        options.addOption("instance", true, "databases.Database instance");
        options.addOption("username", true, "databases.Database username");
        options.addOption("password", true, "databases.Database password");
        options.addOption("threads", true, "Number of threads");
        options.addOption("records", true, "Number of mock records");
        options.addOption("maxSize", true, "Maximum record size in KB");
        options.addOption("workload", true, "Workload type (read, write, mixed)");
        options.addOption("output", true, "Benchmark Results File output path");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        String dbType = cmd.getOptionValue("db");
        String host = cmd.getOptionValue("host", "127.0.0.1");
        String instance = cmd.getOptionValue("instance", "test");
        String username = cmd.getOptionValue("username", "test");
        String password = cmd.getOptionValue("password", "password");
        int threads = Integer.parseInt(cmd.getOptionValue("threads", "1"));
        int records = Integer.parseInt(cmd.getOptionValue("records", "10000"));
        int maxSize = Integer.parseInt(cmd.getOptionValue("maxSize", "10"));
        String workload = cmd.getOptionValue("workload", "mixed");
        String outputPath = cmd.getOptionValue("output", System.getProperty("user.home") + "\\Desktop\\" + dbType + workload + threads + ".csv");


        Database db;
        // füge datenbanken hinzu -> erstellt neuen Driver
        switch (dbType.toLowerCase()) {
            case "test" -> db = new TestDB();
            case "couchbase" -> db = new CouchbaseClient(host, username, password, instance);
            case "orient" -> db = new OrientClient(host, username, password, instance);
            case "mongo" -> db = new MongoDBClient(host, username, password, instance);
            default -> throw new IllegalArgumentException("Unsupported DB: " + dbType);
        }
        BenchmarkRunner benchmark = new BenchmarkRunner(db, threads, records, maxSize, workload, outputPath);
        benchmark.run();
        db.disconnect();
    }
}
