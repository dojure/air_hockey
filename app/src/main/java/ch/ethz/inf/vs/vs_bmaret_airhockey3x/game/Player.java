package ch.ethz.inf.vs.vs_bmaret_airhockey3x.game;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Valentin on 14/11/15.
 */

public class Player {

    private final int mPosition;
    private BluetoothDevice mBDevice;

    public Player(int pos)
    {
        mPosition = pos;
    }

    public void setBDevice(BluetoothDevice device) {mBDevice = device;}
    public BluetoothDevice getBDevice() {return mBDevice;}

    public int getPosition() {return mPosition;}
}
