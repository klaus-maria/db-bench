package databases;

import java.util.HashMap;
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
    public Map<String, Object> read() {
        System.out.println("read op");
        return new HashMap<>();
    }

    @Override
    public Object aggregate(){
        System.out.println("agg op");
        return new Object();
    }


}
