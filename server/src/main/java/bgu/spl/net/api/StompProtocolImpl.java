package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.impl.data.Database;
import bgu.spl.net.impl.data.LoginStatus;

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
        String[] lines = message.split("\n");
        if (lines.length < 1) {
            HandleError("Empty message");
            return;
        }
        String command = lines[0].trim();
        switch (command) {
            case "CONNECT":
                if(lines.length!=7){
                    HandleError("incorrect connect format");
                    return;
                }
                if (isConnected) {
                    HandleError("Already connected");
                    return;
                }
                if (!lines[1].trim().equals("accept-version:1.2")) {
                    HandleError("Missing or wrong accept-version:1.2");
                    return;
                }
                if (!lines[2].trim().equals("host:stomp.cs.bgu.ac.il")) {
                    HandleError("Missing or wrong host");
                    return;
                }
                String username = "";
                if (lines[3].startsWith("login:")) {
                    username = lines[3].substring(6).trim();                 // "login:" is 6 characters
                } else {
                    HandleError("Missing login header");
                    return;
                }
                String password = "";
                if (lines[4].startsWith("passcode:")) {
                    password = lines[4].substring(9).trim();                    // "passcode:" is 9 characters
                } else {
                    HandleError("Missing passcode header");
                    return;
                }
                LoginStatus status = Database.getInstance().login(connectionId, username, password);
                if (status == LoginStatus.LOGGED_IN_SUCCESSFULLY || status == LoginStatus.ADDED_NEW_USER) {
                    this.isConnected = true;
                    String response = "CONNECTED\nversion:1.2\n\n\u0000";
                    connections.send(connectionId, response);
                } else if (status == LoginStatus.WRONG_PASSWORD) {
                    HandleError("Wrong password");
                } else if (status == LoginStatus.ALREADY_LOGGED_IN) {
                    HandleError("User already logged in");
                } else {
                    HandleError("Login failed");
                }
                break;





            case "SEND":
                break;
            case "SUBSCRIBE":
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