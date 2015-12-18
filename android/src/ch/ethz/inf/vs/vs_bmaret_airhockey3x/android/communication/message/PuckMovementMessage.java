package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message;


import com.badlogic.gdx.math.Vector2;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Rimle on 09.12.2015.
 *
 * subclass of Message
 * creates messages with information about the puck such es direction and speed, which can be sent to other players potentially receiving the puck
 *
 */

public class PuckMovementMessage extends Message {

    private final static String X_POSITION_KEY = "positionx";
    private final static String Y_POSITION_KEY = "positiony";

    private final static String X_VELOCITY_KEY = "velocityx";
    private final static String Y_VELOCITY_KEY = "velocityy";

    private float mXPos;
    private float mYPos;

    private float mXVel;
    private float mYVel;


    public PuckMovementMessage(int receiverPos, float xpos, float ypos, float xvel, float yvel)
    {
        super(receiverPos,Message.PUCK_MOVEMENT_MSG);

        mXPos = xpos;
        mYPos = ypos;

        mXVel = xvel;
        mYVel = yvel;

        try {
            mBody = new JSONObject();
            mBody.put(X_POSITION_KEY, mXPos);
            mBody.put(Y_POSITION_KEY, mYPos);
            mBody.put(X_VELOCITY_KEY, mXVel);
            mBody.put(Y_VELOCITY_KEY, mYVel);
            mMsg.put(BODY_KEY,mBody);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public PuckMovementMessage(Message msg)
    {
        super(msg);
        try {
            mBody = new JSONObject();
            mBody = mMsg.getJSONObject(BODY_KEY);
            mXPos = new Double(mBody.getDouble(X_POSITION_KEY)).floatValue();
            mYPos = new Double(mBody.getDouble(Y_POSITION_KEY)).floatValue();
            mXVel = new Double(mBody.getDouble(X_VELOCITY_KEY)).floatValue();
            mYVel = new Double(mBody.getDouble(Y_VELOCITY_KEY)).floatValue();
        } catch (JSONException e) {e.printStackTrace();}
    }

    public float getXPosition() {return mXPos;}
    public float getYPosition() {return mYPos;}
    public float getXVelocity() {return mXVel;}
    public float getYVelocity() {return mYVel;}


}
