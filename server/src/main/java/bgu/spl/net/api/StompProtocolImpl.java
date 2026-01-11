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
        String receiptID=null;
        boolean frameHasReceipt=false;
        for(String line:lines){
            if(line.startsWith("receipt:")){
                frameHasReceipt=true;
                receiptID=line.substring(8).trim();
                break;
            }    
        }
        String command = lines[0].trim();
        switch (command) {
            case "CONNECT":
                if(!frameHasReceipt){
                    if(lines.length!=6){
                        HandleError("Wrong format- frame Has less/more lines than needed");
                        return;
                    }
                }
                else{
                    if(lines.length!=7){
                        HandleError("Wrong format- frame Has less/more lines than needed");
                        return;
                    }
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
                    if(!frameHasReceipt){
                        String response = "CONNECTED\nversion:1.2\n\n\u0000";  //actual response to client
                        connections.send(connectionId, response);
                    }
                    else{
                        String response = "CONNECTED\nversion:1.2\n\n\u0000";  //actual response to client
                        String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n\n\u0000";
                        connections.send(connectionId, response);
                        connections.send(connectionId, receiptResponse);
                    }
                    
                } 
                else if (status == LoginStatus.WRONG_PASSWORD) {
                    HandleError("Wrong password!");
                    return;
                }
                else if (status == LoginStatus.ALREADY_LOGGED_IN) {
                    HandleError("User already logged in!");
                    return;
                } 
                else {
                    HandleError("Login failed!");
                    return;
                }
                
                break;

            case "SEND":    ///////////////////////////////////////////////////////////////////////////////send
                if(!frameHasReceipt){
                    if(lines.length<4)
                        HandleError("Missing lines on SEND frame");
                    if(!lines[1].startsWith("destination:/topic/")){
                        HandleError("Missing or wrong destination header");
                        return;
                    }
                    if(lines[2].length()!=0){
                        HandleError("Wrong format- frame missing an empty line between header and body!");
                        return;
                    }      
                }
                else{
                    if(lines.length<5)
                        HandleError("Missing lines on SEND frame");
                    if(!(lines[1].startsWith("destination:/topic/")&&lines[2].startsWith("receipt:"))
                        ||lines[1].startsWith("receipt:")&&lines[2].startsWith("destination:/topic/")){
                        HandleError("Wrong header/s for SEND frame!");
                        return;
                    }
                }
                
                int j=1;
                if(lines[2].startsWith("destination:/topic/")){
                    j=2;
                }
                     
                String destination= lines[j].substring(19).trim();
                if(destination.length()==0){
                    HandleError("Missing a topic!");
                }
                int validity=((ConnectionsImpl)connections).topicContainsUniqID(destination,connectionId);
                if(validity==-1){
                    HandleError("Given topic does not exist!");
                    return;

                }
                if (validity==0){
                    HandleError("Given user isnt subscribed to given topic!");    
                    return;
                } 
                String toSend=null;
                if(!frameHasReceipt){
                    int firstLiner=message.indexOf('\n');
                    int secondLiner=message.substring(firstLiner+1).indexOf('\n');
                    int thirdLiner=message.substring(secondLiner+1).indexOf('\n');  //a way to find the body start index
                    toSend=message.substring(thirdLiner+1); 
                
                }
                else{
                    int firstLiner=message.indexOf('\n');
                    int secondLiner=message.substring(firstLiner+1).indexOf('\n');
                    int thirdLiner=message.substring(secondLiner+1).indexOf('\n');  //a way to find the body start index
                    int fourthLiner=message.substring(thirdLiner+1).indexOf('\n');
                    toSend=message.substring(fourthLiner+1); 
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n\n\u0000";
                
                }
                    
                    //MUST ADD SUBSCRIBE ID TO STOMP!!!!!!!!!!!!!!!!!
                
                break;
                //todo if sub has recepit gotta send him the receipt
            case "SUBSCRIBE":      //////////////////////////////////////////////////////////////SUBSCRIBE
                String Sub_id=null;
                String topic=null;
                boolean hasEmptyLine=false;
                if(!frameHasReceipt){
                    if(lines.length!=4){
                        HandleError("Wrong subscribe format- to many/not enough lines!");
                        return;
                    }
                }   
                else if(lines.length!=5){
                    HandleError("Wrong subscribe format- to many/not enough lines!");
                    return;
                } 
                for(int i=1;i<lines.length;i++){
                    if(lines[i].startsWith("destination:/topic/")){
                        topic=lines[i].substring(19).trim();
                    }
                    else if (lines[i].startsWith("id:")){
                        Sub_id=lines[i].substring(3).trim();
                    }
                    else if(lines[i].isEmpty()){
                        hasEmptyLine=true;
                    }
                }
                if(!hasEmptyLine){
                    HandleError("Wrong frame format-missing an empty line!");
                    return;
                }
                if(Sub_id==null || topic==null){
                    HandleError("Missing ID or Destination headers");
                    return;
                }
                boolean isNumeric = Sub_id.chars().allMatch(Character::isDigit);
                if(!isNumeric){
                    HandleError("ID must contain only numbers!");
                    return;
                }
                int subID_num = Integer.parseInt(Sub_id);
                int topicID_isUnique=(((ConnectionsImpl)connections).topicContainsSubID(topic,subID_num));
                if(topicID_isUnique==1){
                    HandleError("Someone with this ID already subscribed to this topic!");
                    return;
                }
                int alreadySubscribed=((ConnectionsImpl)connections).topicContainsUniqID(topic,subID_num);
                if(alreadySubscribed==1){
                    HandleError("Client already subscribed to the channel!");
                    return;
                }
                 Subscriber sub=new Subscriber(connectionId, subID_num);
                ((ConnectionsImpl)connections).addClientToTopic(sub,topic);
                if(frameHasReceipt){
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n\n\u0000";
                    connections.send(connectionId, receiptResponse);
                }
                break;








            case "UNSUBSCRIBE":
                break;
            case "DISCONNECT":
                break;

            default:   
                HandleError("Invalid Command!"); 
                break;



            }
        }




    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }


    public void HandleError(String s){}
}