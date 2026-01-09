package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;

public class ConnectionsImpl<T> implements Connections<T> {
    private final Map<Integer, ConnectionHandler<T>> activeClients = new ConcurrentHashMap<>();
    private final Map<String, Set<Integer>> channelSubscriptions = new ConcurrentHashMap<>();

    
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        if (handler != null) {
            handler.send(msg); 
            return true;
        }
        return false;
    }

  
    public void send(String channel, T msg) {
        Set<Integer> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            for (Integer id : subscribers) {
                send(id, msg);
            }
        }
    }

    
    public void disconnect(int connectionId) {
        activeClients.remove(connectionId);
        
    }
    
    
    public void addClient(int connectionId, ConnectionHandler<T> handler) {
        activeClients.put(connectionId, handler);
    }
}