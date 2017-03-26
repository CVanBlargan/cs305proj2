
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

        if((index >= messages.size()-1) || (index < baseNumber)) {
            return false;
        }

        if(usingTCP) { //using tcp

        } else { //using GBN
            if(index < baseNumber+n){ //falls in window size
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
        return false;
    }

    public void receiveMessage(Packet pkt)
    {
        if(usingTCP) {

        } else { //using GBN
            if(pkt.isCorrupt()) { //ignore the message if it is corrupt
                System.out.println("Received corrupt ack");
            } else if(!pkt.isCorrupt() && pkt.getAcknum() <baseNumber + n) { //if we receive an ack that is in the window
                tl.stopTimer();
                int amountShifted = pkt.getAcknum() - baseNumber + 1;
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

        } else {
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
