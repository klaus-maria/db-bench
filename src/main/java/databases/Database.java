package databases;

import java.util.ArrayList;
import java.util.Map;

public interface Database {

    // connect to database
    void connect();
    // write a document to database or update a document field
    void write(Map<String, Object> record);
    // queries for a document or value
    void read(BenchmarkRunner.queryRecord q);

    void disconnect();

}
