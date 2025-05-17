# Database Benchmarking Tool (Java)

A command-line Java benchmarking tool for evaluating the performance of document-oriented databases like MongoDB, Couchbase, and OrientDB.

## Features

* Single- or multi-threaded benchmarking
* Pluggable database driver architecture (`Database` interface)
* 3 supported workloads: `write`, `read`, `mixed`
* Random write/read operation generation
* Set document size
* CSV export for all performance metrics
* Real-time CPU and memory usage tracking

---

## Currently Supported Databases

* **MongoDB**
* **Couchbase**
* **OrientDB**

Additional drivers can be implemented by extending the `Database` interface.

---

## Requirements

* Java 17+
* Gradle (wrapper included)

---

## How to Run

### 1. Clone and Build

```bash
git clone https://github.com/klaus-maria/db-bench.git
cd db-bench
./gradlew build
```

### 2. Run the Benchmark

```bash
java -jar build/libs/db-bench.jar \
  --db=couchbase \
  --host=localhost \
  --threads=2 \
  --records=10000 \
  --workload=write \
  --csv-output=results.csv
```

---

## CLI Arguments

| Argument       | Description                                         |
| -------------- | --------------------------------------------------- |
| `--db`         | Database type: `mongodb`, `couchbase`, `orientdb`   |
| `--host`       | Database server host (default: `localhost`)         |
| `--instance`   | Database name (default: `test`)                     |
| `--username`   | Database username (default: `test`)                 |
| `--password`   | Database password (default: `password`)             |
| `--threads`    | Number of concurrent threads (default: `1`)         |
| `--records`    | Total operations (default: `10000`)                 |
| `--maxSize`    | Document size in KB (default: `10`)                 |
| `--workload`   | Type: `write`, `read`, `mixed`                      |
| `--output`     | Path to write CSV results                           |

---

## Metrics Export

All benchmark metrics are saved to a CSV file, including:

* Timestamp
* Operation type
* Latency (ms)
* Total throughput
* CPU and RAM usage
* Document size
* Thread count

---

## Adding a New Database

1. Implement the `Database` interface.
2. Register the implementation in the `DBBench` class.

---

## License

MIT License — use freely for research and testing.

