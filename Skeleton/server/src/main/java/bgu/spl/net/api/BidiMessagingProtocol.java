package bgu.spl.net.api;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.impl.tftp.TftpConnections;
import bgu.spl.net.srv.Connections;

public interface BidiMessagingProtocol<T>  {
	/**
	 * Used to initiate the current client protocol with it's personal connection ID and the connections implementation
	**/                         
    void start(int connectionId, TftpConnections connections, ConcurrentHashMap<String , Integer> loggedInClients);
    
    void process(T message);
	
	/**
     * @return true if the connection should be terminated
     */
    boolean shouldTerminate();
}
