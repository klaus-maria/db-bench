package databases;

import com.couchbase.client.java.*;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;

import java.util.Collections;
import java.util.Map;

public class CouchbaseClient implements Database {

    private final String host;
    private final String username;
    private final String password;
    private final String instance;
    private Cluster cluster;
    private Bucket bucket;
    private Collection collection;

    public CouchbaseClient(String host, String username, String password, String instance){
        this.host = host;
        this.username = username;
        this.password = password;
        this.instance = instance;
    }

    @Override
    public void connect() {
        cluster = Cluster.connect(host, username, password);
        bucket = cluster.bucket(instance);
        collection = bucket.defaultCollection();
    }

    @Override
    public void write(Map<String, Object> record) {
        collection.insert(record.get("name").toString(), record);
    }

    @Override
    public void read(BenchmarkRunner.queryRecord query) {
        switch (query.type()){
            case "find" -> collection.get(query.id());
            case "query" -> cluster.query("select " + query.field() + " from " + instance + query.condition());
            case "aggregate" -> cluster.query("select count(" + query.field() + ") from " + instance + query.condition());
            default -> throw new IllegalArgumentException("Invalid Query Type");
        }
    }

    @Override
    public void disconnect() {
        cluster.disconnect();
    }
}
