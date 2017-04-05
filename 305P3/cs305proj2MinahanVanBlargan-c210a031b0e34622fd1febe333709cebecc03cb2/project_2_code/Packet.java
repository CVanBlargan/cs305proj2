import java.util.*;

/**
 * A class which represents a packet
 */
public class Packet
{
    
    private Message msg; //the enclosed message
    private int seqnum; //packets seq. number
    private int acknum; //packet ack. number
    private int checksum; //packet checksum

    Random ran; //random number generator

    public Packet(Message msg, int seqnum, int acknum, int checksum)
    {
        this.msg=msg;
        this.seqnum=seqnum;
        this.acknum=acknum;
        setChecksum();
        this.ran=new Random();
    }

    public int getAcknum()
    {
        return acknum;
    }
    
    public int getSeqnum()
    {
        return seqnum;
    }

    public Message getMessage()
    {
        return msg;
    }
    
    /**
     * This method just takes the individual bytes of the message field, acknum field, and seqnum field and adds them all up.
     * That is the checksum.
     */
    public void setChecksum()
    {
        
        char[] charArray = msg.getMessage().toCharArray();
        int checksumCnt = 0;
        for(int i = 0; i < charArray.length; i++) {
            checksumCnt+=charArray[i];
        }
        checksumCnt += acknum;
        checksumCnt += seqnum;
        
        
        this.checksum = checksumCnt;
    }
    
    /**
     * All this method does is see if the checksum is the same when it reaches the side its intended to get to.  Uses the same process.
     */
    public boolean isCorrupt()
    {
        
        char[] charArray = msg.getMessage().toCharArray();
        int checksumCnt = 0;
        for(int i = 0; i < charArray.length; i++) {
            checksumCnt+=charArray[i];
        }
        checksumCnt += acknum;
        checksumCnt += seqnum;
        

        if(this.checksum != checksumCnt) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This method curropts the packet the follwing way:
     * curropt the message with a 75% chance
     * curropt the seqnum with 12.5% chance
     * curropt the ackum with 12.5% chance
     */
    public void corrupt()
    {
        
        System.out.println("Corrupting packet " + (seqnum+acknum));
        if(ran.nextDouble()<0.75)
        {this.msg.corruptMessage();}
        else if(ran.nextDouble()<0.875)
        {this.seqnum=this.seqnum+1;}
        else
        {this.acknum=this.acknum+1;}

    }
    

}
