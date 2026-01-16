package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

import java.util.jar.Attributes.Name;

import javax.xml.crypto.Data;
import bgu.spl.net.impl.data.User;
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
             HandleError("GENERAL","Empty message",null,message);
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
                         HandleError(command,"Wrong format1- frame Has less/more lines than needed",receiptID,message);
                        return;
                    }
                }
                else{
                    if(lines.length!=7){
                         HandleError(command,"Wrong format 2- frame Has less/more lines than needed",receiptID,message);
                        return;
                    }
                }
                
                if (isConnected) {
                     HandleError(command,"User already logged in",receiptID,message);
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
                     HandleError(command,"Wrong format- Missing an empty line!",receiptID,message);
                    return;
                }

                if (!"1.2".equals(acceptVersion)) {
                     HandleError(command,"Wrong or missing accept-version!",receiptID,message);
                    return;
                }
                if (!"stomp.cs.bgu.ac.il".equals(host)) {
                     HandleError(command,"Wrong or missing host!",receiptID,message);
                    return;
                }
                if (username == null || password == null) {
                     HandleError(command,"Missing login or passcode headers!",receiptID,message);
                    return;
                }
                LoginStatus status = Database.getInstance().login(connectionId, username, password);
                if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY || status == LoginStatus.ADDED_NEW_USER) {
                    this.isConnected = true;
                    if(!frameHasReceipt){
                        String response = "CONNECTED\nversion:1.2\n";  //actual response to client
                        connections.send(connectionId, response);
                    }
                    else{
                        String response = "CONNECTED\nversion:1.2\n";  //actual response to client
                        String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n";
                        connections.send(connectionId, response);
                        connections.send(connectionId, receiptResponse);
                    }
                    
                } 
                else if (status == LoginStatus.WRONG_PASSWORD) {
                     HandleError(command,"Wrong password",receiptID,message);
                    return;
                }
                else if (status == LoginStatus.ALREADY_LOGGED_IN) {
                     HandleError(command,"User already logged in",receiptID,message);
                    return;
                } 
                else {
                     HandleError(command,"Login failed!",receiptID,message);
                    return;
                }
                
                break;

            case "SEND":    ///////////////////////////////////////////////////////////////////////////////send
            if(!isConnected){
                     HandleError(command,"User is not connected to the system!",receiptID,message);
                     return;
                }
                if(!frameHasReceipt){
                    if(lines.length<4){
                        HandleError(command,"Missing lines on SEND frame",receiptID,message);
                        return;
                    }
                    if(!lines[1].startsWith("destination:/")){
                         HandleError(command,"Missing or wrong destination header",receiptID,message);
                        return;
                    }
                    if(lines[2].length()!=0){
                         HandleError(command,"Wrong format- frame missing an empty line between header and body!",receiptID,message);
                        return;
                    }      
                }
                else{
                    if(lines.length<5){
                        HandleError(command,"Missing lines on SEND frame",receiptID,message);
                        return;
                    }
                       
                    if(!(lines[1].startsWith("destination:/")&&lines[2].startsWith("receipt:"))
                        ||lines[1].startsWith("receipt:")&&lines[2].startsWith("destination:/")){
                         HandleError(command,"Wrong header/s for SEND frame!",receiptID,message);
                        return;
                    }
                    if(lines[3].length()!=0){
                         HandleError(command,"Wrong format- frame missing an empty line between header and body!",receiptID,message);
                        return;
                    } 
                }
                
                int j=1;
                if(lines[2].startsWith("destination:/")){
                    j=2;
                }
                     
                String destination= lines[j].substring(13).trim();
                if(destination.length()==0){
                     HandleError(command,"Missing a topic!",receiptID,message);
                     return;
                }
                int validity=((ConnectionsImpl)connections).topicContainsUniqID(destination,connectionId);
                if(validity==-1){
                     HandleError(command,"Given topic does not exist!",receiptID,message);
                    return;

                }
                if (validity==0){ 
                     HandleError(command,"Given user isnt subscribed to given topic!",receiptID,message);   
                    return;
                } 
                String toSend=message.substring(message.indexOf("\n\n")+2);
                if(!frameHasReceipt){
                    connections.send(destination, toSend); // ConnectionsImpl will wrap toSend with MESSAGE text format
                }
                else{
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n";
                    connections.send(destination, toSend); // ConnectionsImpl will wrap toSend with MESSAGE text format
                    connections.send(connectionId, receiptResponse); 
                }
                break;

                
            case "SUBSCRIBE":      //////////////////////////////////////////////////////////////SUBSCRIBE
            if(!isConnected){
                     HandleError(command,"User is not connected to the system!",receiptID,message);
                     return;
                }
                String sub_id=null;
                String topic=null;
                boolean hasEmptyLine=false;
                if(!frameHasReceipt){
                    if(lines.length!=4){
                         HandleError(command,"Wrong subscribe format- to many/not enough lines!",receiptID,message);
                        return;
                    }
                }   
                else if(lines.length!=5){
                     HandleError(command,"Wrong subscribe format- to many/not enough lines!",receiptID,message);
                    return;
                } 
                for(int i=1;i<lines.length;i++){
                    if(lines[i].startsWith("destination:/")){
                        topic=lines[i].substring(13).trim();
                    }
                    else if (lines[i].startsWith("id:")){
                        sub_id=lines[i].substring(3).trim();
                    }
                    else if(lines[i].isEmpty()){
                        hasEmptyLine=true;
                    }
                }
                if(!hasEmptyLine){
                     HandleError(command,"Wrong frame format-missing an empty line!",receiptID,message);
                    return;
                }
                if(sub_id==null || topic==null){
                     HandleError(command,"Missing ID or Destination headers",receiptID,message);
                    return;
                }
                boolean isNumeric = sub_id.chars().allMatch(Character::isDigit);
                if(!isNumeric){
                     HandleError(command,"ID must contain only numbers!",receiptID,message);
                    return;
                }
                int sub_num = Integer.parseInt(sub_id);
                int alreadySubscribed=((ConnectionsImpl)connections).topicContainsUniqID(topic,sub_num);
                if(alreadySubscribed==1){
                     HandleError(command,"Client already subscribed to the channel!",receiptID,message);
                    return;
                }
                Subscriber sub=new Subscriber(connectionId, sub_num,topic);
                ((ConnectionsImpl)connections).addClientToTopic(sub,topic);
                Database.getInstance().getUserByConnectionId(connectionId).addToSubsList(sub);
                if(frameHasReceipt){
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n";
                    connections.send(connectionId, receiptResponse);
                }
                break;

            case "UNSUBSCRIBE":            //////////////////////////////////////////////////////////////////////UNSUB
                if(!isConnected){
                     HandleError(command,"User is not connected to the system!",receiptID,message);
                     return;
                }
                if(frameHasReceipt){
                    if(lines.length !=4){
                         HandleError(command,"Unsubscribe format invalid",receiptID,message);
                        return;
                    }
                    if(!(lines[1].startsWith("id:") && lines[2].startsWith("receipt:")) &&
                        !(lines[1].startsWith("receipt:") && lines[2].startsWith("id:"))){
                             HandleError(command,"Wrong headers for unsubscribe!",receiptID,message);
                            return;
                        }
                }
                else if(lines.length != 3){
                     HandleError(command,"Unsubscribe format invalid",receiptID,message);
                    return;
                }
                if(!(lines[1].startsWith("id:") && lines[2].isEmpty())){
                     HandleError(command,"Wrong headers for unsubscribe!",receiptID,message);
                    return;
                }
                int k = 1;
                if(lines[1].startsWith("receipt:")){
                    k = 2;
                }
                String subs_id = lines[k].substring(3).trim();
                if(subs_id.isEmpty()){
                     HandleError(command,"Invalid ID!",receiptID,message);
                    return;
                }
                boolean is_Numeric = subs_id.chars().allMatch(Character::isDigit);
                if(!is_Numeric){
                     HandleError(command,"Invalid ID!",receiptID,message);
                    return;
                }
                int subs_int = Integer.parseInt(subs_id);
                Database.getInstance().getUserByConnectionId(connectionId).removeSubFromList(subs_int);
                boolean success = ((ConnectionsImpl)connections).findAndRemoveSub(connectionId, subs_int);
                if(!success){
                     HandleError(command,"this user with given id is not subscribed to any topic",receiptID,message);
                    return;
                }
                if(frameHasReceipt){
                    String receiptResponse="RECEIPT\nreceipt-id:"+receiptID+"\n";
                    connections.send(connectionId, receiptResponse);
                    return;
                }
                break;
               
            case "DISCONNECT":                   ///////////////////////////////////////////////////////////////////////////////////////
                if(!isConnected){
                     HandleError(command,"User is not connected to the system!",receiptID,message);
                     return;
                }
                if(!lines[1].startsWith("receipt:")){
                     HandleError(command,"Disconnect frame must include receipt!",receiptID,message);
                    return;
                }
                if(lines.length!=3){
                     HandleError(command,"Invalid format - must be of required rows",receiptID,message);
                    return;
                }
                if(!lines[2].isEmpty()){
                    HandleError(command,"Invalid Command!",receiptID,message);
                    return;
                }
                String receipt_id = lines[1].substring(8).trim();
                String receiptResponse="RECEIPT\nreceipt-id:"+receipt_id+"\n";
                Database.getInstance().getUserByConnectionId(connectionId).clearAllSubs();
                connections.send(connectionId, receiptResponse);
                ((ConnectionsImpl)connections).disconnect(connectionId);
                Database.getInstance().logout(connectionId);
                shouldTerminate = true;
                break;

            default:   
            HandleError("WRONG COMMAND","Invalid Command!",receiptID,message);
            break;
        }
            
        }




    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }


    public void HandleError(String caseType,String err,String receiptID,String originalMSG){
        String toSend="ERROR\n";
        if(receiptID!=null){
            toSend=toSend+"\nreceipt-id: "+receiptID+"\n";
        }
        toSend=toSend+"message: "+err+"\n";
        toSend=toSend+"Error Came from COMMAND: "+caseType+"\n---------------\nOriginal message sent:\n"+originalMSG+"\n"+"---------------";
        connections.send(connectionId, toSend);
        connections.disconnect(connectionId);
        if(isConnected){
            Database.getInstance().logout(connectionId);
        }
        isConnected=false;
        shouldTerminate=true;
    }
    
}