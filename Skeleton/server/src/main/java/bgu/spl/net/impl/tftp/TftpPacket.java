package bgu.spl.net.impl.tftp;

enum PacketType {
    RRQ, 
    WRQ, 
    DATA, 
    ACK, 
    ERROR, 
    DIRQ, 
    LOGRQ, 
    DELRQ, 
    BCAST, 
    DISC
}
/*
 * This class represents a TFTP packet.
 */
public class TftpPacket {
    private volatile PacketType type;
    private volatile byte[] data;

    public TftpPacket(byte[] data, int type) {
        this.data = data;
        switch (type) {
            case 1:
                this.type = PacketType.RRQ;
                break;
            case 2:
                this.type = PacketType.WRQ;
                break;
            case 3:
                this.type = PacketType.DATA;
                break;
            case 4:
                this.type = PacketType.ACK;
                break;
            case 5:
                this.type = PacketType.ERROR;
                break;
            case 6:
                this.type = PacketType.DIRQ;
                break;
            case 7:
                this.type = PacketType.LOGRQ;
                break;
            case 8:
                this.type = PacketType.DELRQ;
                break;
            case 9:
                this.type = PacketType.BCAST;
                break;
            case 10:
                this.type = PacketType.DISC;
                break;
            default:
                throw new IllegalArgumentException("Invalid type "+type);

        }
    }

    public byte[] getData() {
        return data;
    }
    public byte[] getType() {
        byte[] type = new byte[2];
        type[0] = 0;
        type[1] = (byte) (this.type.ordinal()+1);
        return type;
    }
    public int getTypeInt() {
        return this.type.ordinal()+1;
    }
    public PacketType getTypeEnum() {
        return this.type;
    }

    
}