package bgu.spl.net.api;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

import java.util.LinkedList;
import java.util.List;

public class StompTester {

    public static void main(String[] args) {
        System.out.println("--- Starting STOMP Logic Test ---\n");

        // 1. Setup Mock Connections
        MockConnections connections = new MockConnections();
        StompProtocolImpl protocol = new StompProtocolImpl();
        int connectionId = 1;

        // 2. Start Protocol
        protocol.start(connectionId, connections);

        // 3. Define Test Cases
        List<String> testFrames = new LinkedList<>();

        // Case 1: CONNECT (Login)
        testFrames.add(
            "CONNECT\n" +
            "accept-version:1.2\n" +
            "host:stomp.cs.bgu.ac.il\n" +
            "login:meni\n" +
            "passcode:films\n"
        );

        // Case 2: SUBSCRIBE (Receipt request)
        testFrames.add(
            "SUBSCRIBE\n" +
            "destination:/topic/sci-fi\n" +
            "id:78\n" +
            "receipt:101\n"
        );

        // Case 3: SEND (Normal body)
        testFrames.add(
            "SEND\n" +
            "destination:/topic/sci-fi\n" +
            "\n" +
            "Hello Sci-Fi fans!\n"
        );

        // Case 4: SEND (Body with newlines - The "Tricky" Case)
        // If your split("\n") logic is broken, this will fail or print headers incorrectly.
        testFrames.add(
            "SEND\n" +
            "destination:/topic/sci-fi\n" +
            "\n" +
            "This message\nhas multiple\nlines inside the body.\n"
        );

        // Case 5: UNSUBSCRIBE
        testFrames.add(
            "UNSUBSCRIBE\n" +
            "id:78\n"
        );

        // Case 6: DISCONNECT
        testFrames.add(
            "DISCONNECT\n" +
            "receipt:77\n"
        );

        // 4. Run Tests
        int i = 1;
        for (String frame : testFrames) {
            System.out.println(">>> TEST " + i++ + ": Sending Frame...");
            System.out.println(frame.replace("\u0000", "^@")); // Visualizing null char
            
            try {
                protocol.process(frame);
            } catch (Exception e) {
                System.out.println("[CRITICAL ERROR] Exception during process(): " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("--------------------------------------------------");
        }
    }

    /**
     * Mocks the Connections interface to print output instead of sending over network.
     */
    // Change 'implements' to 'extends' and specify <String>
    static class MockConnections extends ConnectionsImpl<String> {

        @Override
        public boolean send(int connectionId, String msg) {
            System.out.println("<<< [SERVER TO CLIENT " + connectionId + "]:");
            System.out.println(msg);
            return true;
        }

        // Override the other send so it prints instead of using the real logic
        @Override
        public void send(String channel, String msg) {
            System.out.println("<<< [SERVER TO CHANNEL " + channel + "]:");
            System.out.println(msg);
        }

        @Override
        public void disconnect(int connectionId) {
            System.out.println("<<< [SERVER]: Disconnecting Client " + connectionId);
        }
    }
}