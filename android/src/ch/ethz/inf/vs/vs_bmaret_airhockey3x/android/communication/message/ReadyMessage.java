package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 16/12/15.
 */
public class ReadyMessage extends Message {

    public ReadyMessage(int receiverPos)
    {
        super(receiverPos, Message.READY_MSG);

        try {
            mBody = new JSONObject();
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
        } catch (JSONException e) {e.printStackTrace();}
    }
}
