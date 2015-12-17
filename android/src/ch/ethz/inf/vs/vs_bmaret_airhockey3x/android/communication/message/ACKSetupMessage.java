package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 03/12/15.
 *
 * Use this class to acknowledge actions during the setup phase.
 */
public class ACKSetupMessage extends Message {

    private final static String ACK_CODE_KEY = "ack_code";

    // ACK codes
    public final static int ENTERED_SETUP_ACTIVITY = 0;
    public final static int ALL_CONNECTED = 1;

    private int mAckCode;

    public ACKSetupMessage(int receiverPos, int ackCode)
    {
        super(receiverPos,Message.ACK_SETUP_MSG);

        mAckCode = ackCode;

        try {
            mBody = new JSONObject();
            mBody.put(ACK_CODE_KEY, ackCode);
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ACKSetupMessage(Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mAckCode = mBody.getInt(ACK_CODE_KEY);
        } catch (JSONException e) {e.printStackTrace();}
    }

    public int getAckCode() {return mAckCode;}

}
