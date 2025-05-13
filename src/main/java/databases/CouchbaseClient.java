package databases;

import com.couchbase.client.core.error.CouchbaseException;
import com.couchbase.client.java.*;
import com.couchbase.client.java.json.JsonObject;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
        System.out.println("connected to couchbase");
    }

    @Override
    public void write(ObjectNode record) {
        try {
            collection.insert(record.get("name").toString(), record);
            System.out.println("document inserted to couchbase");
        } catch (CouchbaseException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void read(QueryRecord query) {
        switch (query.type()){
            case "find" -> collection.get(query.id());
            case "query" -> cluster.query("select " + query.field() + " from " + instance + query.condition());
            case "aggregate" -> cluster.query("select count(" + query.field() + ") from " + instance + query.condition());
            default -> throw new IllegalArgumentException("Invalid Query Type");
        }
        System.out.println("query performed on couchbase");
    }

    @Override
    public void disconnect() {
        cluster.disconnect();
        System.out.println("disconnected from couchbase");
    }
}
