package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.api.StompProtocolImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {
    private final int port;
    private final Supplier<StompMessagingProtocol<T>> stompProtocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private final Supplier<MessagingProtocol<T>> protocolFactory;
    private ServerSocket sock;
    private ConnectionsImpl<T> connections; 
    private boolean isStomp=false;

    public BaseServer(
            boolean isStomp,int port,
            Supplier<StompMessagingProtocol<T>> stompProtocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {
        this.stompProtocolFactory=stompProtocolFactory;
        this.port = port;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections=new ConnectionsImpl<>();
        protocolFactory=null;
        this.isStomp=true;
    }
    public BaseServer(
            int port,
            Supplier<MessagingProtocol<T>> msgFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {
        this.protocolFactory=msgFactory;
        this.port = port;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections=new ConnectionsImpl<>();
        this.stompProtocolFactory=null;
    }



    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();
                int uniqID=connections.newUniqID();
                if(isStomp){
                    BlockingStompHandler<T> handler = new BlockingStompHandler<>(
                    clientSock,
                    encdecFactory.get(),
                    stompProtocolFactory.get(),uniqID,connections);
                connections.addClientToActiveClients(uniqID, handler);
                execute(handler);
            }
            else{
                BlockingConnectionHandler<T> handler2=new BlockingConnectionHandler<>(
                clientSock,
                encdecFactory.get(),
                protocolFactory.get(),uniqID,connections);
                connections.addClientToActiveClients(uniqID, handler2);
                execute(handler2);
            }
                
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingStompHandler<T>  handler);
    
    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
