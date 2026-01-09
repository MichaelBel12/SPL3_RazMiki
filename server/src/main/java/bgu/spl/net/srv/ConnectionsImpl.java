package bgu.spl.net.srv;

public class ConnectionsImpl implements Connections<String> {

    @Override
    public boolean send(int connectionId, String msg) {
        // Implementation here
        return false;
    }

    @Override
    public void send(String channel, String msg) {
        // Implementation here
    }

    @Override
    public void disconnect(int connectionId) {
        // Implementation here
    }

}
