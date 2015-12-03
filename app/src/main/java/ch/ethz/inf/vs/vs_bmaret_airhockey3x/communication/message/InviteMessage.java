package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 03/12/15.
 *
 * No special functionality in here; Consider making one class for all classes that have no special
 * functionality.
 */
public class InviteMessage extends Message {


    public InviteMessage(int receiverPos)
    {
        //super(Message.INVITE_MSG, senderId);
        super(receiverPos, Message.INVITE_MSG);

        try {
            mBody = new JSONObject();
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public InviteMessage(Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
        } catch (JSONException e) {e.printStackTrace();}

    }


}
