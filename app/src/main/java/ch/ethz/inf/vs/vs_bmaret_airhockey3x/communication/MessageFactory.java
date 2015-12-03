package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.webkit.JsPromptResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;


/**
 *  DO NOT USE THIS CLASS. Use the Message class instead
 */


/**
 * Created by Valentin on 15/11/15.
 *
 * Defines all messages that can be sent. Provides methods to get message.
 *
 * TODO: It would probably be more elegant to have a message class; s.t. the message is an object
 * and the clients dont have to deal with JSON
 */
public class MessageFactory {

    // Define types
    public final static String MOCK_MSG = "mock";
    public final static String INVITE_MSG = "invite";
    public final static String INVITE_REMOTE_MSG = "invite_remote";

    // Keys occuring in msg
    private final static String HEADER_KEY = "header";
    private final static String BODY_KEY = "body";
    private final static String PLAYER_KEY = "player";
    private final static String TYPE_KEY = "type";
    private final static String ASSIGNED_POS_KEY = "assigned_pos";
    private final static String SENDER_POS_KEY = "sender_pos";
    private final static String ABS_POS_KEY = "abs_pos";
    private final static String ADDRESS_KEY = "address";


    private MessageFactory() {}

    /**
     * Converter msg -> bytes
     * @param msg   Message in form of JSONObject
     * @return      bytes representing the message
     */
    static byte[] msgToBytes(JSONObject msg)
    {
        if (msg != null) return msg.toString().getBytes();
        else return null;
    }

    /**
     * Converter bytes -> msg
     * @param bytes     bytes representing a JSONObject
     * @param noBytes   number of bytes in bytes
     * @return          Message in form of JSONObject
     */
    static JSONObject bytesToMsg(byte[] bytes, int noBytes)
    {
        // TODO: Handle cases where bytes are not actually a msg
        JSONObject msg = null;
        try {
            if (bytes != null && bytes.length != 0) {
                String tmp = new String(bytes, 0, noBytes, "UTF-8");
                msg = new JSONObject(tmp);
            }
        } catch (JSONException e) {e.printStackTrace();}
        catch (UnsupportedEncodingException e) {e.printStackTrace();}
        return msg;
    }


    // Returns player id of sender
    public int getSender(JSONObject msg)
    {
        int playerId = -1;
        try {
            JSONObject header = msg.getJSONObject(HEADER_KEY);
            playerId = header.getInt(PLAYER_KEY);
        } catch (JSONException e) {e.printStackTrace();}
        return playerId;
    }

    // Returns type of message
    public String getType(JSONObject msg)
    {
        String type = "";
        try {
            JSONObject header = msg.getJSONObject(HEADER_KEY);
            type = header.getString(TYPE_KEY);
        } catch (JSONException e) {e.printStackTrace();}
        return type;
    }


    /**
     * Create message.
     * TODO: This must likely still be extended. E.g. with receiver..
     * @param type      Type of message
     * @param senderId  Sender id
     * @return          byte array ready to send - may change to JSONObj and then seperately conv to bytes
     */
    public JSONObject createMessage(String type, int senderId, JSONObject body)
    {
        JSONObject msg = new JSONObject();
        JSONObject header = new JSONObject();
        try {
            header.put(PLAYER_KEY,senderId);
            header.put(TYPE_KEY,type);
            msg.put(HEADER_KEY,header);
            msg.put(BODY_KEY,body);
        } catch (JSONException e) {e.printStackTrace();}

        return msg;
    }


    /**
     * Invite message
     */

    public JSONObject inviteMessageBody(int assignedPos, int senderPos)
    {
        JSONObject body = null;
        try {
            body = new JSONObject();
            body.put(ASSIGNED_POS_KEY,assignedPos);
            body.put(SENDER_POS_KEY,senderPos);
        }catch (JSONException e) {e.printStackTrace();}
        return body;
    }

    public int getAssignedPosition(JSONObject msg)
    {
        int pos = -1;
        try {
            JSONObject body = msg.getJSONObject(BODY_KEY);
            pos = body.getInt(ASSIGNED_POS_KEY);
        } catch (JSONException e) {e.printStackTrace();}
        return pos;
    }
    public int getSenderPos(JSONObject msg)
    {
        int pos = -1;
        try {
            JSONObject body = msg.getJSONObject(BODY_KEY);
            pos = body.getInt(SENDER_POS_KEY);
        } catch (JSONException e) {e.printStackTrace();}
        return pos;
    }

    /**
     * Remote invite
     */

    public JSONObject remoteInviteMessageBody(int absPos, String address)
    {
        JSONObject body = null;
        try {
            body = new JSONObject();
            body.put(ABS_POS_KEY,absPos);
            body.put(ADDRESS_KEY,address);
        }catch (JSONException e) {e.printStackTrace();}
        return body;
    }


    public String getRemoteInviteAddress(JSONObject msg)
    {
        String address = "";
        try {
            JSONObject body = msg.getJSONObject(BODY_KEY);
            address = body.getString(ADDRESS_KEY);
        } catch (JSONException e) {e.printStackTrace();}
        return address;
    }

    public int getRemoteInviteAbsPos(JSONObject msg)
    {
        int absPos = -1;
        try {
            JSONObject body = msg.getJSONObject(BODY_KEY);
            absPos = body.getInt(ABS_POS_KEY);
        } catch (JSONException e) {e.printStackTrace();}
        return absPos;
    }

}

