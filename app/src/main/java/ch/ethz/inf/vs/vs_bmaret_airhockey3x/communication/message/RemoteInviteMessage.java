package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 03/12/15.
 */
public class RemoteInviteMessage extends Message {

    private final static String ABS_POS_KEY = "abs_pos";
    private final static String ADDRESS_KEY = "address";

    private int mAbsPos;
    private String mAddress;

    public RemoteInviteMessage(String type, int senderId, int absPos, String address) {
        super(type, senderId);

        mAbsPos = absPos;
        mAddress = address;

        try {
            mBody = new JSONObject();
            mBody.put(ABS_POS_KEY, mAbsPos);
            mBody.put(ADDRESS_KEY, mAddress);
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public RemoteInviteMessage(byte[] bytes, int noBytes)
    {
        super(bytes, noBytes);

        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mAbsPos = mBody.getInt(ABS_POS_KEY);
            mAddress = mBody.getString(ADDRESS_KEY);
        } catch (JSONException e) {e.printStackTrace();}
    }

    public RemoteInviteMessage(Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mAbsPos = mBody.getInt(ABS_POS_KEY);
            mAddress = mBody.getString(ADDRESS_KEY);
        } catch (JSONException e) {e.printStackTrace();}

    }

    public int getAbsPos() {return mAbsPos;}
    public String getAddress() {return mAddress;}

}
