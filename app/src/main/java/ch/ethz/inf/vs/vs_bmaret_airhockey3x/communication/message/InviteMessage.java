package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 03/12/15.
 */
public class InviteMessage extends Message {

    private final static String ASSIGNED_POS_KEY = "assigned_pos";
    private final static String SENDER_POS_KEY = "sender_pos";

    private int mAssignedPos;
    private int mSenderPos;

    public InviteMessage(String type, int senderId, int assignedPos, int senderPos) {
        super(type, senderId);

        mAssignedPos = assignedPos;
        mSenderPos = senderPos;

        try {
            mBody = new JSONObject();
            mBody.put(SENDER_POS_KEY, mSenderPos);
            mBody.put(ASSIGNED_POS_KEY,mAssignedPos);
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public InviteMessage(byte[] bytes, int noBytes)
    {
        super(bytes, noBytes);

        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mSenderPos = mBody.getInt(SENDER_POS_KEY);
            mAssignedPos = mBody.getInt(ASSIGNED_POS_KEY);
        } catch (JSONException e) {e.printStackTrace();}
    }

    public InviteMessage(Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mAssignedPos = mBody.getInt(ASSIGNED_POS_KEY);
            mSenderPos = mBody.getInt(SENDER_POS_KEY);
        } catch (JSONException e) {e.printStackTrace();}

    }

    public int getAssignedPos() {return mAssignedPos;}
    public int getSenderPos() {return mSenderPos;}


}
