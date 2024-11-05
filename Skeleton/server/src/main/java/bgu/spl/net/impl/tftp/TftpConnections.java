package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.srv.Connections;

/*
 * The TftpConnections class is used to manage the server's connections.
 * it maps between connectionId and ConnectionHandler.
 */
public class TftpConnections implements Connections<TftpPacket> {

    // maps connectionId --> ConnectionHandler
    private volatile ConcurrentHashMap<Integer, TftpConnectionHandler> connections;
    

    public TftpConnections() {
        connections = new ConcurrentHashMap<Integer, TftpConnectionHandler>();
    }

    @Override
    public boolean connect(int connectionId, TftpConnectionHandler handler) {
        boolean alreadyConnected  = this.isConnected(connectionId);
        if(!alreadyConnected)
            connections.put(connectionId, handler);
        else
            System.out.println("connection "+connectionId+" already exists.");
            
        return !alreadyConnected;
        
    }

    public boolean isConnected(int connectionId){
        return connections.containsKey(connectionId);
    }

    @Override
    public boolean send(int connectionId, TftpPacket msg) {
        try{
            connections.get(connectionId).send(msg);
            return true;
        }
        catch (NullPointerException e){
            System.out.println("send to "+connectionId+" failed.");
            return false;
        }
    }

    @Override
    public void disconnect(int connectionId) {
        connections.remove(connectionId);
    }
     
}