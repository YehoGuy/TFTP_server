package bgu.spl.net.srv;

import java.io.IOException;

import bgu.spl.net.impl.tftp.TftpConnectionHandler;
import bgu.spl.net.impl.tftp.TftpConnections;

public interface Connections<T> {
                                        
    boolean connect(int connectionId, TftpConnectionHandler handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);
}
