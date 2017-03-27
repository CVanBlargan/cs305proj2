
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
        /*
         * Regardless of protocol, if you are told to send a message that doesn't exist for
         * some reason, don't send a message because it doesn't exist.
         */
        if((index >= messages.size()-1) || (index < baseNumber)) {
            return false;
        }

        if(usingTCP) { //using tcp
            
            if(index < baseNumber+n && index >= baseNumber){ //falls in window size
                tl.startTimer(150);
                /*
                 * Create a new packet with proper sequence number. It is important to keep track
                 * of the last sent sequence number because when you shift the window after receiving
                 * an ack, the lastSendSeqNum tells you how many additional acks to send
                 * (ie if your window covers packets 2,3,4, lastSendSeqNum = 4, and you receive an
                 * ack for 2, you only need to shift up so the window is now 3,4,5)
                 */
                Packet newPacket = new Packet(msg, index,0,0);
                nl.sendPacket(newPacket, 1);
                System.out.println("Packet " + index + ": Was in window, sending");
                lastSendSeqNum = index;
                System.out.println("Adjusting lastSendSeqNum to: " + lastSendSeqNum);
                return true;
            }else {// not in window
                /*
                 * Create a send event because the timeline will have to try again later
                 */
                tl.createSendEvent();
                System.out.println("Packet " + index + ": Was not in window, not sending");
                return true;
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
                tl.createSendEvent();
                System.out.println("Packet " + index + ": Was not in window, not sending");
                return true;
            }

        }
    }

    public void receiveMessage(Packet pkt)
    {        
        if(usingTCP) {
            
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
                tl.createSendEvent();
            } else if(!pkt.isCorrupt() && pkt.getAcknum() < baseNumber + n && pkt.getAcknum() > baseNumber) {
                
            } else if(!pkt.isCorrupt() && pkt.getAcknum() == baseNumber){
                //Handles the fast retransmit
                totalDups++;
                if(totalDups == 3) {
                    sendMessage(baseNumber, new Message(messages.get(baseNumber)));
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
                tl.createSendEvent();
            }

            if(pkt.isCorrupt()) { //ignore the message if it is corrupt
                //need to create new event to tell the timeline to try sending again
                System.out.println("Received corrupt ack");
                tl.createSendEvent();
            } else if(!pkt.isCorrupt() &&
                        pkt.getAcknum() <baseNumber + n &&
                        pkt.getAcknum() > baseNumber) { //if we receive an ack that is in the window
                //stop current timer (because cumulative ack means we received the proper one)
                tl.stopTimer();
                int amountShifted = pkt.getAcknum() - baseNumber + 1; //how many new packets to send
                baseNumber = pkt.getAcknum()+1; //shift the entire window up, cumulative ack

                System.out.println("Received ack " + pkt.getAcknum() + ": that was in the window, cumulative ack, shift the window to new location");
                System.out.println("Base Number is now: " + baseNumber);
                System.out.println("Shifting the window: " + amountShifted + " packets.");

                //this for loop is used to send all the new messages as a result of the shifted window
                for(int i = lastSendSeqNum+1; i < lastSendSeqNum + amountShifted;i++) {
                    if(i >= messages.size()-1) {
                        break;
                    }
                    sendMessage(i,new Message(messages.get(i)));
                }

                //This should be handled in send message already, but just in case?
                if(lastSendSeqNum+(pkt.getAcknum()-baseNumber) + 1 > lastSendSeqNum) {
                    lastSendSeqNum = lastSendSeqNum+(pkt.getAcknum()-baseNumber) + 1;
                    System.out.println("Adjusting lastSendSeqNum to: " + lastSendSeqNum);
                }
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
            if(baseNumber>= messages.size()-1) {
                return;
            }
            sendMessage(baseNumber, new Message(messages.get(baseNumber)));
        } else {
            /*
             * When using GBN, send whole window size on dead timer
             */
            for(int i = 0; i < n; i++) { //send the whole window size worth of packets
                if(baseNumber+i >= messages.size()-1) {
                    break;
                }
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
