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
import java.util.Set;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Player;

/**
 * Created by Valentin on 14/11/15.
 *
 * This is the only class that gets called from without the package. It provides all necessary
 * services to the communication layer clients.
 *
 */

public class BluetoothComm implements BluetoothServicesListener {

    // Constants
    private final String LOGTAG = "BluetoothComm";

    private BluetoothCommListener mListener; // Allow only one mListener
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothServices mBS;
    private MessageFactory mMF;
    private Boolean mScanning = false;
    private Boolean mEnable = false;

    // Be carful to keep this consistent
    private List<BluetoothDevice> mDevices = new ArrayList<>(); // List of paired and discovered devices
    private List<BluetoothDevice> mPairedDevices = new ArrayList<>(); // List of paired devices
    private Player mCurrentPlayer = null;



    public BluetoothComm(BluetoothCommListener lis, Context appContext)
    {
        mListener = lis;

        // Get Bluetooth adapter
        mContext = appContext;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.d(LOGTAG,"Bluetooth not enabled");

            // TODO: Show this in Activity somehow
            //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mEnable = true;

            // Add already paired mDevices - maybe do more intelligently
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice d : pairedDevices) {
                Log.d(LOGTAG, "Device already paired: " + d.getName());

                // Should we only consider mDevices with nonnull name??
                if (d != null && d.getName() != null) {
                    mDevices.add(d);
                    mListener.onDeviceFound(d.getName());
                }
            }

            mMF = new MessageFactory();
            mBS = new BluetoothServices(this);

            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            mContext.registerReceiver(receiver, filter);
        }
    }


    // Let client unregister - 'this' is not needed anymore -> do cleanup
    public void unregisterListener(BluetoothCommListener lis)
    {
        if (mEnable) {
            mBS.unregisterListener(this);
            if (mListener == lis) { // Must be same mListener of course
                mListener = null;
                mContext.unregisterReceiver(receiver); // TODO: Not working? get all devices found twice in LOG?
            }
        }
    }

    /**
     * Requests paired device for given player
     * @param player    Player to whom the device should be associated to
     * @param name      Name of device
     */
    public void requestPairedDevice(Player player, String name)
    {
        if (mEnable) {
            if(player != null) {
                mCurrentPlayer = player;
                for (BluetoothDevice d : mDevices) {
                    if(d.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.d(LOGTAG, "Device already paired - add to player");

                        // TODO:
                        // Not that we do not open a connection to the other player...
                        // Maybe we should to check if it works?
                        mCurrentPlayer.setBDevice(d); // Device already paired
                        break;
                    }
                    // Device not paired yet
                    if(d.getName().equals(name)) {
                        // This establishes a connection to the other device which involves pairing
                        // The connection is left open afterwards
                        mBS.connect(d);
                        break;
                    }
                }
            } else Log.d(LOGTAG,"Tried to get paired device for null-Player");
        }
    }


    public void disconnect()
    {
        if(mEnable) mBS.disconnect();
    }

    /**
     * Listen for incoming connections
     */
    public void listen(Boolean enable)
    {
        if (mEnable) {
            Log.d(LOGTAG,"Enable listening: " + Boolean.toString(enable));
            if (enable) mBS.listen();
            else mBS.disconnect();
        }
    }

    public void discoverable()
    {
        if(mEnable) {
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
    }

    /**
     *
     * @param msg       msg to be sent
     * @param receiver  Player to receive it
     */
    public void sendMessageToPlayer(JSONObject msg, Player receiver)
    {
        if (mEnable && msg != null && receiver != null) {
            Log.d(LOGTAG,"Sending message to receiver at pos " + receiver.getPosition());

            // TODO: Check if device ok?
            BluetoothDevice recDevice = receiver.getBDevice();
            mBS.send(recDevice, MessageFactory.msgToBytes(msg));

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
                        mListener.onDeviceFound(device.getName());
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
        if (mEnable) {
            // TODO: Listener only gets new devices. Somehow callback him with already paired devices
            if(!mScanning) {
                mScanning = true;
                mBluetoothAdapter.startDiscovery();
            }
        }
    }

    /**
     *
     * BluetoothServices callbacks
     *
     */

    public void onReceiveBytes(byte[] bytes, int noBytes)
    {
        JSONObject msg = MessageFactory.bytesToMsg(bytes, noBytes);
        Log.d(LOGTAG,"Receiving message " + mMF.getType(msg));
        mListener.onReceiveMessage(msg);
    }

    public void onConnected(BluetoothDevice device)
    {
        Log.d(LOGTAG, "Connected to " + device.getName());
        /*if (mCurrentPlayer < 0) Log.d(LOGTAG,"mCurrentPlayer not set");
        else mPlayerDevices[mCurrentPlayer - 1] = device; // Store device*/
        if(mCurrentPlayer != null) {
            if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                mCurrentPlayer.setBDevice(device);
                mCurrentPlayer = null;
            } else Log.d(LOGTAG,"Cannot set Bluetooth device because it's not paired");
        } else Log.d(LOGTAG,"Cannot set Bluetooth device because mCurrentPlayer is null");
    }

}
