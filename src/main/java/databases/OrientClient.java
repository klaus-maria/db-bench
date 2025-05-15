package databases;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.*;
import com.orientechnologies.orient.client.*;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;

public class OrientClient implements Database {

    private final String host;
    private final String username;
    private final String password;
    private final String instance;

    public OrientClient(String host, String username, String password, String instance){
        this.host = host;
        this.username = username;
        this.password = password;
        this.instance = instance;

    }
    @Override
    public void connect() {
        OrientDB orientDB = new OrientDB(host, OrientDBConfig.defaultConfig());
        ODatabaseDocument db = orientDB.open(instance, username, password);

    }

    @Override
    public void write(ObjectNode record) {

    }

    @Override
    public void read(QueryRecord q) {

    }

    @Override
    public void disconnect() {

    }
}
