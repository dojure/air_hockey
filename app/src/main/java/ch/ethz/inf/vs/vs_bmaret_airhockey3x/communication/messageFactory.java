package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Valentin on 15/11/15.
 *
 * Defines all messages that can be sent. Provides methods to get message.
 *
 */
public class messageFactory {

    // Define types
    public final static String MOCK_MESSAGE = "mock";

    // Keys occuring in msg
    private final static String HEADER_KEY = "header";
    private final static String BODY_KEY = "body";
    private final static String PLAYER_KEY = "player";
    private final static String TYPE_KEY = "type";

    public messageFactory() {}

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
     *
     * Create message. This must likely still be extended.
     *
     * @param type      Type of message
     * @param playerId  Sender id
     * @param body      Body of message, use provided method for respective type to create body
     * @return          msg in form of JSON Object
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
