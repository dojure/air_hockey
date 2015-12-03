package ch.ethz.inf.vs.vs_bmaret_airhockey3x.game;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Created by Valentin on 14/11/15.
 *
 */

public class Player {

    private final static String LOGTAG = "Player";

    private final int mPosition;
    private boolean mReady = false;
    private boolean mConnected = false; // TODO: Must be updated when connection lost

    public Player(int pos)
    {
        mPosition = pos;
    }

    public int getPosition() {return mPosition;}
    public void setReady(boolean ready) {mReady = ready;}
    public boolean isReady() {return mReady;}
    public boolean isConnected() {return mConnected;}
    public void setConnected(boolean connected)
    {
        Log.d(LOGTAG,"Player " + Integer.toString(mPosition) + " is now " + Boolean.toString(connected) + " connected");
        mConnected = connected;}
}
