package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 03/12/15.
 *
 * This message is used to tell a player he should connect to another player.
 */
public class RemoteInviteMessage extends Message {

    private final static String TARGET_POS_KEY = "target_pos";
    private final static String ADDRESS_KEY = "address";

    private int mTargetPos; // Player which should be invited by receiver of this message
    private String mAddress; // Address of this other player

    public RemoteInviteMessage(int receiverPos, int targetPos, String address)
    {
        super(receiverPos, Message.INVITE_REMOTE_MSG);

        mTargetPos = targetPos;
        mAddress = address;

        try {
            mBody = new JSONObject();
            mBody.put(TARGET_POS_KEY, mTargetPos);
            mBody.put(ADDRESS_KEY, mAddress);
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public RemoteInviteMessage(Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mTargetPos = mBody.getInt(TARGET_POS_KEY);
            mAddress = mBody.getString(ADDRESS_KEY);
        } catch (JSONException e) {e.printStackTrace();}

    }

    public int getTargetPos() {return mTargetPos;}
    public String getAddress() {return mAddress;}

}
