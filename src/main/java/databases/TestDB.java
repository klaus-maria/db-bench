package databases;

import java.util.HashMap;
import java.util.Map;

public class TestDB implements Database {

    @Override
    public void connect() {

    }

    @Override
    public void loadTestData(int count, boolean fixedSize, int maxSize) {
        for(int i = 0; i < count; i++){
            System.out.println("inserted record");
        }
    }

    @Override
    public void write(Map<String, Object> record) {
        System.out.println(record.toString());
    }

    @Override
    public Map<String, Object> read() {
        return new HashMap<>();
    }

    @Override
    public Object aggregate(){
        return new Object();
    }


}
