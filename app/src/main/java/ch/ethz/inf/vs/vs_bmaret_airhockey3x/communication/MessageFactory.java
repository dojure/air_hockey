package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by Valentin on 15/11/15.
 *
 * Defines all messages that can be sent. Provides methods to get message.
 *
 * TODO: It would probably be more ellegant to have a message class; s.t. the message is an object
 * and the clients dont have to deal with JSON
 */
public class MessageFactory {

    // Define types
    public final static String MOCK_MESSAGE = "mock";

    // Keys occuring in msg
    private final static String HEADER_KEY = "header";
    private final static String BODY_KEY = "body";
    private final static String PLAYER_KEY = "player";
    private final static String TYPE_KEY = "type";

    public MessageFactory() {}

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
     * @param playerId  Sender id
     * @param body      Body of message, use provided method for respective type to create body
     * @return          byte array ready to send - may change to JSONObj and then seperately conv to bytes
     */
    public JSONObject createMessage(String type, int playerId, JSONObject body)
    {
        JSONObject msg = new JSONObject();
        JSONObject header = new JSONObject();

        try {
            header.put(PLAYER_KEY,playerId);
            header.put(TYPE_KEY,type);
            msg.put(HEADER_KEY,header);
            msg.put(BODY_KEY,body);
        } catch (JSONException e) {e.printStackTrace();}

        return msg;
    }

}

