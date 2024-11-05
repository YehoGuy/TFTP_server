package bgu.spl.net.impl.tftp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;
/*
 * The [TftpProtocol] class is the brain of the server, 
 * it is responsible for the packet processing logic of the server,
 * handling the different types of packets.
 */
public class TftpProtocol implements BidiMessagingProtocol<TftpPacket>  {
    
    private TftpConnections connections;
    private boolean shouldTerminate;

    private int ownerId;
    private FileRR fileRR;
    // both stored by the server & passed to each protocol object
    private ConcurrentHashMap<String , Integer> loggedInClients;
    private String userName;

    // file-to-send byte buffer
    private Queue<TftpPacket> fileToSend;

    @Override
    public void start(  int connectionId, 
                        TftpConnections connections, 
                        ConcurrentHashMap<String , Integer> loggedInClients) 
                        {
        this.ownerId = connectionId;
        this.connections = connections;
        this.shouldTerminate = false;
        this.fileRR = new FileRR();
        this.loggedInClients = loggedInClients;
        this.userName = null;
        this.fileToSend = new LinkedList<TftpPacket>(); // static type being Queue narrows the options to Queue ones.
    }

    @Override
    public void process(TftpPacket message) {
        switch(message.getTypeEnum()){


            case RRQ:
                System.out.println("RRQ recieved from ownerId: "+ownerId);
                if(!loggedInClients.containsValue(ownerId)){
                    this.sendError("user not logged in", (short)6);
                    break;
                }
                int RRQlength = 0;
                for (byte b : message.getData()) {
                    if (b == 0)
                        break;
                    RRQlength+=1;
                }
                String RRQfilename = new String(message.getData(), 0, RRQlength,  StandardCharsets.UTF_8);
                System.out.println("RRQ from ownerId: "+ownerId+" filename: "+RRQfilename);
                if(!fileRR.doesFileExist(RRQfilename)){
                    this.sendError("file not found", (short)1);
                    break;
                }

                byte[] RRQfile = fileRR.readFile(RRQfilename);
                // split into data packet and add to fileToSend
                for(int i = 0 ; i < RRQfile.length ; i+=512){
                    byte[] blockFileData = new byte[512];
                    short blockSize = 0;
                    short blockNumber = (short) ((i/512)+1);
                    for(int j = 0 ; j < 512 && i+j < RRQfile.length ; j++){
                        blockFileData[j] = RRQfile[i+j];
                        blockSize+=1;
                    }
                    byte[] packetData = new byte[4+blockSize];
                    packetData[0] = (byte) (blockSize >> 8);
                    packetData[1] = (byte) (blockSize & 0xff);
                    packetData[2] = (byte) (blockNumber >> 8);
                    packetData[3] = (byte) (blockNumber & 0xff);
                    for(int j = 0 ; j < blockSize ; j++){
                        packetData[j+4] = blockFileData[j];
                    }
                    TftpPacket p = new TftpPacket(packetData, 3);
                    this.fileToSend.add(p);
                    // handleing of the case that the number of bytes%512 == 0
                    if(i == RRQfile.length-1){
                        byte[] lastPacketData = {0,0,(byte) (blockNumber+1 >> 8),(byte) (blockNumber+1 & 0xff)};
                        this.fileToSend.add(new TftpPacket(lastPacketData,3));
                    }
                }
                // send first data packet
                this.connections.send(ownerId, this.fileToSend.poll());
                break;


            case WRQ:
                System.out.println("WRQ recieved from ownerId: "+ownerId);
                if(!loggedInClients.containsValue(ownerId)){
                    this.sendError("user not logged in", (short)6);
                    break;
                }
                int WRQlength = 0;
                for (byte b : message.getData()) {
                    if (b == 0)
                        break;
                    WRQlength+=1;
                }
                String WRQfilename = new String(message.getData(), 0, WRQlength,  StandardCharsets.UTF_8);
                System.out.println("WRQ from ownerId: "+ownerId+" filename: "+WRQfilename);
                if(fileRR.doesFileExist(WRQfilename)){
                    this.sendError("file already exists", (short)5);
                    break;
                }
                this.fileRR.setFtwName(WRQfilename);
                this.sendAck((short)0);

                break;


            case DATA:
                System.out.println("RRQ recieved from ownerId: "+ownerId);
                short dataLength = (short) (((short)message.getData()[0]) << 8 | (short)(message.getData()[1]) & 0xff);
                this.fileRR.recieveBlock(message.getData(), dataLength);

                short dataBlockNumber = (short) (((short)message.getData()[2]) << 8 | (short)(message.getData()[3]) & 0xff);
                this.sendAck(dataBlockNumber);

                if(dataLength < 512)
                {
                    try {
                        String fileName = this.fileRR.writeFile();
                        this.sendBcast(fileName , 1);
                    } 
                    catch (AccessDeniedException e) 
                    {
                        this.fileRR.cleanData();
                        this.sendError("Access violation - File cannot be written", (short)2);
                        break;
                    }
                    catch(IOException e){
                        this.fileRR.cleanData();
                        this.sendError("File cannot be written", (short)0);
                        break;
                    }
                }
                break;


            case ACK:
                short ackBlockNumber = (short) (((short)message.getData()[0]) << 8 | (short)(message.getData()[1]) & 0xff);
                System.out.println("ACK recieved from ownerId: "+ownerId+" blockNumber: "+ackBlockNumber);
                if(ackBlockNumber != 0)
                {
                    if(this.fileToSend.size() > 0)
                        this.connections.send(ownerId, this.fileToSend.poll());
                }
                break;

            case DIRQ:
                System.out.println("DIRQ recieved from ownerId: "+ownerId);
                if(!loggedInClients.containsValue(ownerId)){
                    this.sendError("user not logged in", (short)6);
                    break;
                }
                byte[] dirData = fileRR.readDirectory();
                // split into data packet and add to fileToSend
                for(int i = 0 ; i < dirData.length ; i+=512){
                    byte[] blockFileData = new byte[512];
                    short blockSize = 0;
                    short blockNumber = (short) ((i/512)+1);
                    for(int j = 0 ; j < 512 && i+j < dirData.length ; j++){
                        blockFileData[j] = dirData[i+j];
                        blockSize+=1;
                    }
                    byte[] packetData = new byte[4+blockSize];
                    packetData[0] = (byte) (blockSize >> 8);
                    packetData[1] = (byte) (blockSize & 0xff);
                    packetData[2] = (byte) (blockNumber >> 8);
                    packetData[3] = (byte) (blockNumber & 0xff);
                    for(int j = 0 ; j < blockSize ; j++){
                        packetData[j+4] = blockFileData[j];
                    }
                    TftpPacket p = new TftpPacket(packetData, 3);
                    this.fileToSend.add(p);
                    // handleing of the case that the number of bytes%512 == 0
                    if(i == dirData.length-1){
                        byte[] lastPacketData = {0,0,(byte) (blockNumber+1 >> 8),(byte) (blockNumber+1 & 0xff)};
                        this.fileToSend.add(new TftpPacket(lastPacketData,3));
                    }
                }
                // send first data packet
                this.connections.send(ownerId, this.fileToSend.poll());
                System.out.println("DIRQ first dataPacket sent! to ownerId: "+ownerId);
                break;

            case LOGRQ: //V
                System.out.println("LOGRQ recieved from ownerId: "+ownerId);
                int LOGRQlength = 0;
                for (byte b : message.getData()) {
                    if (b == 0)
                        break;
                    LOGRQlength+=1;
                }
                String LOGRQusername = new String(message.getData(), 0, LOGRQlength,  StandardCharsets.UTF_8);
                boolean isLoggedIn = loggedInClients.containsKey(LOGRQusername) | loggedInClients.containsValue(ownerId);
                if(isLoggedIn){
                    System.out.println("user is already logged in.");
                    this.sendError("user is already logged in.", (short)0);
                }
                else{
                    loggedInClients.put(LOGRQusername, ownerId);
                    this.userName = LOGRQusername;
                    this.sendAck((short)0);
                    System.out.println("user "+this.userName+ "logged in succesfully + ack sent.");
                }       
                break;

            case DELRQ: //V
                if(!loggedInClients.containsValue(ownerId)){
                    this.sendError("user not logged in", (short)6);
                    break;
                }
                int DELRQlength = 0;
                for (byte b : message.getData()) {
                    if (b == 0)
                        break;
                    DELRQlength+=1;
                }
                String DELRQfilename = new String(message.getData(), 0, DELRQlength,  StandardCharsets.UTF_8);

                if(!fileRR.doesFileExist(DELRQfilename)){
                    this.sendError("file not found", (short)1);
                    break;
                }

                try {
                    fileRR.deleteFile(DELRQfilename);
                    this.sendAck((short)0);
                    this.sendBcast(DELRQfilename , 0);
                } 
                catch (AccessDeniedException e) 
                {
                    this.sendError("Access violation - File cannot be deleted", (short)2);
                    break;
                }
                catch (IOException e) 
                {
                    this.sendError("File cannot be deleted", (short)0);
                    break;
                }

                break;

            
                
            case DISC:
                if(!loggedInClients.containsValue(ownerId)){
                    this.sendError("user not logged in", (short)6);
                    break;
                }
                this.sendAck((short)0);
                this.loggedInClients.remove(this.userName);
                this.connections.disconnect(ownerId);
                this.shouldTerminate = true;
                break;

            default:
                this.sendError("unknown op-code "+message.getTypeInt()+" Illegal Tftp Operation.", (short)0);

        }
    }

    @Override
    public boolean shouldTerminate() {
        System.out.println("shouldTerminate: "+this.shouldTerminate);
        return this.shouldTerminate;
    } 

    // method for simply sending an AckPacket to Client identified by ownerId
    private void sendAck(short blockNumber){
        byte[] ackData = new byte[2];
        ackData[0] = (byte) (blockNumber >> 8);
        ackData[1] = (byte) (blockNumber & 0xff);
        connections.send(ownerId, new TftpPacket(ackData, 4));
    }

    // method for simply sending an ErrorPacket to Client identified by ownerId
    private void sendError(String msg, short errorCode)
    {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        byte[] errorData = new byte[3+msgBytes.length];
        errorData[0] = (byte) (errorCode >> 8);
        errorData[1] = (byte) (errorCode & 0xff);
        for(int i = 0 ; i < msgBytes.length ; i++){
            errorData[i+2] = msgBytes[i];
        }
        errorData[errorData.length-1] = 0;
        connections.send(ownerId, new TftpPacket(errorData, 5));
    }

    // send Bcast packet to all logged in clients
    private void sendBcast(String fileName , int action){

        byte[] fileBytes = fileName.getBytes(StandardCharsets.UTF_8);
        byte[] bcastData = new byte[1+fileBytes.length+1];

        bcastData[0] = (byte)(action);
        for(int i = 0 ; i < fileBytes.length ; i++){
            bcastData[i+1] = fileBytes[i];
        }
        bcastData[bcastData.length-1] = 0;
        
        for(Integer id : loggedInClients.values()){
            connections.send(id, new TftpPacket(bcastData, 9));
        }

    }

    
}
