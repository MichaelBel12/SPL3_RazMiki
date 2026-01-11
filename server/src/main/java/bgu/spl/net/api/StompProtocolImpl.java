package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.LoginStatus;
import bgu.spl.net.impl.data.Subscriber;

public class StompProtocolImpl implements StompMessagingProtocol<String> {

    private int connectionId;
    private Connections<String> connections;
    private boolean shouldTerminate = false;
    private boolean isConnected = false;

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = (ConnectionsImpl)connections;
    }

    @Override
    public void process(String message) {
        // Split the message into lines so we can check them one by one
        String[] lines = message.split("\n");
        if (lines.length < 1) {
            HandleError("Empty message");
            return;
        }
        String command = lines[0].trim();
        switch (command) {
            case "CONNECT":
                if(lines.length!=6){
                    HandleError("Wrong format- frame Has less/more lines than needed");
                    return;
                }
                if (isConnected) {
                    HandleError("User is already connected!");
                    return;
                }
                String acceptVersion = null;
                String host = null;
                String username = null;
                String password = null;
                int emptylines=0;
                for (int i = 1; i < lines.length; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()){
                        emptylines++;
                        break;
                    }  
                    if (line.startsWith("accept-version:")) {
                        acceptVersion = line.substring(15).trim();
                    } else if (line.startsWith("host:")) {
                        host = line.substring(5).trim();
                    } else if (line.startsWith("login:")) {
                        username = line.substring(6).trim();
                    } else if (line.startsWith("passcode:")) {
                        password = line.substring(9).trim();
                    }
                }
                if(emptylines!=1){
                    HandleError("Wrong format- Missing an empty line!");
                    return;
                }

                if (!"1.2".equals(acceptVersion)) {
                    HandleError("Wrong or missing accept-version!");
                    return;
                }
                if (!"stomp.cs.bgu.ac.il".equals(host)) {
                    HandleError("Wrong or missing host!");
                    return;
                }
                if (username == null || password == null) {
                    HandleError("Missing login or passcode headers!");
                    return;
                }
                LoginStatus status = Database.getInstance().login(connectionId, username, password);
                if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY || status == LoginStatus.ADDED_NEW_USER) {
                    this.isConnected = true;
                    String response = "CONNECTED\nversion:1.2\n\n\u0000";  //actual response to client
                    connections.send(connectionId, response);
                } else if (status == LoginStatus.WRONG_PASSWORD) {
                    HandleError("Wrong password!");
                    return;
                } else if (status == LoginStatus.ALREADY_LOGGED_IN) {
                    HandleError("User already logged in!");
                    return;
                } else {
                    HandleError("Login failed!");
                    return;
                }
                break;

            case "SEND":
                if(!lines[1].startsWith("destination:/topic/")){
                    HandleError("Missing or wrong destination header");
                    return;
                }
                if(lines[2].length()!=0){
                     HandleError("Wrong format- frame missing an empty line between header and body!");
                     return;
                }
                   
                String destination= lines[1].substring(19).trim();
                int validity=((ConnectionsImpl)connections).subContains(destination,connectionId);
                if(validity==-1){
                    HandleError("Given Subscription Channel does not exist!");
                    return;

                }
                if (validity==0){
                    HandleError("Given user isnt subscribed to given Channel!");    
                    return;
                }  
                
                else{
                    int firstLiner=message.indexOf('\n');
                    int secondLiner=message.substring(firstLiner+1).indexOf('\n');
                    int thirdLiner=message.substring(secondLiner+1).indexOf('\n');  //a way to find the body start index
                    String toSend=message.substring(thirdLiner+1); 
                    connections.send(destination, toSend); 
                    //to add: what the servers sends to all the sub's clients from this message.
                }
                break;

            case "SUBSCRIBE":
                String id=null;
                String topic=null;
                boolean hasEmptyLine=false;
                if(lines.length!=3){
                    HandleError("Wrong subscribe format- to many/not enough lines!");
                    return;
                }
                for(String line:lines){
                    if(line.startsWith("destination/topic/")){
                        topic=line.substring(9).trim();
                    }
                    else if (line.startsWith("id:")){
                        id=line.substring(3).trim();
                    }
                    else if(line.isEmpty()){
                        hasEmptyLine=true;
                    }
                }
                if(!hasEmptyLine){
                    HandleError("Wrong frame format-missing an empty line!");
                    return;
                }
                if(id==null || topic==null){
                    HandleError("Missing ID or Destination headers");
                    return;
                }
                boolean isNumeric = id.chars().allMatch(Character::isDigit);
                if(!isNumeric){
                    HandleError("ID must contain only numbers!");
                }
                int id_num = Integer.parseInt(id);
                Subscriber sub=new Subscriber(connectionId, id_num);
                //to continue
             


                break;
            case "UNSUBSCRIBE":
                break;
            case "DISCONNECT":
                break;





            }
        }




    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }


    public void HandleError(String s){}
}