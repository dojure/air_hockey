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
    private Boolean mScanning = false;

    private List<BluetoothDevice> mDevices = new ArrayList<>();
    private List<BluetoothDevice> mPairedDevices = new ArrayList<>();



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
        }

        // Add already paired mDevices - maybe do more intelligently
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice d : pairedDevices) {
            Log.d(LOGTAG, "Device already paired: " + d.getName());

            // Should we only consider mDevices with nonnull name??
            if (d != null && d.getName() != null) {
                mDevices.add(d);
                pairedDevices.add(d);
                mListener.onDeviceFound(d.getName());
            }
        }

        mBS = new BluetoothServices(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(receiver, filter);
    }


    // Let client unregister - 'this' is not needed anymore -> do cleanup
    public void unregisterListener(BluetoothCommListener lis)
    {
        mBS.unregisterListener(this);
        if (mListener == lis) { // Must be same mListener of course
            mListener = null;
            mContext.unregisterReceiver(receiver); // TODO: Not working? get all devices found twice in LOG?
        }
    }

    /**
     * Try to connect to device with given name
     * @param name  Name of device
     */
    public void connectTo(String name)
    {
        // Just connect to first with this name. Maybe we should base decision on other factor?
        for (BluetoothDevice d : mDevices) {
            if(d.getName().equals(name)) {
                mBS.connect(d);
                break;
            }
        }
    }

    /**
     * Listen for incoming connections
     */
    public void listen() { mBS.listen();}

    /**
     *
     * @param msg       msg to be sent
     * @param receiver  Player to receive it
     */
    public void sendMessageToPlayer(JSONObject msg, Player receiver)
    {
        // TODO
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

    public void onReceiveBytes(byte[] bytes)
    {

    }

    public void onConnected(BluetoothDevice device)
    {
        // TODO: Somehow link this device to the player which it belongs to. We do however not want to
        // pass the device object to listener.

        Log.d(LOGTAG, "Connected to " + device.getName() + " send mock message");

        // Send test message
        byte[] msg = new MessageFactory().createMessage(MessageFactory.MOCK_MESSAGE,-1,null);
        mBS.send(msg);
    }

}
