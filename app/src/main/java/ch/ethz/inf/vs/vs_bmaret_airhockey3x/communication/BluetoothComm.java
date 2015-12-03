package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.InviteMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.Message;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.RemoteInviteMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Player;

/**
 * Created by Valentin on 14/11/15.
 *
 * This is the only class that gets called from outside the package. It provides all necessary
 * services to the communication layer clients.
 *
 */
public class BluetoothComm implements BluetoothServicesListener {

    private final String LOGTAG = "BluetoothComm";

    private BluetoothCommListener mListener; // Allow only one mListener
    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServices mBS;
    //private MessageFactory mMF;

    private Boolean mScanning = false;

    /** List of paired and discovered devices*/
    private List<BluetoothDevice> mDevices = new ArrayList<>();
    /** Player that is currently selected*/
    private Player mCurrentPlayer = null;


    private static BluetoothComm ourInstance = new BluetoothComm();
    public static BluetoothComm getInstance() {return ourInstance;}

    public void init(BluetoothCommListener listener, Context appContext)
    {
        mContext = appContext;
        mListener = listener;

        //mMF = new MessageFactory();
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

            // TODO: If the mEnable is still false, exit gracefully
        }


        // The pairing stuff used to be here before it was a singleton


        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(receiver, filter);
    }

    public void registerListener(BluetoothCommListener listener)
    {
        if(listener != null) mListener = listener;
        else Log.d(LOGTAG,"Tried to register null-listener");
    }

    // Let client unregister - 'this' is not needed anymore -> do cleanup
    public void unregisterListener(BluetoothCommListener listener)
    {
        //mBS.unregisterListener(this); // Not needed anymore because singleton??
        if (mListener == listener) { // Must be same mListener of course
            mListener = null;

            // TODO: Not working? get all devices found twice in LOG?
            mContext.unregisterReceiver(receiver);
        }
    }

    public List<String> getPairedDeviceNamesAdresses()
    {
        List<String> result = new ArrayList<>();
        // Add already paired mDevice
        // TODO: maybe do more intelligently
        for (BluetoothDevice d : mBluetoothAdapter.getBondedDevices()) {
            Log.d(LOGTAG, "Device already paired: " + d.getName());

            // TODO: Should we only consider mDevices with nonnull name??
            if (d != null && d.getName() != null) {
                mDevices.add(d);
                String name = d.getName();
                String address = d.getAddress();
                result.add(name + " " + address);
            }
        }
        return result;
    }

    /**
     * Invite a player (device) to the game.
     * Set the device of mCurrentPlayer to the invited device
     *
     * @param player    Player to whom the device should be associated to
     * @param entry      Name of device
     */
    public void invite(Player player, String entry)
    {
        if(player != null) {
            mCurrentPlayer = player;

            // TODO: Check whether this player was already invited and has a seat in the game
            // It should not be possible that a device occupies 2 seats in the game

            for (BluetoothDevice d : mDevices) {
                String compare = d.getName() + " " + d.getAddress();
                if (compare.equals(entry)) {

                    // Open a connection to the device at the specific player
                    mBS.connect(d.getAddress(), player.getPosition());

                    // Send a message with an invitation for the game
                    // TODO: Send a invitation message

                    // If the player accepted set the device of the mCurrentPlayer to d
                    // TODO: Check whether the player accepted the invitation, otherwise prompt an error
                    // TODO: If the player accepts the invitation he should get to the "frozen" set up screen

                    //mCurrentPlayer.setBDevice(d);

                    // There is no need to finish the loop
                    break;
                }
            }

        } else {
            Log.d(LOGTAG,"Tried to get paired device for null-Player");
        }
    }

    public void remoteInvite(Player p1, Player p2)
    {
        // Tell p1 to connect to p2
        String addressP2 = p2.getBDevice();
        //JSONObject msg = mMF.createMessage(MessageFactory.INVITE_REMOTE_MSG,
        //        0, mMF.remoteInviteMessageBody(p2.getPosition(),addressP2));
        Message msg = new RemoteInviteMessage(Message.INVITE_REMOTE_MSG,0,p2.getPosition(),addressP2);
        mBS.send(p1.getPosition(),msg.toBytes());
    }

    /**
     * Stop BluetoothServices - Also stops listening
     */
    public void disconnect() {
        mBS.stop();
    }


    /**
     * Listen for incoming connections
     * @param enable    Either listen or disconnect
     */
    public void listen(Boolean enable) {
        Log.d(LOGTAG,"Enable listening: " + Boolean.toString(enable));
        if (enable)
            mBS.listen();
        else
            mBS.stop();
    }

    /**
     * Make the device discoverable. This must be done if the two devices are not paired yet
     */
    public void discoverable()
    {
        Log.d(LOGTAG, "Make discoverable");
        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            listen(false); // Stop listening if were listening
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            mContext.startActivity(i);
            listen(true); // Start again
        }
    }

    /**
     * Sends a message to a specific player
     * @param msg       msg to be sent
     * @param receiver  Player to receive it
     */
    public void sendMessageToPlayer(Message msg, Player receiver)
    {
        if (msg != null && receiver != null) {
            Log.d(LOGTAG,"Sending message to receiver at pos " + receiver.getPosition());

            // TODO: Check if device ok?

            mBS.send(receiver.getPosition(), msg.toBytes());

        } else Log.d(LOGTAG,"There is a problem sending a message to a receiver");
    }

    // Listener to device scan
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(LOGTAG,"Found device " + device.getName());
                // If it's already paired, skip it, because it's been listed already
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED && !mDevices.contains(device)) {

                    // Consider only mDevices with nonnull name??
                    if (device.getName() != null) {
                        mDevices.add(device);
                        String name = device.getName();
                        String address = device.getAddress();
                        mListener.onDeviceFound(name, address);
                    }
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                // Scan done
                mScanning = false;
            }
        }
    };

    /**
     *  Scan for remote devices
     */
    public void scan()
    {
        // TODO: Listener only gets new devices. Somehow callback him with already paired devices
        if(!mScanning) {
            mScanning = true;
            mBluetoothAdapter.startDiscovery();
        }
    }

    /**
     *
     * BluetoothServices callbacks
     *
     */

    public void onReceiveBytes(byte[] bytes, int noBytes)
    {
        //JSONObject msg = MessageFactory.bytesToMsg(bytes, noBytes);
        Message msg = new Message(bytes, noBytes);
        String msgType = msg.getType();
        Log.d(LOGTAG,"Receiving message " + msgType);
        mListener.onReceiveMessage(msg);

        switch (msgType) {
            case Message.INVITE_MSG:
                /**
                 * S: 2      I: 1    S: 2      I: 3
                 * 1   3 --> 0   2,  1   3 --> 2   0
                 *   0         3       0         1
                 * E.g. if assigned pos =1 then the initiator is at pos 3 (first example)
                 */
                InviteMessage mInv = new InviteMessage(msg);
                int assignedPos = mInv.getAssignedPos();
                int senderPos = mInv.getSenderPos();
                int relPos = 4-assignedPos;
                mBS.setPosForLastConnectedDevice(relPos);
                mListener.onPlayerConnected(relPos);
                break;
            case Message.INVITE_REMOTE_MSG:
                RemoteInviteMessage mRem = new RemoteInviteMessage(msg);
                int absPos = mRem.getAbsPos();
                String deviceAddress = mRem.getAddress();
                Log.d(LOGTAG, "Got remote invite messgae with abspos " + Integer.toString(absPos) +
                        " and address " + deviceAddress);
                // TODO: General case this works only for 3
                relPos = 4-absPos;
                mBS.setPosForAddress(relPos,deviceAddress);
                mBS.connect(deviceAddress, relPos);
                mListener.onPlayerConnected(relPos);
                break;
        }
    }

    public void onConnected(String deviceAddr)
    {
        Log.d(LOGTAG, "Connected to " + deviceAddr);
        if (mCurrentPlayer != null) {
            mCurrentPlayer.setBDevice(deviceAddr);
            mListener.onPlayerConnected(mCurrentPlayer.getPosition());
        } else Log.d(LOGTAG,"mCurrenPlayer was null!");

    }

    /*
    /**
     * Get relative position of 'other'
     * @param own       Relative position with respect to 'other' of us
     * @param otherAbs  Absolute position of 'other'
     * @return          Relative position of 'other' with respect to us
     */
    /*
    private int gerRelativePosition(int own, int otherAbs, int numberOfPlayers)
    {
        int res = -1;
        switch (numberOfPlayers) {
            case 3:
                res = 4-own;
            default:
                Log.d(LOGTAG,"gerRelativePosition not implemented for " + Integer.toString(numberOfPlayers) + " players");

        }
        return res;
    }
    */
}
