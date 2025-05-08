public interface Database {

    void connect();
    void loadTestData();
    void write();
    void read();
}
