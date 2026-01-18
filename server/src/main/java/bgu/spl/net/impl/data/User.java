package bgu.spl.net.impl.data;
import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArraySet;
public class User {
	public final String name;
	public final String password;
	private int connectionId;
	private boolean isLoggedIn = false;
	private CopyOnWriteArraySet<Subscriber> mySubs=new CopyOnWriteArraySet<>();

	public User(int connectionId, String name, String password) {
		this.connectionId = connectionId;
		this.name = name;
		this.password = password;
	}

	public boolean isLoggedIn() {
		return isLoggedIn;
	}

	public void login() {
		isLoggedIn = true;
	}

	public void logout() {
		isLoggedIn = false;
	}

	public int getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}

	public CopyOnWriteArraySet<Subscriber> getSubsList(){
		return mySubs;
	}
	public void addToSubsList(Subscriber sub){
		mySubs.add(sub);
	}
	public void removeSubFromList(int subID){
		for(Subscriber sub:mySubs){
			if (sub.getSubID()==subID){
				mySubs.remove(sub);
			}
		}
	}
	public void clearAllSubs(){ //only for disconnect
		for(Subscriber sub:mySubs){
			mySubs.remove(sub);
		}
	}
	public String getname(){
		return name;
	}

}
