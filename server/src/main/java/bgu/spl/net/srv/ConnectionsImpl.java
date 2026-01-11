package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import bgu.spl.net.impl.data.Subscriber;
import java.util.Map;
import java.util.LinkedList;

public class ConnectionsImpl<T> implements Connections<T> {
    private final Map<Integer, ConnectionHandler<T>> activeClients = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArraySet<Subscriber>> channelSubscriptions = new ConcurrentHashMap<>();
    private int message_id = 0;

    
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        if (handler != null) {
            handler.send(msg); 
            return true;
        }
        return false;
    }

  
    public void send(String channel, T msg) {
        CopyOnWriteArraySet<Subscriber> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            for (Subscriber sub : subscribers) {
                String newMsg = "MESSAGE\nsubscription:"+sub.getSubID()+"\nmessage-id:"+message_id+"\ndestination:/topic/"+channel+"\n\n"+msg+"\n\u0000";
                System.out.println("WWWWWWW"+newMsg);
                send(sub.getUniqID(), (T)newMsg);
                message_id++;
            }
        }
    }

    
    public void disconnect(int connectionId) {
        Subscriber mySub = null;
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        activeClients.remove(connectionId);
        for (CopyOnWriteArraySet<Subscriber> subscribers : channelSubscriptions.values()) {
            for(Subscriber sub:subscribers){
                if (sub.getUniqID()==connectionId){   // finding the subscriber object of connectionId
                    mySub = sub;
                    break;
                }
            }
        }
        if(mySub != null){
            LinkedList<String> myTopics = mySub.getTopics();
            for(String topic : myTopics){
                channelSubscriptions.get(topic).remove(mySub); // To save dear time, we iterate less channels.
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

    public void addClientToTopic(Subscriber sub,String topic){
        if(channelSubscriptions.containsKey(topic)){
            channelSubscriptions.get(topic).add(sub);
            sub.addTopic(topic); //adds topic to client personal list
            return;
        }
        channelSubscriptions.put(topic,new CopyOnWriteArraySet<Subscriber>());
        channelSubscriptions.get(topic).add(sub);
        sub.addTopic(topic);
    }

    public int topicContainsUniqID(String topic,int connectionID){
        if(!channelSubscriptions.containsKey(topic)){
            return -1;          //this topic does not exists.
        }   
        CopyOnWriteArraySet<Subscriber> set = channelSubscriptions.get(topic);  
        for(Subscriber cur:set){       
            if(cur.getUniqID()==connectionID){
                return 1;         //topic exists, also contain client
            }
        }
        return 0;    //topic exists, doesnt contain client    
    }

    public boolean findAndRemoveSub(Subscriber sub){
        LinkedList<String> myTopics = sub.getTopics();
        for(String topic : myTopics){
            if(channelSubscriptions.get(topic).contains(sub)){ //sub id is unique per client across all topics
                channelSubscriptions.get(topic).remove(sub);
                return true;
            }
        }
        return false;
    }

    public void disconnectDupe(int connectionId, T msg) { // DUPLICATE TO SEND DISCONNECT + SEND IN CORRECT ORDER
        Subscriber mySub = null;
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        activeClients.remove(connectionId);
        for (CopyOnWriteArraySet<Subscriber> subscribers : channelSubscriptions.values()) {
            for(Subscriber sub:subscribers){
                if (sub.getUniqID()==connectionId){   // finding the subscriber object of connectionId
                    mySub = sub;
                    break;
                }
            }
        }
        if(mySub != null){
            LinkedList<String> myTopics = mySub.getTopics();
            for(String topic : myTopics){
                channelSubscriptions.get(topic).remove(mySub); // To save dear time, we iterate less channels.
            }
        }
        handler.send(msg);
    }
}