package bgu.spl.net.impl.stomp;
import java.util.function.Supplier;
import bgu.spl.net.api.StompEncDec;
import bgu.spl.net.api.StompProtocolImpl;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        // According to the PDF, args[0] is port and args[1] is server type
        if (args.length < 2) {
            System.out.println("Usage: StompServer <port> <tpc/reactor>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String serverType = args[1];

        if (serverType.equalsIgnoreCase("tpc")) {
            // Thread-Per-Client server
            Server.threadPerClient(
                    true,
                    port,
                    ()->new StompProtocolImpl(),   
                    ()->new StompEncDec()
            ).serve();

        } else if (serverType.equalsIgnoreCase("reactor")) {
            // Reactor server
            Server.reactor(
                    true,
                    Runtime.getRuntime().availableProcessors(), 
                    port,
                    ()->new StompProtocolImpl(),   
                    ()->new StompEncDec()
            ).serve();

        } else {
            System.out.println("Unknown server type. Please use 'tpc' or 'reactor'.");
        }
    }
}