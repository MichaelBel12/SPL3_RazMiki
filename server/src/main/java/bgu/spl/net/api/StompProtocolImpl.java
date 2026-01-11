package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

import java.util.jar.Attributes.Name;

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
        this.connections = connections;
    }

    @Override
    public void process(String message) {
        // Split the message into lines so we can check them one by one
        String[] lines = message.split("\n", -1);
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
            if(line.isEmpty()){
                break;
            }    
        }
        String command = lines[0].trim();
        switch (command) {
            case "CONNECT":
                if(!frameHasReceipt){
                    System.out.println("LINES LENGTH: "+lines.length);
                    if(lines.length!=6){
                        HandleError("Wrong format1- frame Has less/more lines than needed");
                        return;
                    }
                }
                else{
                    if(lines.length!=7){
                        HandleError("Wrong format 2- frame Has less/more lines than needed");
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
                    if(lines[3].length()!=0){
                        HandleError("Wrong format- frame missing an empty line between header and body!");
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
                String toSend=message.substring(message.indexOf("\n\n")+2);
                if(!frameHasReceipt){
                    connections.send(destination, toSend); // ConnectionsImpl will wrap toSend with MESSAGE text format
                }
                else{
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n\n\u0000";
                    connections.send(destination, toSend); // ConnectionsImpl will wrap toSend with MESSAGE text format
                    connections.send(connectionId, receiptResponse); 
                }
                break;

                // #NOTE MUST CONSIDER A CASE WHERE USER ISNT EVEN CONNECTED!!
            case "SUBSCRIBE":      //////////////////////////////////////////////////////////////SUBSCRIBE
                String sub_id=null;
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
                        sub_id=lines[i].substring(3).trim();
                    }
                    else if(lines[i].isEmpty()){
                        hasEmptyLine=true;
                    }
                }
                if(!hasEmptyLine){
                    HandleError("Wrong frame format-missing an empty line!");
                    return;
                }
                if(sub_id==null || topic==null){
                    HandleError("Missing ID or Destination headers");
                    return;
                }
                boolean isNumeric = sub_id.chars().allMatch(Character::isDigit);
                if(!isNumeric){
                    HandleError("ID must contain only numbers!");
                    return;
                }
                int sub_num = Integer.parseInt(sub_id);
                int alreadySubscribed=((ConnectionsImpl)connections).topicContainsUniqID(topic,sub_num);
                if(alreadySubscribed==1){
                    HandleError("Client already subscribed to the channel!");
                    return;
                }
                Subscriber sub=new Subscriber(connectionId, sub_num);
                ((ConnectionsImpl)connections).addClientToTopic(sub,topic);

                if(frameHasReceipt){
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n\n\u0000";
                    connections.send(connectionId, receiptResponse);
                }
                break;








            case "UNSUBSCRIBE":
                if(frameHasReceipt){
                    if(lines.length !=4){
                        HandleError("Unsubscribe format invalid");
                        return;
                    }
                    if(!(lines[1].startsWith("id:") && lines[2].startsWith("receipt:")) ||
                        !(lines[1].startsWith("receipt:") && lines[2].startsWith("id:"))){
                            HandleError("Wrong headers for unsubscribe!");
                            return;
                        }
                }
                else if(lines.length != 3){
                    HandleError("Unsubscribe format invalid");
                    return;
                }
                if(!(lines[1].startsWith("id:") && lines[2].isEmpty())){
                    HandleError("Wrong headers for unsubscribe!");
                    return;
                }
                int k = 1;
                if(lines[1].startsWith("receipt:")){
                    k = 2;
                }
                String subs_id = lines[k].substring(3).trim();
                if(subs_id.isEmpty()){
                    HandleError("Invalid ID!");
                    return;
                }
                boolean is_Numeric = subs_id.chars().allMatch(Character::isDigit);
                if(!is_Numeric){
                    HandleError("Invalid ID!");
                    return;
                }
                int subs_int = Integer.parseInt(subs_id);
                Subscriber subsc = new Subscriber(connectionId, subs_int);
                boolean success = ((ConnectionsImpl)connections).findAndRemoveSub(subsc);
                if(!success){
                    HandleError("id is not subscribed to any topic");
                    return;
                }
                if(frameHasReceipt){
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n\n\u0000";
                    connections.send(connectionId, receiptResponse);
                    return;
                }

                
                break;
                // #NOTE MUST CONSIDER A CASE WHERE USER ISNT EVEN CONNECTED!!
            case "DISCONNECT":
                if(!lines[1].startsWith("receipt:")){
                    HandleError("Disconnect frame must include receipt!");
                    return;
                }
                if(lines.length!=3){
                    HandleError("Invalid format - must be of required rows");
                    return;
                }
                if(!lines[2].isEmpty()){
                    HandleError("Invalid format - must have empty line");
                }
                String receipt_id = lines[1].substring(8).trim();
                String receiptResponse="RECEIPT\nreceipt-id:"+receipt_id+"\n\n\u0000";
                ((ConnectionsImpl)connections).disconnectDupe(connectionId, receiptResponse);
                
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


    public void HandleError(String s){
        System.out.println("THIS IS THE MOTHER FUCKING ERROR: "+s);
    }
}