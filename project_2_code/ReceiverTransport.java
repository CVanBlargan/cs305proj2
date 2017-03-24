
/**
 * A class which represents the receiver transport layer
 */
public class ReceiverTransport
{
    private ReceiverApplication ra;
    private NetworkLayer nl;
    private boolean usingTCP;

    private int lastReceivedSeqNum;

    public ReceiverTransport(NetworkLayer nl){
        ra = new ReceiverApplication();
        this.nl=nl;
        initialize();
    }

    public void initialize()
    {
    }

    public void receiveMessage(Packet pkt)
    {
        if(!pkt.isCorrupt()) {
            ra.receiveMessage(pkt.getMessage());
        }

        if(usingTCP) { //using tcp

            if(pkt.isCorrupt()) { //send a previous ack
                Packet newPacket = new Packet(new Message("Ack"), 0, lastReceivedSeqNum + 1, 0);
                nl.sendPacket(newPacket,0);
            } else { //send appropriate ack
                Packet newPacket = new Packet(new Message("Ack"), 0, pkt.getSeqnum() + 1, 0);
                nl.sendPacket(newPacket,0);
                lastReceivedSeqNum = pkt.getSeqnum();
            }
        } else {//using GBN
            if(pkt.isCorrupt()) {//send a previous ack
                Packet newPacket = new Packet(new Message("Ack"), 0, lastReceivedSeqNum, 0);
                nl.sendPacket(newPacket,0);
            } else { //send appropriate ack
                Packet newPacket = new Packet(new Message("Ack"), 0, pkt.getSeqnum(), 0);
                nl.sendPacket(newPacket,0);
                lastReceivedSeqNum = pkt.getSeqnum();
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
