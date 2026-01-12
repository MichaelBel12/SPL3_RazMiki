package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.Subscriber;

import java.util.LinkedList;
import java.util.Map;
import bgu.spl.net.impl.data.User;

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
                send(sub.getUniqID(), (T)newMsg);
                message_id++;
            }
        }
    }

    
    public void disconnect(int connectionId) {  //we're not using this anyways
       Subscriber mySub = null;
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        activeClients.remove(connectionId);
        for(CopyOnWriteArraySet<Subscriber> subscribers : channelSubscriptions.values()){    //removing my subs from all topics 
            for(Subscriber sub:subscribers){
                if(sub.getSubID()==connectionId){
                    subscribers.remove(sub);
                }
            }
        }
        User myUser = Database.getInstance().getUserByConnectionId(connectionId); //clearing the user's subscriptions list
        LinkedList<Subscriber> subsList = myUser.getSubsList();
        for(Subscriber sub:subsList){
            subsList.remove(sub);
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
            return;
        }
        channelSubscriptions.put(topic,new CopyOnWriteArraySet<Subscriber>());
        channelSubscriptions.get(topic).add(sub);
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

    public boolean findAndRemoveSub(int uniqID,int subID){ //for unsubscribe
        LinkedList<Subscriber> subsList = Database.getInstance().getUserByConnectionId(uniqID).getSubsList();
        String topic=null;
        for(Subscriber sub:subsList){
            if(sub.getSubID()==subID){
                topic=sub.getTopic();
                CopyOnWriteArraySet<Subscriber> set = channelSubscriptions.get(topic);
                set.remove(sub);
                return true;
            }
        
        }
        return false;
    }
        

    public void disconnectDupe(int connectionId, T msg) { // Wont useeee
        Subscriber mySub = null;
        ConnectionHandler<T> handler = activeClients.get(connectionId);
        activeClients.remove(connectionId);
        for(CopyOnWriteArraySet<Subscriber> subscribers : channelSubscriptions.values()){    //removing my subs from all topics 
            for(Subscriber sub:subscribers){
                if(sub.getSubID()==connectionId){
                    subscribers.remove(sub);
                }
            }
        }
        User myUser = Database.getInstance().getUserByConnectionId(connectionId); //clearing the user's subscriptions list
        LinkedList<Subscriber> subsList = myUser.getSubsList();
        for(Subscriber sub:subsList){
            subsList.remove(sub);
        }
        handler.send(msg);
    }


    public boolean userIsConnected(int id){
        return activeClients.containsKey(id);
    }

    public ConnectionHandler<T> getHandler(int id){
        return activeClients.get(id);
    }
}