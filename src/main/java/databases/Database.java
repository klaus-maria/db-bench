package databases;

import java.util.ArrayList;
import java.util.Map;

public interface Database {

    // connect to database
    void connect();
    // load test data
    void loadTestData(int recordcount, boolean fixedSize, int maxSize);
    // write a document to database or update a document field
    void write(Map<String, Object> record);
    // retrieve a document or read a field
    Map<String, Object> read();
    // retrieve a calculated value derived from multiple documents
    Object aggregate();

}
