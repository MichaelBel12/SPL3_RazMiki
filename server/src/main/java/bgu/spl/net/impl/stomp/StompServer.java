package bgu.spl.net.impl.stomp;
import java.util.*;
public class StompServer {

    public static void main(String[] args) {
        // TODO: implement this
        String message="SEND\ndestination:/topic/123\n\nRazx stared at the golden swirl of hummus, its olive oil glistening like a desert oasis. He scooped a massive glob with warm pita, the creamy chickpeas melting on his tongue. Tahini richness and garlic tang exploded in every bite. Razx smiled, belly full, finally at peace with the world.";
        int firstLiner=message.indexOf('\n');
                    int secondLiner=message.substring(firstLiner+1).indexOf('\n');
                    int thirdLiner=message.substring(secondLiner+1).indexOf('\n');
                    String toSend=message.substring(thirdLiner+1);
                    System.out.println(toSend);
         
         }
         
    }

