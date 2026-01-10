package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.data.Subscriber;

import java.util.Map;
import java.util.Set;

public class ConnectionsImpl<T> implements Connections<T> {
    private final Map<Integer, ConnectionHandler<T>> activeClients = new ConcurrentHashMap<>();
    private final Map<String, Set<Subscriber>> channelSubscriptions = new ConcurrentHashMap<>();

    
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        if (handler != null) {
            handler.send(msg); 
            return true;
        }
        return false;
    }

  
    public void send(String channel, T msg) {
        Set<Subscriber> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            for (Subscriber sub : subscribers) {
                send(sub.getUniqID(), msg);
            }
        }
    }

    
    public void disconnect(int connectionId) {
        activeClients.remove(connectionId);
        for (Set<Subscriber> subscribers : channelSubscriptions.values()) {
            for(Subscriber sub:subscribers){
                if (sub.getUniqID()==connectionId){   //had to, because of the stamp of the function
                    subscribers.remove(sub);
                    break;
                }
            }
            
        }
        
    }
    
    public int newUniqID(){
        int id=(int)(Math.random()*100000000);  
        while(activeClients.containsKey(id));
            id=(int)(Math.random()*100000000);
        return id;
    }
    
    public void addClient(int connectionId, ConnectionHandler<T> handler) {
        activeClients.put(connectionId, handler);
    }

    public int subContains(String sub,int connectionID){
        if(!channelSubscriptions.containsKey(sub)){
            return -1;          //this sub channel does not exists.
        }   
        Set<Subscriber> set = channelSubscriptions.get(sub);    //sub exists, now checks for client
        for(Subscriber cur:set){       
            if(cur.getUniqID()==connectionID){
                return 1;
            }
        }
        return 0;
        
        }

    
}