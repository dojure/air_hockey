package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by Valentin on 03/12/15.
 *
 * Subclass from this class. To have specialized messages for your purpose.
 * Provides Header; Body must be provided by subclass.
 * Define your type in here.
 */
public class Message {

    private final String LOGTAG = "Message";


    // Define types
    public final static String TEST_MSG = "test";
    public final static String INVITE_MSG = "invite";
    public final static String INVITE_REMOTE_MSG = "invite_remote";
    public final static String READY_MSG = "ready_message";
    public final static String ACK_SETUP_MSG = "ack_setup";
    public final static String PUCK_MOVEMENT_MSG = "puck_movement";
    public final static String SCORE_MSG = "score_message";
    public final static String EXIT_GAME_MSG = "exit_game";

    // Use as position to broadcast
    public final static int BROADCAST = -2;

    // Keys occuring in msg
    protected final static String HEADER_KEY = "header";
    protected final static String BODY_KEY = "body";
    protected final static String SENDER_KEY = "sender";
    protected final static String RECEIVER_KEY = "receiver";
    protected final static String TYPE_KEY = "type";

    protected JSONObject mMsg;
    protected JSONObject mHeader;
    protected JSONObject mBody; // Let subclass must set
    protected String mType;

    /**
     * IMPORTANT
     * The receiver is the position relative to the sender
     * The sender is the position relative to the receiver -> can be computed from sender
     */
    protected int mReceiver;
    protected int mSender;

    protected Message(int receiver, String type)
    {
        mType = type;
        mReceiver = receiver;
        mSender = 4 - receiver;

        try {
            mMsg = new JSONObject();
            mHeader = new JSONObject();
            mHeader.put(TYPE_KEY,mType);
            mHeader.put(RECEIVER_KEY,mReceiver);
            mHeader.put(SENDER_KEY,mSender);
            mMsg.put(HEADER_KEY,mHeader);
        } catch (JSONException e) {e.printStackTrace();}
    }

    /**
     * Get message object from bytes, e.g. when receiving a message.
     * @param bytes     Received bytes
     * @param noBytes   Number of received bytes
     */
    public Message(byte[] bytes, int noBytes)
    {
        try {
            if (bytes != null && bytes.length != 0) {
                String tmp = new String(bytes, 0, noBytes, "UTF-8");
                mMsg = new JSONObject(tmp);
                mHeader = mMsg.getJSONObject(HEADER_KEY);
                if (mMsg.has(BODY_KEY)) mBody = mMsg.getJSONObject(BODY_KEY);
                mType = mHeader.getString(TYPE_KEY);
                mReceiver = mHeader.getInt(RECEIVER_KEY);
                mSender = mHeader.getInt(SENDER_KEY);
            }
        }
        catch (JSONException e) {e.printStackTrace();}
        catch (UnsupportedEncodingException e) {e.printStackTrace();}
    }

    // Used in subclasses, when they want "cast" a Message Object to their specific type
    protected Message(Message msg)
    {
        try {
            mMsg = msg.toJSON();
            mHeader = mMsg.getJSONObject(HEADER_KEY);
            if (mMsg.has(BODY_KEY)) mBody = mMsg.getJSONObject(BODY_KEY);
            mType = mHeader.getString(TYPE_KEY);
            mReceiver = mHeader.getInt(RECEIVER_KEY);
            mSender = mHeader.getInt(SENDER_KEY);
        } catch(JSONException e) {e.printStackTrace();}

    }

    // Not so elegant, need for broadcast
    public void setReceiver(int receiver)
    {
        mReceiver = receiver;
        mSender = 4 - receiver;
        try {
            mHeader.put(RECEIVER_KEY,mReceiver);
            mHeader.put(SENDER_KEY,mSender);
            mMsg.put(HEADER_KEY,mHeader);
        } catch (JSONException e) {e.printStackTrace();}

    }
    public int getReceiver() {return mReceiver;}
    public int getSender() {return mSender;}
    public String getType() {return mType;}

    /**
     * Get bytes for sending.
     * @return  Bytes ready to send
     */
    public byte[] toBytes()
    {
        if (mMsg != null) return mMsg.toString().getBytes();
        else return null;
    }

    /**
     * Get the underlying JSON representation.
     * @return  JSON representation
     */
    public JSONObject toJSON() {return mMsg;}

}
