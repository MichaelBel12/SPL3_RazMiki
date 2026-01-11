package bgu.spl.net.impl.data;

import java.util.LinkedList;

public class Subscriber {
    private int uniqID;
    private int subID;
    private LinkedList<String> myTopics = new LinkedList<>();
    public Subscriber(int Uniq,int sub){
        uniqID=Uniq;
        subID=sub;
    }

    public int getUniqID(){return uniqID;}

    public int getSubID(){return subID;}

    public void addTopic(String topic){
        myTopics.add(topic);
    }
    public LinkedList<String> getTopics(){
        return myTopics;
    }

    @Override
    public boolean equals(Object other){
        if(other instanceof Subscriber){
            if(((Subscriber)other).getSubID() == this.getSubID() && ((Subscriber)other).getUniqID() == this.getUniqID()){
                return true;
            }
        }
        return false;
    }

}
