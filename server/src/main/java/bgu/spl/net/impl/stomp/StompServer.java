package bgu.spl.net.impl.stomp;
import java.util.*;
public class StompServer {

    public static void main(String[] args) {
        // TODO: implement this
        String message="CONNECT\naccept-version:1.2\nhost:stomp.cs.bgu.ac.il\nlogin:papino\npassword:23kakino\n\n\u0000";
         String[] lines = message.split("\n");
         for(int i=0;i<lines.length;i++){
            System.out.println(lines[i]+ " next row");
         }
         
    }
}
