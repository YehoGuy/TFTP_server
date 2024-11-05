package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.ConnectionHandler;
/*
 * The [TftpConnectionHandler] class is responsible for handling the connection with a single client.
 * It is responsible for sending and receiving messages from the client.
 * Each client will have its own [TftpConnectionHandler] instance.
 */
public class TftpConnectionHandler implements Runnable, ConnectionHandler<TftpPacket> {

    private TftpConnections connections;

    private final TftpProtocol protocol;
    private final TftpEncoderDecoder encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    private int ownerId;

    public TftpConnectionHandler(   int owner, 
                                    TftpConnections connections, 
                                    Socket sock, 
                                    TftpEncoderDecoder encdec, 
                                    TftpProtocol protocol,
                                    ConcurrentHashMap<String , Integer> loggedInClients) 
                                    {
        this.sock = sock;
        this.encdec = encdec;
        this.protocol = protocol;
        this.ownerId = owner;
        this.protocol.start(owner, connections, loggedInClients);
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { 
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                TftpPacket nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    // protocol handles responses
                    protocol.process(nextMessage);
                }
            }

            System.out.println("CH: closing connection with user: "+this.ownerId);
            this.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(TftpPacket msg) {
        try {
            System.out.println("TftpConnectionHandler: sending message of type: "+msg.getTypeEnum());
            out.write(encdec.encode(msg));
            System.out.println("message sent");
            out.flush();
            System.out.println("flushed");
        } catch (IOException e) {
            System.out.println("TftpConnectionHandler: send failed. "+e.getMessage()+". msg type was: "+msg.getTypeEnum());
        }
    }
}