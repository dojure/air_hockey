package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 16/12/15.
 */
public class ReadyMessage extends Message {

    private final static String READY_KEY = "ready";

    private boolean mReady;

    public ReadyMessage(int receiverPos, boolean ready)
    {
        super(receiverPos, Message.READY_MSG);

        mReady = ready;

        try {
            mBody = new JSONObject();
            mBody.put(READY_KEY, mReady);
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ReadyMessage(ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mReady = mBody.getBoolean(READY_KEY);
        } catch (JSONException e) {e.printStackTrace();}
    }

    public boolean getReady() {return mReady;}
}
