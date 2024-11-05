package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// [TFTP Server] - Main Server Class
public class TftpServer {

    private final int port;
    private final Supplier<TftpProtocol> protocolFactory;
    private final Supplier<TftpEncoderDecoder> encdecFactory;
    private ServerSocket serverSocK;

    // maps username --> connectionId
    private volatile ConcurrentHashMap<String , Integer> loggedInClients;
    // next connectionId = connectionCounter+1
    private int connectionsCounter;
    //Connections Object
    private TftpConnections connections;

    // references to both Maps will be passed to the protocol object
    // since he is the one in charge of updating them

    public TftpServer(
            int port,
            Supplier<TftpProtocol> protocolFactory,
            Supplier<TftpEncoderDecoder> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.serverSocK = null;

        loggedInClients = new ConcurrentHashMap<String, Integer>();
        connectionsCounter = 0;
        connections = new TftpConnections();
    }

    public static void main(String[] args) {
        new TftpServer(7777 , ()-> new TftpProtocol() , ()-> new TftpEncoderDecoder()).serve();
    }

    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.serverSocK = serverSock; //for automatic closing

            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("waiting for client connection");
                Socket clientSock = serverSock.accept();
                // Client Connected 
                connectionsCounter+=1;
                TftpConnectionHandler handler = new TftpConnectionHandler(
                        connectionsCounter,
                        this.connections,
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),
                        this.loggedInClients);

                this.connections.connect(connectionsCounter, handler);

                execute(handler);
            }
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
        System.out.println("server closed!!!");
        try{
            this.close();
            System.out.println("ServerSocket closed successfully");
        }
        catch (IOException e){
            System.out.println("ServerSocket close failed: "+e.getMessage());
        }
        
    }

    
    public void close() throws IOException {
		if (serverSocK != null)
			serverSocK.close();
    }

    private void execute(TftpConnectionHandler handler) {
        new Thread(handler).start();
        System.out.println("client "+this.connectionsCounter+" connected successfully!");
    }
    
}
