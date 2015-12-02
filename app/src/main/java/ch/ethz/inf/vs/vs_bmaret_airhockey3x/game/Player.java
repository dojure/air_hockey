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
    private String mBDeviceAddress = null;
    private int mRandId;
    private boolean mReady = false;

    public Player(int pos)
    {
        mPosition = pos;
        mRandId = (int) Math.random()*2048;
    }

    // Getter and setter for BluetoothDevice
    public void setBDevice(String deviceAddress) {mBDeviceAddress = deviceAddress;}
    public String getBDevice() {return mBDeviceAddress;}
    public int getRandId() {return mRandId;}

    public int getPosition() {return mPosition;}
    public void setReady(boolean ready) {mReady = ready;}
    public boolean isReady() {return mReady;}
    public boolean isConnected() {return mBDeviceAddress != null;}
}
