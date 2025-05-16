package databases;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.Map;

public class MongoDBClient implements Database {

    private final String host;
    private final String username;
    private final String password;
    private final String instance;
    private MongoDatabase db;
    private MongoClient client;
    MongoCollection<Document> collection;

    public MongoDBClient(String host, String username, String password, String instance){
        this.host = host;
        this.username = username;
        this.password = password;
        this.instance = instance;
    }
    @Override
    public void connect() {
        client = MongoClients.create(host);
        db = client.getDatabase(instance);
        collection= db.getCollection("data");
    }

    @Override
    public void write(Map<String, Object> record) {

    }

    @Override
    public void read(QueryRecord q) {

    }

    @Override
    public void disconnect() {
        client.close();
    }
}
