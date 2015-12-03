package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

/**
 * Created by Valentin on 03/12/15.
 *
 * Test message. Use for example to show in a dialog.
 */
public class TestMessage extends Message {

    public TestMessage(int receiverPos) {super(receiverPos, Message.TEST_MSG);}
    public TestMessage(Message msg) {super(msg);}
}
