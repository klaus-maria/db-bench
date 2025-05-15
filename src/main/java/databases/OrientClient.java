package databases;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.*;
import com.orientechnologies.orient.client.*;
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.impl.ODocument;

import java.util.Iterator;
import java.util.Map;

public class OrientClient implements Database {

    private final String host;
    private final String username;
    private final String password;
    private final String instance;
    OrientDB orientDB;
    ODatabaseDocument db;

    public OrientClient(String host, String username, String password, String instance){
        this.host = host;
        this.username = username;
        this.password = password;
        this.instance = instance;

    }
    @Override
    public void connect() {
        orientDB = new OrientDB(host, OrientDBConfig.defaultConfig());
        db = orientDB.open(instance, username, password);

    }

    @Override
    public void write(Map<String, Object> record) {
        try{
            db.activateOnCurrentThread();
            ODocument doc = new ODocument("data");
            record.forEach(doc::field);
            db.save(doc);
            ORID rid = doc.getIdentity();
            System.out.println("Saved with RID: " + rid);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void read(QueryRecord query) {
        try{
            db.activateOnCurrentThread();
            switch (query.type()){
                case "find" -> db.query("SELECT * FROM data WHERE name=" + query.id());
                case "query" -> db.query("SELECT " + query.field() + " from data " + query.condition()); //sleect from data because data class is used to write to
                case "aggregate" -> db.query("SELECT count(" + query.field() + ") from data " + query.condition());
                default -> throw new IllegalArgumentException("Invalid Query Type");
            }
            System.out.println("query " + query.type() + " performed on orient");
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void disconnect() {
        db.close();
        System.out.println("disconnected from orient");
    }
}
