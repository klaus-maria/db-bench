package databases;

import java.util.Map;

public class TestDB implements Database {

    @Override
    public void connect() {
        System.out.println("connecting to DB");
    }

    @Override
    public void write(Map<String, Object> record) {
        System.out.println(record.toString());
    }

    @Override
    public void read(QueryRecord q) {
        System.out.println(q.toString());
    }

    @Override
    public void disconnect() {
        System.out.println("disconnect");
    }


}
