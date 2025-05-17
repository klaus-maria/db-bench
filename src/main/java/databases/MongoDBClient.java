package databases;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;

import java.util.Arrays;
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
        System.out.println("connected to mongodb");
    }

    @Override
    public void write(Map<String, Object> record) {
        try {
            collection.insertOne(new Document(record));
            System.out.println("document inserted to mongo");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void read(QueryRecord query) {
        try{
            switch (query.type()){
                case "find" -> collection.find(eq("name", query.id())).first();
                case "query" -> {
                    Bson filter = Filters.and(Filters.lt("someValue", 1400), Filters.gt("someValue", -567));
                    collection.find(filter);
                }
                case "aggregate" -> collection.aggregate(Arrays.asList(
                        Aggregates.match(Filters.eq("details.active", true)),
                        Aggregates.project(new Document("listSize", new Document("$size", "$details.list")))
                ));
                default -> throw new IllegalArgumentException("Invalid Query Type");
            }
            System.out.println("query " + query.type() + " performed on mongo");
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        client.close();
        System.out.println("disconnected from mongodb");
    }
}
