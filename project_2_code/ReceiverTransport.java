import java.util.ArrayList;

/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer nl;
    private boolean usingTCP;

    private int lastInOrderPacketReceived = -1;
    private ArrayList<Packet> packetBuffer;
    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
    }

    public void initialize()
    {
        packetBuffer = new ArrayList<Packet>();
    }

    public void receiveMessage(Packet pkt)
    {
        
        /**
         * THIS IS THE PROCESS FOR BUFFERING PACKETS FOR TCP.  FORGOT GBN DOESN'T BUFFER
         * 
         * 
         * 
        if(!pkt.isCorrupt() && pkt.getSeqnum() == lastInOrderPacketReceived + 1) { //if in the correct order
            ra.receiveMessage(pkt.getMessage()); //send the message up to the application
            lastInOrderPacketReceived = pkt.getSeqnum(); //increase base pointer

            //put the packet in the buffer (for consistency, maybe not really needed.  But easier to think about if it's there)
            //aka by the end the packetBuffer will eventually hold all packets.
            for(int i = 0; i < packetBuffer.size();i++) {
                if(packetBuffer.get(i).getSeqnum() > pkt.getSeqnum()) {
                    packetBuffer.add(i, pkt);
                }
            }
            
        } else if(!pkt.isCorrupt() && !(pkt.getSeqnum() == lastInOrderPacketReceived + 1)) { //if needs to be buffer
            //This section puts the packets in the buffer in the correct order
            for(int i = 0; i < packetBuffer.size();i++) {
                if(packetBuffer.get(i).getSeqnum() > pkt.getSeqnum()) {
                    packetBuffer.add(i, pkt);
                }
            }
        }
        
        //if there were packets in the buffer that can be passed up, pass them up and increase the base number
        for(int i = lastInOrderPacketReceived; i < packet.size(); i++) {
            ra.receiveMessage(packetBuffer.get(i).getMessage());
            lastInOrderPacketReceived = i;
        }
        **/
        

        if(usingTCP) { //using tcp
            
            

        } else {//using GBN
            
            if(!pkt.isCorrupt() && pkt.getSeqnum() == lastInOrderPacketReceived + 1) { //if we receive the correct packet
                
                System.out.println("Received an In-Order Packet with the seq num of: " + pkt.getSeqnum());
                ra.receiveMessage(pkt.getMessage());
                Packet ackPacket = new Packet(new Message("Ack"), 0, lastInOrderPacketReceived + 1, 0);
                lastInOrderPacketReceived++;
                nl.sendPacket(ackPacket, 0);
            } else {
                /**
                 * This covers the cases of
                 * -not corrupted packet in incorrect order
                 * -corrupted packet in correct order
                 * -corrupted packet in incorrect order
                 */
                
                System.out.println("Received an erroneous packet");
                Packet ackPacket = new Packet(new Message("Ack"), 0, lastInOrderPacketReceived, 0);
                nl.sendPacket(ackPacket, 0);
                
            }
            
        }
    }

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }

}
