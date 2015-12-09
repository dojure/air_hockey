package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.InviteMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.Message;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.RemoteInviteMessage;

/**
 * Created by Valentin on 14/11/15.
 *
 * This is the only class that gets called from outside the package. It provides all necessary
 * services to the communication layer clients.
 *
 * This class is a Singleton. That way we can easyily keep the connections and other communication
 * related state during all the activities.
 *
 * TODO s (in addition the others already in the code)
 * 1. Think about the cleanup. When to call .stop() and what must be done?
 * 2. Could change the names (for the list) to only the name of the device rather than the name and
 *      the address
 * 3. Test pairing from within the application. For some devices it works and for some not.. Maybe
 *      some hidden Bluetooth preferences that i'm not aware of. In the worst case we can still pair
 *      all devices BEFORE we start the app. Also sometimes when the 'other two' are not paired yet,
 *      they might not connect
 */
public class BluetoothComm implements BluetoothServicesListener {

    private final String LOGTAG = "BluetoothComm";

    private BluetoothCommListener mListener; // Allow only one mListener
    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServices mBS;
    private int mNoConnections = -1; // Nr of connections that we must support

    private List<BluetoothDevice> mDevices = new ArrayList<>(); // Devices from scan or already paired
    private int mCurrentPlayerPos = -1;

    // Callback for BluetoothAdapter
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d(LOGTAG,"Found device " + device.getName());
                    // If it's already paired, skip it, because it's been listed already
                    if (device != null ) {  //&& !mDevices.contains(device) && device.getBondState() != BluetoothDevice.BOND_BONDED  (commented out)

                        // Consider only mDevices with nonnull name??
                        if (device.getName() != null) {
                            mDevices.add(device);
                            String name = device.getName();
                            String address = device.getAddress();
                            mListener.onDeviceFound(name, address);
                        }
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Log.d(LOGTAG,"Scanning done");
                    mListener.onScanDone();
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        mListener.onNotDiscoverable();
                    }
                    break;
            }
        }
    };


    private static BluetoothComm ourInstance = new BluetoothComm();
    public static BluetoothComm getInstance() {return ourInstance;}

    public void init(BluetoothCommListener listener, Context appContext)
    {
        mContext = appContext;
        mListener = listener;

        mBS = new BluetoothServices(this);

        // Get Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        // If this is not successful exit.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.d(LOGTAG,"Bluetooth not enabled or not supported");

            // TODO: Exit gracefully if Bluetooth is not supported
            // TODO: If Bluetooth is just not enabled, prompt a dialog to enable it

            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

//        //test clear devices
//        mDevices.clear();

        // Register bluetooth callback
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        mContext.registerReceiver(receiver, filter);
    }

    /**
     * Register BluetoothCommListener; Note we allow only for one listener
     * @param listener  Listener
     */
    public void registerListener(BluetoothCommListener listener)
    {
        if(listener != null) mListener = listener;
        else Log.d(LOGTAG,"Tried to register null-listener");
    }

    /**
     * Unregister BluetoothCommListener.
     * @param listener  Listener
     */
    public void unregisterListener(BluetoothCommListener listener)
    {
        if (mListener == listener) { // Must be same mListener of course
            mListener = null;
        }
    }

    /**
     * This class needs to know about the number of players in order to be able to broadcast messages.
     * @param noConnections     Number of players
     */
    public void setNoConnections(int noConnections) {mNoConnections = noConnections;}

    /**
     * Get the names of all the devices that are already paired.
     * @return  List of all names and addresses (In one entry) of devices that are already paired
     */
//    public List<String> getPairedDeviceNamesAdresses()
//    {
//        List<String> result = new ArrayList<>();
//        // Add already paired mDevice
//        for (BluetoothDevice d : mBluetoothAdapter.getBondedDevices()) {
//            Log.d(LOGTAG, "Device already paired: " + d.getName());
//
//            // We take only devices with nonnull name (OK ?)
//            if (d != null && d.getName() != null) {
//                mDevices.add(d);
//                String name = d.getName();
//                String address = d.getAddress();
//                result.add(name + " " + address);
//            }
//        }
//        return result;
//    }

    /**
     * Start or stop scan for devices
     * @param enable    Wheter to start ot stop the scan
     */
    public void scan(boolean enable)
    {
        if(mBluetoothAdapter != null) {
            if (enable && !mBluetoothAdapter.isDiscovering()) {
                Log.d(LOGTAG,"Scanning..");
                mBluetoothAdapter.startDiscovery();
            }
            else if (!enable && mBluetoothAdapter.isDiscovering()) {
                Log.d(LOGTAG,"Stop scanning");
                mBluetoothAdapter.cancelDiscovery();
            }
        } else Log.d(LOGTAG,"Cannot scan - bluetoothAdapter was null");
    }

    /**
     * Stop BluetoothServices
     */
    public void stop()
    {
        // TODO: Cleanup when done -> maybe something about the bluetooth callback
        // Also figure out a good place to call this
        mBS.stop();
    }


    /**
     * Listen for incoming connections
     * @param enable    Either listen or disconnect (??)
     */
    public void listen(Boolean enable)
    {
        // TODO: Rethink the what to do when enable is false
        Log.d(LOGTAG, "Enable listening: " + Boolean.toString(enable));
        if (enable) mBS.listen();
        //else mBS.stop();
    }

    /**
     * Make device discoverable such that other bluetooth devices can find it via discovery, or cancel
     * the visibility. Note that the user gets asked in both cases to allow the action.
     * @param enable    Wheter to let it be discoverable or not
     */
    public void discoverable(boolean enable)
    {
        Log.d(LOGTAG, "Make discoverable " + Boolean.toString(enable));
        if(enable && mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            listen(false); // Stop listening if were listening
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            mContext.startActivity(i);
            listen(true); // Start again
        } else if (!enable && mBluetoothAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1); // Hacky but ok
            mContext.startActivity(i);
        }
    }
    /**
     * Invite a player (device) to the game.
     * Set the device of mCurrentPlayer to the invited device
     *
     * @param playerPos  Player to whom the device should be associated to
     * @param entry      Name of device
     */
    public void invite(int playerPos, String entry)
    {
        if(playerPos >= 0 && playerPos <= 3) {
            mCurrentPlayerPos = playerPos;

            // TODO: Check whether this player was already invited and has a seat in the game
            // It should not be possible that a device occupies 2 seats in the game

            for (BluetoothDevice d : mDevices) {
                String compare = d.getName() + " " + d.getAddress();
                if (compare.equals(entry)) {
                    // Open a connection to the device at the specific player
                    mBS.connect(d.getAddress(), mCurrentPlayerPos);
                    mListener.onStartConnecting();
                }
            }
        } else Log.d(LOGTAG, "Tried to get paired device for null-Player");
    }

    /**
     * Tell a player that he should connect to an other player
     * @param p1    The player who gets told
     * @param p2    The player to whom p1 should connect
     */
    public void remoteInvite(int p1, int p2)
    {
        // We tell p1 the address of p2; the chance is great that p1 doesnt know p2 yet
        String addressP2 = mBS.getAddressForPoisition(p2);
        Message msg = new RemoteInviteMessage(p1,p2,addressP2);
        mBS.send(p1, msg.toBytes());
    }

    /**
     * Sends a message (Receiver specified in message)
     * @param msg       msg to be sent
     */
    public void sendMessage(Message msg)
    {
        if (msg != null) {
            if (msg.getReceiver() == Message.BROADCAST) {
                Log.d(LOGTAG,"Broadcasting message");

                // Not so cool because we use information about the game
                switch (mNoConnections) {
                    case 1:
                        msg.setReceiver(2);
                        mBS.send(2,msg.toBytes());
                        break;
                    case 2:
                        msg.setReceiver(1);
                        mBS.send(1,msg.toBytes());
                        msg.setReceiver(3);
                        mBS.send(3,msg.toBytes());
                        break;
                    case 3:
                        for (int i = 1; i < 4; i++) {
                            msg.setReceiver(i);
                            mBS.send(i,msg.toBytes());
                        }
                        break;
                    default:
                        Log.d(LOGTAG,"No connections not properly set, cannot broadcast.");
                }
            }
            else {
                Log.d(LOGTAG,"Sending message " + msg.getType() + " to receiver at pos " + msg.getReceiver());
                mBS.send(msg.getReceiver(), msg.toBytes());
            }
        } else Log.d(LOGTAG,"There is a problem sending a message to a receiver");
    }

    /**
     *
     * BluetoothServices callbacks
     *
     */

    /**
     * Handle incoming messages (messages firs in byte form)
     * Note that the message Object gets also passed to the BluetoothCommListener -> Not all messages
     * must be handled here. Only messages that do not need to concern the listener.
     * @param bytes     Message as bytes
     * @param noBytes   Number of bytes
     */
    public void onReceiveBytes(byte[] bytes, int noBytes)
    {
        Message msg = new Message(bytes, noBytes);
        String msgType = msg.getType();
        Log.d(LOGTAG,"Receiving message " + msgType);
        mListener.onReceiveMessage(msg);

        switch (msgType) {
            case Message.INVITE_MSG:
                mBS.setPosForLastConnectedDevice(msg.getSender());
                mListener.onPlayerConnected(msg.getSender());
                mCurrentPlayerPos = -1; // Not sure if needed bcz we are dealing at the moment with the sender
                break;
            case Message.INVITE_REMOTE_MSG:
                RemoteInviteMessage mRem = new RemoteInviteMessage(msg);
                int absPos = mRem.getTargetPos();
                String deviceAddress = mRem.getAddress();
                Log.d(LOGTAG, "Got remote invite messgae with abspos " + Integer.toString(absPos) +
                        " and address " + deviceAddress);
                mCurrentPlayerPos = 4-absPos;
                mBS.setPosForAddress(mCurrentPlayerPos, deviceAddress);
                mBS.connect(deviceAddress, mCurrentPlayerPos);
                mListener.onStartConnecting();
                InviteMessage invm = new InviteMessage(mCurrentPlayerPos);
                mBS.send(mCurrentPlayerPos, invm.toBytes());
                break;
        }
    }

    /**
     * Device with given address was connected.
     * @param deviceAddr    Address of connected device
     */
    public void onConnected(String deviceAddr)
    {
        Log.d(LOGTAG, "Connected to " + deviceAddr);
        if (mCurrentPlayerPos != -1) {
            mListener.onPlayerConnected(mCurrentPlayerPos); // Notify listener
            mCurrentPlayerPos = -1;
        } else Log.d(LOGTAG,"mCurrenPlayer was -1!");

    }

    /**
     * change device name to simplify setup for the user
     * @param name  new device name
     */
    public void changeDeviceName(String name){
        mBluetoothAdapter.setName(name);
    }

    //may not needed anymore....
    public void clearDeviceList(){
        mDevices.clear();
    }
}
