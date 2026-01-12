package bgu.spl.net.impl.data;


public class Subscriber {
    private int uniqID;
    private int subID;
    private String myTopic;
    
    public Subscriber(int Uniq,int sub,String topic){
        uniqID=Uniq;
        subID=sub;
        myTopic=topic;
    }


    public int getUniqID(){return uniqID;}

    public int getSubID(){return subID;}

    public String getTopic(){return myTopic;}

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
