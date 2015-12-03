package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by Valentin on 03/12/15.
 */
public class Message {

    // Define types
    public final static String TEST_MSG = "test";
    public final static String INVITE_MSG = "invite";
    public final static String INVITE_REMOTE_MSG = "invite_remote";

    // Keys occuring in msg
    protected final static String HEADER_KEY = "header";
    protected final static String BODY_KEY = "body";
    protected final static String SENDER_KEY = "sender";
    protected final static String TYPE_KEY = "type";


    protected JSONObject mMsg;
    protected JSONObject mHeader;
    protected JSONObject mBody; // Let subclass set
    protected String mType;
    protected int mSender;

    protected Message(String type, int senderId)
    {
        // TODO: Check input
        mType = type;
        mSender = senderId;

        try {
            mMsg = new JSONObject();
            mHeader = new JSONObject();
            mHeader.put(TYPE_KEY,mType);
            mHeader.put(SENDER_KEY,senderId);
            mMsg.put(HEADER_KEY,mHeader);
        } catch (JSONException e) {e.printStackTrace();}
    }

    public Message(byte[] bytes, int noBytes)
    {
        // TODO: Error handling
        try {
            if (bytes != null && bytes.length != 0) {
                String tmp = new String(bytes, 0, noBytes, "UTF-8");
                mMsg = new JSONObject(tmp);
                mHeader = mMsg.getJSONObject(HEADER_KEY);
                if (mMsg.has(BODY_KEY)) mBody = mMsg.getJSONObject(BODY_KEY);
                mType = mHeader.getString(TYPE_KEY);
                mSender = mHeader.getInt(SENDER_KEY);
            }
        }
        catch (JSONException e) {e.printStackTrace();}
        catch (UnsupportedEncodingException e) {e.printStackTrace();}
    }

    protected Message(Message msg)
    {
        // TODO: Error handling
        try {
            mMsg = msg.toJSON();
            mHeader = mMsg.getJSONObject(HEADER_KEY);
            if (mMsg.has(BODY_KEY)) mBody = mMsg.getJSONObject(BODY_KEY);
            mType = mHeader.getString(TYPE_KEY);
            mSender = mHeader.getInt(SENDER_KEY);
        } catch(JSONException e) {e.printStackTrace();}

    }

    public int getSender() {return mSender;}
    public String getType() {return mType;}

    public byte[] toBytes()
    {
        if (mMsg != null) return mMsg.toString().getBytes();
        else return null;
    }

    public JSONObject toJSON() {return mMsg;}

}
