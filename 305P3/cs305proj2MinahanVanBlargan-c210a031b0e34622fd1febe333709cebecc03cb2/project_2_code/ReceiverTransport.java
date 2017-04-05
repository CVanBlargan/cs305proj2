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
        if(usingTCP) { //using tcp
            if(!pkt.isCorrupt() && 
            pkt.getSeqnum() == lastInOrderPacketReceived + 1) { //if in the correct order
                ra.receiveMessage(pkt.getMessage()); //send the message up to the application
                if(pkt.getSeqnum() >lastInOrderPacketReceived) {
                    lastInOrderPacketReceived = pkt.getSeqnum(); //increase base pointer
                }

                System.out.println("Received Packet Number : " + pkt.getSeqnum() + " that was in correct order");

                //put the packet in the buffer (for consistency, maybe not really needed.  But easier to think about if it's there)
                //aka by the end the packetBuffer will eventually hold all packets.
                boolean inBuffer = false;
                for(int i = 0; i < packetBuffer.size();i++) {
                    if(packetBuffer.get(i).getSeqnum() == pkt.getSeqnum()) {
                        System.out.println("Packet number: " + pkt.getSeqnum() + " was already in buffer.  Ignoring");
                        inBuffer = true;
                        break;
                    }
                    if(packetBuffer.get(i).getSeqnum() > pkt.getSeqnum()) {
                        System.out.println("Packet Number: " + pkt.getSeqnum() + " was not in buffer.  Adding to Buffer.");
                        inBuffer = true;
                        packetBuffer.add(i, pkt);
                        break;
                    }
                }

                if(!inBuffer) {
                    packetBuffer.add(pkt);
                    System.out.println("Packet Number : " + pkt.getSeqnum() + " was not in buffer.  Adding to back of buffer.");
                }

            } else if(!pkt.isCorrupt() && !(pkt.getSeqnum() == lastInOrderPacketReceived + 1)) { //if needs to be buffer

                System.out.println("Received a non-corrupt packet in the incorrect order");
                System.out.println("Buffering Packet: " + pkt.getSeqnum());
                System.out.println("Was expected Packet: " + (lastInOrderPacketReceived +1));

                //This section puts the packets in the buffer in the correct order
                boolean inBuffer = false;
                for(int i = 0; i < packetBuffer.size();i++) {
                    if(packetBuffer.get(i).getSeqnum() == pkt.getSeqnum()) {
                        System.out.println("Packet number: " + pkt.getSeqnum() + " was already in buffer.  Ignoring");
                        inBuffer = true;
                        break;
                    }

                    if(packetBuffer.get(i).getSeqnum() > pkt.getSeqnum()) {
                        packetBuffer.add(i, pkt);
                        System.out.println("Packet Number: " + pkt.getSeqnum() + " was not in buffer.  Adding to Buffer.");
                        inBuffer = true;
                        break;
                    }
                }

                if(!inBuffer) {
                    packetBuffer.add(pkt);
                    System.out.println("Packet Number : " + pkt.getSeqnum() + " was not in buffer.  Adding to back of buffer.");
                }
            } else {
                System.out.println("Received an erroneous packet");
                System.out.println("Was expecting a packet with the Sequence Number : " + (lastInOrderPacketReceived+1));
                Packet ackPacket;
                if(lastInOrderPacketReceived != -1) {
                    System.out.println("Sending an ack with ack Number : " + (lastInOrderPacketReceived));
                    ackPacket = new Packet(new Message("Ack"), 0, lastInOrderPacketReceived, 0);
                } else {
                    System.out.println("Sending an ack with ack Number : 0");
                    ackPacket = new Packet(new Message("Ack"),0,0,0);
                }
                nl.sendPacket(ackPacket, 0);
                return;
            }

            //if there were packets in the buffer that can be passed up, pass them up and increase the base number
            System.out.println("Giving any relevant messages to the Receiver Application Layer");
            for(int i = lastInOrderPacketReceived; i < packetBuffer.size(); i++) {
                if(i != -1) {
                    ra.receiveMessage(packetBuffer.get(i).getMessage());
                    System.out.println("Giving packet number : " + packetBuffer.get(i).getSeqnum() + " to the application layer");
                    lastInOrderPacketReceived = i;
                    System.out.println("Adjusting lastInOrderPacketReceived to " + i);
                }

                //EX if the buffer looks like 2,3,5, stops after sending 3.
                if(i+1 < packetBuffer.size()) {
                    if(packetBuffer.get(i+1).getSeqnum() > packetBuffer.get(i).getSeqnum()) {
                        break;
                    }
                }
            }

            System.out.println("Sending an Ack with ackNum: " + (lastInOrderPacketReceived+1));
            Packet ackPacket = new Packet(new Message("Ack"), 0, lastInOrderPacketReceived+1, 0);
            nl.sendPacket(ackPacket, 0);
            return;

        } else {//using GBN

            if(!pkt.isCorrupt() && pkt.getSeqnum() == lastInOrderPacketReceived + 1) { //if we receive the correct packet

                System.out.println("Received an In-Order Packet with the seq num of: " + pkt.getSeqnum());
                ra.receiveMessage(pkt.getMessage());
                //Send ack back to sender and adjust the instance variables for what packet you last got
                System.out.println("Increasing the LastInOrderPacketReceived to " + (lastInOrderPacketReceived+1));
                System.out.println("Creating a new ack with ack number " + (lastInOrderPacketReceived + 1));
                Packet ackPacket = new Packet(new Message("Ack"), 0, lastInOrderPacketReceived + 1, 0);
                lastInOrderPacketReceived++;
                nl.sendPacket(ackPacket, 0);
                return;
            } else {
                /**
                 * This covers the cases of
                 * -not corrupted packet in incorrect order
                 * -corrupted packet in correct order
                 * -corrupted packet in incorrect order
                 */

                //send the lowest acked packet again
                System.out.println("Received an erroneous packet");
                System.out.println("Was Expecting packet with sequence Number : " + (lastInOrderPacketReceived +1));
                Packet ackPacket;
                if(lastInOrderPacketReceived != -1) {
                    System.out.println("Sending an ack with ack Number : " + (lastInOrderPacketReceived));
                    ackPacket = new Packet(new Message("Ack"), 0, lastInOrderPacketReceived, 0);
                } else {
                    System.out.println("Sending an ack with ack Number : 0");
                    ackPacket = new Packet(new Message("Ack"),0,-1,0);
                }
                nl.sendPacket(ackPacket, 0);
                return;

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
