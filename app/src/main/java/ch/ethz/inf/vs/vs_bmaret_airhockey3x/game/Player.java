package ch.ethz.inf.vs.vs_bmaret_airhockey3x.game;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Valentin on 14/11/15.
 *
 * Note Bluetooth: We keep the BluetoothDevice in the player because that way it can easily be
 * passed on between Activities and in particular to the game since the game object which contains
 * the players is a singleton.
 */

public class Player {

    private final int mPosition;
    private BluetoothDevice mBDevice;

    public Player(int pos)
    {
        mPosition = pos;
    }

    // Getter and setter for BluetoothDevice
    public void setBDevice(BluetoothDevice device) {mBDevice = device;}
    public BluetoothDevice getBDevice() {return mBDevice;}

    public int getPosition() {return mPosition;}
}
