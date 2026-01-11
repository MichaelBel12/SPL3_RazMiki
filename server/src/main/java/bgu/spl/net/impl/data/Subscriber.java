package bgu.spl.net.impl.data;

public class Subscriber {
private int uniqID;
private int subID;

public Subscriber(int Uniq,int sub){
    uniqID=Uniq;
    subID=sub;
}

public int getUniqID(){return uniqID;}

public int getSubID(){return subID;}


}
