package bgu.spl.net.impl.tftp;

import java.util.LinkedList;
import bgu.spl.net.api.MessageEncoderDecoder;

/*
 * This class is responsible for encoding and decoding TFTP packets.
 * Each time the ConnectionHandler receives a new byte, it calls the decodeNextByte method.
 * The method returns null until a full packet is decoded, then it returns the packet.
 * ! to understand the decoding process, read the instructions.pdf file !
 *   it explains the structure of each packet.
 */
public class TftpEncoderDecoder implements MessageEncoderDecoder<TftpPacket> {
    private volatile LinkedList<Byte> packetData;
    private volatile int type;
    private volatile Short expectedDataSize;

    public TftpEncoderDecoder() {
        this.packetData = new LinkedList<Byte>();
        this.type = 0;
        this.expectedDataSize = null;
    }


    @Override
    public TftpPacket decodeNextByte(byte nextByte) {
        if(this.type == 0){
            this.type = nextByte;
            if(this.type == 6 || this.type == 10)
            {
                TftpPacket packet = new TftpPacket(new byte[0], this.type);
                cleanData();
                return packet;
            }
            else
                return null;
        }
        else{
            switch( this.type ){
                case 1: //RRQ
                    this.packetData.add(nextByte);
                    if(nextByte == 0)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - RRQ packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else
                        return null;

                case 2: //WRQ
                    this.packetData.add(nextByte);
                    if(nextByte == 0)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - WRQ packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else
                        return null;
                case 3: //DATA
                    this.packetData.add(nextByte);
                    if(this.expectedDataSize!=null && this.packetData.size()-4 == this.expectedDataSize) //-4 since size excludes PS & BN
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - DATA packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else{
                        if(this.expectedDataSize == null && this.packetData.size() == 2)
                        {
                            this.expectedDataSize = (short)(((short)this.packetData.get(0)) << 8 | (short)(this.packetData.get(1)) & 0xff );
                        }
                        return null;
                    }

                case 4: //ACK
                    this.packetData.add(nextByte);
                    if(this.packetData.size() == 2)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - ACK packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else
                        return null;

                case 5: //ERROR
                    this.packetData.add(nextByte);
                    if(nextByte == 0 && this.packetData.size() >= 2)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - ERROR packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else 
                        return null;
                case 6: //DIRQ
                    throw new IllegalArgumentException("WTF, TFTPENCDEC type 6 in the switch case");
                case 7: //LOGRQ
                    this.packetData.add(nextByte);
                    if(nextByte == 0)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - LOGRQ packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else 
                        return null;

                case 8: //DELRQ
                    this.packetData.add(nextByte);
                    if(nextByte == 0)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - DELRQ packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else 
                        return null;
                    
                case 9: //BCAST
                    this.packetData.add(nextByte);
                    if(nextByte == 0 && this.packetData.size() >= 2)
                    {
                        byte[] data = this.toArr();
                        TftpPacket packet = new TftpPacket(data, this.type);
                        System.out.println("LOG: EncoderDecoder - BCAST packet decoded");
                        this.cleanData();
                        return packet;
                    }
                    else 
                        return null;

                case 10: //DISC
                    throw new IllegalArgumentException("WTF, TFTPENCDEC type 10 in the switch case");
                default:
                    throw new IllegalArgumentException("Invalid type "+this.type);
                
            }
        }
    }


    @Override
    public byte[] encode(TftpPacket message) {
        byte[] packetBytes = new byte[message.getData().length + 2];
        packetBytes[0] = message.getType()[0];
        packetBytes[1] = message.getType()[1];
        for(int i = 2; i < packetBytes.length ; i++)
            packetBytes[i] = message.getData()[i-2];
        return packetBytes;
    }

    private void cleanData(){
        this.type = 0 ;
        this.expectedDataSize = null;
        this.packetData.clear();
    }

    private byte[] toArr(){
        byte[] data = new byte[this.packetData.size()];
        for(int i=0 ; i<data.length ; i++)
            data[i] = this.packetData.removeFirst();
        return data;
    }

    
}