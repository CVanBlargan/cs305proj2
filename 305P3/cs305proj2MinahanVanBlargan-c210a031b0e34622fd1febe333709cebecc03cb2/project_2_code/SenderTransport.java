
import java.util.ArrayList;
/**
 * A class which represents the receiver transport layer
 */
public class SenderTransport
{
    private NetworkLayer nl;
    private Timeline tl;
    private int n;
    private boolean usingTCP;

    private int baseNumber;
    private int totalDups;
    private int lastSendSeqNum;

    ArrayList<String> messages = null;

    public SenderTransport(NetworkLayer nl){
        this.nl=nl;
        initialize();
    }

    public void initialize()
    {
    }

    public boolean sendMessage(int index,Message msg)
    {
        tl.startTimer(150);
        /*
         * Regardless of protocol, if you are told to send a message that doesn't exist for
         * some reason, don't send a message because it doesn't exist.
         */
        if((index > messages.size()) || (index < baseNumber)) {
            return false;
        }

        if(usingTCP) { //using tcp

            if(index + 1 < baseNumber + n && index + 1 >= baseNumber){ //falls in window size
                //tl.startTimer(150);
                /*
                 * Create a new packet with proper sequence number. It is important to keep track
                 * of the last sent sequence number because when you shift the window after receiving
                 * an ack, the lastSendSeqNum tells you how many additional acks to send
                 * (ie if your window covers packets 2,3,4, lastSendSeqNum = 4, and you receive an
                 * ack for 2, you only need to shift up so the window is now 3,4,5)
                 */
                Packet newPacket = new Packet(msg, index,0,0);
                nl.sendPacket(newPacket, 1);
                System.out.println("Packet " + index + ": Was in window, sending" + "  Last sent: " + lastSendSeqNum);
                if(index > lastSendSeqNum) {
                    lastSendSeqNum = index;
                    System.out.println("Adjusting lastSendSeqNum to: " + lastSendSeqNum);
                }
                return true;
            }else {// not in window
                System.out.println("Packet " + index + ": Was not in window, not sending");
                return false;
            }
        } else { //using GBN

            /*
             * Mostly the same as TCP.  Eventually we have to distinguish between the +1's
             * to seqnum's and acks and stuff that TCP does.  Haven't done that yet.  Otherwise
             * the send portion of the protocol should be the same
             */

            if(index < baseNumber+n && index >= baseNumber){ //falls in window size
                tl.startTimer(150);
                Packet newPacket = new Packet(msg, index,0,0);
                nl.sendPacket(newPacket, 1);
                System.out.println("Packet " + index + ": Was in window, sending");
                lastSendSeqNum = index;
                System.out.println("Adjusting lastSendSeqNum to: " + lastSendSeqNum);
                return true;
            }else {// not in window
                //tl.createSendEvent();
                System.out.println("Packet " + index + ": Was not in window, not sending");
                return false;
            }
        }
    }

    public void receiveMessage(Packet pkt)
    {        
        if(usingTCP) {
            tl.startTimer(150);
            /*
             * Didn't really start TCP.  It's going to have to get acks in a totally different
             * way from GBN because it won't be cumulative I don't think.  So if the window is
             * 2,3,4 and we have acks for 3,4, we still need to get the ack for 2 before we can move
             * That'll be hard using the same system as GBN because the BaseNumber defines what you
             * should get, and you have no information for what you've gotten.  Essentially, though
             * this part should be similar to the buffering done in the receiver transport for TCP
             */
            if(pkt.isCorrupt()) {
                //need to create new event to tell the timeline to try sending again
                System.out.println("Received corrupt ack");
                System.out.println("Was expecting an ack number that fell within the range of " + (baseNumber +1) + " and " + (baseNumber + n+1));
                //tl.createSendEvent();
            } else if(!pkt.isCorrupt() 
            && pkt.getAcknum()-1 <= baseNumber + n 
            && pkt.getAcknum()-1 >= baseNumber) {
                //Receive Correct Message
                tl.stopTimer();
                /*
                 * So, for example, if your base number is 4, that means you should be expecting
                 * ack 5.  If you get ack five, you need to shift acknum - basenumber.
                 * 
                 * This logic still holds if your base number is 4, and you get ack 6.  Somewhere
                 * along the way, ack 5 (which would have been the ack for packet 4) got lost, but
                 * we have confirmed (with ack 6, for packet 5), that packet five arrived safely.  So
                 * We can send 2 new packets.  AKA acknum - basenumber
                 */
                int amountShifted = pkt.getAcknum() - baseNumber;

                /*
                 * When we get ack number five, that means we can send packet five.  The base number
                 * would then be five.  AKA the basenumber is now the ack num
                 */
                baseNumber = pkt.getAcknum();

                System.out.println("Received ack " + pkt.getAcknum() + ": that was in window, cumulative ack, shift the window to new location");
                System.out.println("Ack " + pkt.getAcknum() + ": was sent for Packet with sequence number: " + (pkt.getAcknum() -1));
                System.out.println("Base Number is now: " + baseNumber);
                System.out.println("Shifting the window: " + amountShifted + "packets.");

                //So lefts say this is the window [2,3,4,5]6,7
                //baseNumber = 2, lastSendSeqNum=5,expecting ack 6.
                //And we get ack for packet 4 (with ack num of 5).
                //We would shift 3 packets.

                //This loop, in that case, would send packets 6, 7, 8
                for(int i = lastSendSeqNum+1; i < lastSendSeqNum + amountShifted+1;i++) {
                    if(i >= messages.size() || i == messages.size()) {
                        break;
                    }
                    System.out.println("Sending packet number " + i + " because of window shift");
                    sendMessage(i,new Message(messages.get(i)));
                }

                //if 5+6-3 > 5, adjust to be 8 essentially.
                if(lastSendSeqNum+(pkt.getAcknum()-baseNumber)-1 > lastSendSeqNum) {
                    lastSendSeqNum = lastSendSeqNum+(pkt.getAcknum()-baseNumber)-1;
                    System.out.println("Adjusting lastSendSeqNum to: " + lastSendSeqNum);
                }
                tl.startTimer(150);
            } else if(!pkt.isCorrupt()){
                //Handles the fast retransmit
                totalDups++;
                System.out.println("Received a ack that is not in the window size");
                System.out.println("Ack Number: " + pkt.getAcknum() + ", For packet Sequence Number: " + (pkt.getAcknum()-1));
                System.out.println("Current Base Number is " + baseNumber);
                System.out.println("So window size ranges from " + baseNumber + " to " + (baseNumber + n));
                if(totalDups == 3) {
                    System.out.println("Fast Retransmit for packet number: " + baseNumber);

                    if(baseNumber!= messages.size()) {
                        System.out.println("Sending packet " + baseNumber + " because of duplicate acks");
                        sendMessage(baseNumber, new Message(messages.get(baseNumber)));
                    }
                    totalDups = 0;
                }
            }
        } else { //using GBN

            /*
             * If you received an ack from out of the window size, it must have used
             * a timeline event and then died somewhere.  So create a new one (worst case
             * scenario, you create a bunch of send events that die and don't do anything)
             */
            if(pkt.getAcknum() >= baseNumber) {
                //tl.createSendEvent();
            }

            if(pkt.isCorrupt()) { //ignore the message if it is corrupt
                //need to create new event to tell the timeline to try sending again
                System.out.println("Received corrupt ack");
                System.out.println("Was expecting an ack number that fell within the range of " + (baseNumber) + " and " + (baseNumber + n));
                //tl.createSendEvent();
            } else if(!pkt.isCorrupt() &&
            pkt.getAcknum() <= baseNumber + n &&
            pkt.getAcknum() >= baseNumber) { //if we receive an ack that is in the window
                //stop current timer (because cumulative ack means we received the proper one)
                tl.stopTimer();
                int amountShifted = pkt.getAcknum() - baseNumber + 1; //how many new packets to send
                baseNumber = pkt.getAcknum()+1; //shift the entire window up, cumulative ack

                System.out.println("Received ack " + pkt.getAcknum() + ": that was in window, cumulative ack, shift the window to new location");
                System.out.println("Ack " + pkt.getAcknum() + ": was sent for Packet with sequence number: " + (pkt.getAcknum()));
                System.out.println("Base Number is now: " + baseNumber);
                System.out.println("Shifting the window: " + amountShifted + "packets.");

                //this for loop is used to send all the new messages as a result of the shifted window
                for(int i = lastSendSeqNum+1; i < lastSendSeqNum + amountShifted;i++) {
                    if(i >= messages.size()) {
                        break;
                    }
                    System.out.println("Sending packet number " + i + " because of window shift");
                    sendMessage(i,new Message(messages.get(i)));
                }

                //This should be handled in send message already, but just in case?
                if(lastSendSeqNum+(pkt.getAcknum()-baseNumber) + 1 > lastSendSeqNum) {
                    lastSendSeqNum = lastSendSeqNum+(pkt.getAcknum()-baseNumber) + 1;
                    System.out.println("Adjusting lastSendSeqNum to: " + lastSendSeqNum);
                }
            } else {
                System.out.println("Received an ack that was outside of the window");
                System.out.println("Expected an acknumber between " + baseNumber + " and " + (baseNumber+n));
                System.out.println("Received ack had an acknumber of " + pkt.getAcknum());
                //tl.createSendEvent();
            }
        }
    }

    public void timerExpired()
    {
        System.out.println("Timer Expired");
        if(usingTCP) {
            /*
             * When using TCP,only send one packet
             */
            if(baseNumber>= messages.size()) {
                return;
            }
            if(baseNumber != messages.size()) {
                System.out.println("Sending Packet Number " + baseNumber + " because of expired Timer");
                sendMessage(baseNumber, new Message(messages.get(baseNumber)));
            }
        } else {
            /*
             * When using GBN, send whole window size on dead timer
             */
            for(int i = 0; i < n; i++) { //send the whole window size worth of packets
                if(baseNumber+i >= messages.size()) {
                    return;
                }
                System.out.println("Sending Packet Number " + (baseNumber+i) + " because of expired Timer");
                sendMessage(baseNumber + i, new Message(messages.get(baseNumber + i)));
            }
        }
    }

    public void setTimeLine(Timeline tl)
    {
        this.tl=tl;
    }

    public void setWindowSize(int n)
    {
        this.n=n;
    }

    public void setProtocol(int n)
    {
        if(n>0)
            usingTCP=true;
        else
            usingTCP=false;
    }

    public void setMessages(ArrayList<String> messages) {
        this.messages = messages;
    }

}
