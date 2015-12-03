package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

/**
 * Created by Valentin on 03/12/15.
 */
public class TestMessage extends Message {

    public TestMessage(String type, int senderId) {super(type, senderId);}
    public TestMessage(byte[] bytes, int noBytes) {super(bytes, noBytes);}
    public TestMessage(Message msg) {super(msg);}
}
