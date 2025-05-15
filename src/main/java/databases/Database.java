package databases;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public interface Database {

    // connect to database
    void connect();
    // write a document to database or update a document field
    void write(Map<String, Object> record);
    // queries for a document or value
    void read(QueryRecord q);

    void disconnect();

}
