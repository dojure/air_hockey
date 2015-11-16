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
    private BluetoothDevice[] mPlayerDevices = new BluetoothDevice[3]; // Array of counterplayer devices
    private int mCurrentPlayer = -1;



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
     * Try to connect to device with given name
     * @param name  Name of device
     */
    public void connectTo(String name)
    {
        if (mEnable) {
            // Just connect to first with this name. Maybe we should base decision on other factor?
            for (BluetoothDevice d : mDevices) {
                if(d.getName().equals(name)) {
                    mBS.connect(d);
                    break;
                }
            }
        }
    }

    /**
     * The client of this class is responsible to keep inform this class about which of the players
     * is the current player. This applies for SetupActivity
     * @param currentPlayer     Current player (selected button) 1 <= pl <= 3
     */
    public void setCurrentPlayer(int currentPlayer)
    {
        if (currentPlayer < 4 && currentPlayer > 0) mCurrentPlayer = currentPlayer;
        else Log.d(LOGTAG,"Invalid currentPlayer");
    }

    /**
     * Listen for incoming connections
     */
    public void listen(Boolean enable)
    {
        if (mEnable) {
            if (enable) {
                makeDiscoverable();
                mBS.listen();
            } else {
                // TODO: cancel listening and discoverability
            }
        }
    }

    /**
     *
     * @param msg       msg to be sent
     * @param receiver  Player to receive it
     */
    public void sendMessageToPlayer(JSONObject msg, int receiver)
    {
        if (mEnable && receiver < 4 && receiver > 0) {
            BluetoothDevice recDevice = mPlayerDevices[receiver-1];
            byte[] bytes = MessageFactory.msgToBytes(msg);

            // TODO: Make that can send to specified device not just the one that happens to be connected
            mBS.send(bytes);
        }
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

    // Ensure discoverability
    private void makeDiscoverable()
    {
        if(mEnable) {
            if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                mContext.startActivity(i);
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
    }

    public void onConnected(BluetoothDevice device)
    {
        Log.d(LOGTAG, "Connected to " + device.getName());
        if (mCurrentPlayer < 0) Log.d(LOGTAG,"mCurrentPlayer not set");
        else mPlayerDevices[mCurrentPlayer - 1] = device; // Store device
    }

}
