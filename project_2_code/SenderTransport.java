
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

    private int lastReceivedAck;
    private int totalDups;

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
        if(usingTCP) { //using tcp
            if(index < lastReceivedAck +n + 1) { //falls in window size
                Packet newPacket = new Packet(msg, index,0,0);
                nl.sendPacket(newPacket, 1);
            } else {//not in window

            }
        } else { //using GBN
            if(index < lastReceivedAck+n){ //falls in window size
                Packet newPacket = new Packet(msg, index,0,0);
                nl.sendPacket(newPacket, 1);
                System.out.println(index);
                return true;
            }else {// not in window
                //so now we're in this weird state where the timeline thinks the message
                //has been sent but it couldn't be sent because it's outside of the window
                
                //you can try to create a new send event, but the problem is as far as timeline
                //is concerned, "MessagesSent" indicates that all the messages have been sent.
                
                tl.createSendEvent();
                return false;
            }
        }
        return false;
    }

    public void receiveMessage(Packet pkt)
    {
        if(usingTCP) {
            if(pkt.getAcknum() == lastReceivedAck) {
                totalDups++;
            } else if (pkt.getAcknum() != lastReceivedAck) {
                lastReceivedAck = pkt.getAcknum();
            }

            if(totalDups == 3) {
                sendMessage(lastReceivedAck, new Message(messages.get(lastReceivedAck)));
                totalDups = 0;
            }
        } else { //using GBN
            if(pkt.isCorrupt()) {
                //ignore the message if it is corrupt
                
            } else { //not corrupted
                lastReceivedAck = pkt.getAcknum();
                System.out.println(lastReceivedAck);
            }

        }

    }

    public void timerExpired()
    {
        if(usingTCP) {

        } else {
            for(int i = 0; i < n; i++) { //send the whole window size worth of packets
                sendMessage(lastReceivedAck+1+i, new Message(messages.get(lastReceivedAck)));
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
