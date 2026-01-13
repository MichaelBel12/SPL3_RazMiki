package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompProtocolImpl;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder; // Replace with your StompMessageEncoderDecoder
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
                    port,
                    StompProtocolImpl::new,    // Protocol factory: provides a new instance for each client
                    LineMessageEncoderDecoder::new // Replace this with your STOMP-specific EncDec
            ).serve();

        } else if (serverType.equalsIgnoreCase("reactor")) {
            // Reactor server
            Server.Reactor(
                    Runtime.getRuntime().availableProcessors(), // Number of threads in the pool
                    port,
                    StompProtocolImpl::new,    // Protocol factory
                    LineMessageEncoderDecoder::new // Replace this with your STOMP-specific EncDec
            ).serve();

        } else {
            System.out.println("Unknown server type. Please use 'tpc' or 'reactor'.");
        }
    }
}