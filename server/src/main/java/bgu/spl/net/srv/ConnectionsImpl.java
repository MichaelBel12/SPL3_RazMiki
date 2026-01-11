package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.data.Subscriber;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ConnectionsImpl<T> implements Connections<T> {
    private final Map<Integer, ConnectionHandler<T>> activeClients = new ConcurrentHashMap<>();
    private final Map<String, LinkedList<Subscriber>> channelSubscriptions = new ConcurrentHashMap<>();

    
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        if (handler != null) {
            handler.send(msg); 
            return true;
        }
        return false;
    }

  
    public void send(String channel, T msg) {
        LinkedList<Subscriber> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            for (Subscriber sub : subscribers) {
                send(sub.getUniqID(), msg);
            }
        }
    }

    
    public void disconnect(int connectionId) {
        activeClients.remove(connectionId);
        for (LinkedList<Subscriber> subscribers : channelSubscriptions.values()) {
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
    
    public void addClientToActiveClients(int connectionId, ConnectionHandler<T> handler) {
        activeClients.put(connectionId, handler);
    }

    public void addClientToTopic(Subscriber sub,String channel){
        if(channelSubscriptions.containsKey(channel)){
            channelSubscriptions.get(channel).add(sub);
            return;
        }
        channelSubscriptions.put(channel,new LinkedList<Subscriber>());
        channelSubscriptions.get(channel).add(sub);
    }

    public int topicContainsUniqID(String topic,int connectionID){
        if(!channelSubscriptions.containsKey(topic)){
            return -1;          //this topic does not exists.
        }   
        LinkedList<Subscriber> linkedList = channelSubscriptions.get(topic);  
        for(Subscriber cur:linkedList){       
            if(cur.getUniqID()==connectionID){
                return 1;         //topic exists, also contain client
            }
        }
        return 0;    //topic exists, doesnt contain client
        
        }

        public int topicContainsSubID(String topic,int SubID){
        if(!channelSubscriptions.containsKey(topic)){
            return -1;          //this topic channel does not exists.
        }   
        LinkedList<Subscriber> linkedList = channelSubscriptions.get(topic);   
        for(Subscriber cur:linkedList){       
            if(cur.getSubID()==SubID){
                return 1;       //topic exists, but also has the client with this ID
            }
        }
        return 0;      //topic exists, client doesnt
        
        }

    
}