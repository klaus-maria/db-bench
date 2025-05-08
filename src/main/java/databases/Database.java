package databases;

import java.util.Map;

public interface Database {

    void connect();
    void loadTestData(int recordcount);
    void write(Map<String, Object> record);
    Map<String, Object> read();
}
