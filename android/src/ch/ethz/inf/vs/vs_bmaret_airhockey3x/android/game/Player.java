package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Created by Valentin on 14/11/15.
 *
 */

public class Player {

    private final static String LOGTAG = "Player";
    public final String defaultName = "The Name";

    private final int mPosition;
    private int mScore;
    private boolean mConnected = false; // TODO: Must be updated when connection lost
    private String mName = defaultName;

    public Player(int pos)
    {
        mPosition = pos;
    }

    public int getPosition() {return mPosition;}
    public boolean isConnected() {return mConnected;}
    public void setConnected(boolean connected)
    {
        Log.d(LOGTAG,"Player " + Integer.toString(mPosition) + " is now " + Boolean.toString(connected) + " connected");
        mConnected = connected;
    }

    public void setName(String name)
    {
        if (name == null) mName = defaultName;
        else mName = name;
    }
    public String getName() {return mName;}
    public boolean setScore(int score) {
        boolean scored = score > mScore;
        mScore = score;
        return scored;
    }
    public int getScore() {return mScore;}
}
