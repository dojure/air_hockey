package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.text.LoginFilter;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valentin on 15/11/15.
 *
 * This class handles all the connections with Bluetooth devices. This includes
 * connecting to devices, listening for incoming connections and transmitting data.
 *
 * TODO: IMPORTANT !! Error handling and testing
 */
public class BluetoothServices {

    private final String LOGTAG = "BluetoothServices";

    private static final String NAME = "AirHockey3X";

    private BluetoothServicesListener mListener;
    private final BluetoothAdapter mAdapter;

    private ConnectThread mConnectThread;
    private ListenThread mListenThread;

    private static UUID[] mUUIDs;

    private List<String> mDeviceAddresses;  // All addresses seen
    private HashMap<Integer,String> mPositionToAddressMap;  // Map positions to addresses - IMPORTANT
    private HashMap<String,BluetoothSocket> mSocketsMap;    // Map addresses to its sockets
    private HashMap<String,TransmissionThread> mTransmissionThreadMap; // Map addr to the thread handling the connection to there
    private HashMap<String,List<Byte[]>> mPendingMessages;  // Store pending messages if connection doesn't exist yet
    private HashMap<String,String> mAddtressToNameMap;

    public BluetoothServices(BluetoothServicesListener listener)
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mListener = listener;

        mSocketsMap = new HashMap<>();
        mDeviceAddresses = new ArrayList<>();
        mAddtressToNameMap = new HashMap<>();
        mTransmissionThreadMap = new HashMap<>();
        mPositionToAddressMap = new HashMap<>();
        mPendingMessages = new HashMap<>();

        // Basically we are always just trying if one works and if not we just
        mUUIDs = new UUID[6];
        mUUIDs[0] = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a60");
        mUUIDs[1] = UUID.fromString("503c7430-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7431-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7432-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7433-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7434-bc23-11de-8a39-0800200c9a66");
    }

    /**
     * This is a bit hacky. If we got connected through listening we need to associate that connection
     * to a position (for the mPositionToAddressMap) -> So we provide this method that the client
     * can call if he knows the position. We just associate it with the first (only hoepfully) address that
     * has not a position yet
     * @param pos   Position where last connected device belongs to
     */
    public synchronized String setPosForLastConnectedDevice(int pos)
    {
        // TODO: Some sanity check would be good.

        // Assuming only the last is not matched
        boolean found = false;
        String address = null;
        for (String addr : mDeviceAddresses) {
            if(!mPositionToAddressMap.containsValue(addr)) {
                mPositionToAddressMap.put(new Integer(pos),addr);
                address = addr;
                found = true;
            }
        }
        String name = mAddtressToNameMap.get(address);
        if (!found) {
            Log.d(LOGTAG,"Didnt find new address, which had to be associated with its position");
            return null;
        } else if (name != null) return name;
        else Log.d(LOGTAG, "No name was to be found for address");
        return null;
    }

    /**
     * Set new pair of position to address mapping.
     * @param pos           Position
     * @param deviceAddress Address associated with this position
     */
    public synchronized void setPosForAddress(int pos, String deviceAddress)
    {
        // TODO: Some sanity check would be good.

        Log.d(LOGTAG,"Set address " + deviceAddress + " for device at position " + Integer.toString(pos));
        mPositionToAddressMap.put(new Integer(pos), deviceAddress);
    }

    /**
     * Address associated with given position
     * @param pos   Position of interest
     * @return      Address of device at this position
     */
    public synchronized String getAddressForPoisition(int pos)
    {
        String addr = mPositionToAddressMap.get(new Integer(pos));
        if (addr == null || addr.equals("")) Log.d(LOGTAG,"No address for position " + Integer.toString(pos));
        return addr;
    }

    /**
     * Listen for incoming connections.
     * Start new thread if the Listen thread is not running yet.
     */
    public synchronized void listen()
    {
        Log.d(LOGTAG, "Start listening for connections");

        if (mListenThread == null) {
            mListenThread = new ListenThread();
            mListenThread.start();
        } else Log.d(LOGTAG,"Listen thread already running");

    }

    /**
     * Initiate new connection to remote device at given address.
     * @param deviceAddress    Device address to connect to
     */
    public synchronized void connect(String deviceAddress, int playerPos)
    {
        String deviceAdress = deviceAddress;
        mPositionToAddressMap.put(playerPos, deviceAdress);
        mConnectThread = new ConnectThread(deviceAdress);
        mConnectThread.start();
    }

    /**
     * Start handling connection
     * @param   deviceAddr address od other device
     */
    public synchronized void transmit(String deviceAddr)
    {
        Log.d(LOGTAG, "Connected - start transmitting");

        String name = mAddtressToNameMap.get(deviceAddr);
        if (name == null) Log.d(LOGTAG, "There exists no name for address " + deviceAddr);
        else {
            mListener.onConnected(deviceAddr, name);
        }

        TransmissionThread t = new TransmissionThread(mSocketsMap.get(deviceAddr), deviceAddr);
        t.start();
        addNewTransmissionThread(t, deviceAddr);
    }

    /**
     * Add this thread to the collection. Check if there are any pending messages for this thread
     * to send, if so send them right away.
     * @param t             Thread to add
     * @param deviceAddr    Associated device address
     */
    private void addNewTransmissionThread(TransmissionThread t, String deviceAddr)
    {
        mTransmissionThreadMap.put(deviceAddr, t);
        Log.d(LOGTAG,"Check on mTransmissionThreadMap after put() " + Integer.toString(mTransmissionThreadMap.size()));
        List<Byte[]> pending = mPendingMessages.get(deviceAddr);
        if (pending != null) {
            for (Byte[] msg : pending) {
                byte[] bytes = new byte[msg.length];
                for (int i = 0; i < msg.length; i++) {
                    bytes[i] = msg[i].byteValue();
                }
                t.send(bytes);
            }
            mPendingMessages.remove(deviceAddr);
        }
    }

    /**
     * Send bytes to remote device at given position
     * If the connection is not established yet, store message and send later.
     * @param pos         receiver position
     * @param bytes       Bytes to send
     */
    public void send(int pos, byte[] bytes)
    {
        Log.d(LOGTAG, "Sending bytes");
        String deviceAddress = mPositionToAddressMap.get(new Integer(pos)); // Get address
        if (deviceAddress == null) Log.d(LOGTAG,"Didnt find receiver address");
        TransmissionThread transmissionThread = mTransmissionThreadMap.get(deviceAddress);
        if (transmissionThread != null) transmissionThread.send(bytes);
        else {
            // Store message because connection not established yet
            Log.d(LOGTAG, "Couldnt send message because transmission thread doesnt exists; added bytes to pending list");
            Byte[] toSend = new Byte[bytes.length];
            for (int i = 0; i < toSend.length; i++) {
                toSend[i] = new Byte(bytes[i]);
            }
            List<Byte[]> pending = mPendingMessages.get(deviceAddress);
            if (pending != null) pending.add(toSend);
            else pending = new ArrayList<>();
            pending.add(toSend);
            mPendingMessages.put(deviceAddress,pending); // Replace old list
        }
    }

    /**
     * Cancel all threads
     */
    public synchronized void reset()
    {
        Log.d(LOGTAG,"Stop and reset everything");

        // TODO: Make sure this gets called somewhere appropriate

        // Cancel any threads currently trying to establish connection to other
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel all threads doing transmissions - closes also all sockets in mSocketsMap
        // TODO: WHY IS THIS EMPTY ??
        Log.d(LOGTAG,"Check on mTransmissionThreadMap in reset() " + Boolean.toString(mTransmissionThreadMap.isEmpty()));
        for (TransmissionThread t : mTransmissionThreadMap.values()) {
            if (t != null) t.cancel();
        }
        // Cancel all threads that are listening
        if (mListenThread != null) {
            mListenThread.cancel();
            mListenThread = null;
        }

        // TODO: Maybe a bit harsh?
        mPendingMessages.clear();
        mSocketsMap.clear();
        mTransmissionThreadMap.clear();
        mPositionToAddressMap.clear();
        mDeviceAddresses.clear();
        mAddtressToNameMap.clear();
    }

    /**
     * Unregister listener
     * @param listener
     */
    public void unregisterListener(BluetoothServicesListener listener)
    {
        // TODO: Make sure that this gets called at an appropriate place
        if (mListener == listener) { // Must be same mListener of course
            mListener = null;
        }
    }

    /**
     * Handle connection failure.
     *
     * TODO: IMPORTANT !!
     * Figure out where we can have connection errors. -> In all three thread below probably
     * A connection error may arise due to different reasons. for example one player is not reachable
     * or he disables bluetooth etc.
     * Handle them properly. We probably need to inform the listener too
     */
    private void connectionFailed(String address)
    {
        Log.d(LOGTAG, "Connection failed for address " + address);

        // Reverese search the position
        int position = -1;
        for (int p : mPositionToAddressMap.keySet()) {
            String addr = mPositionToAddressMap.get(p);
            if (addr != null && addr.equals(address)) {
                position = p;
            }
        }

        if (position >= 0) {
            mListener.onConnectionLost(position);
            mPositionToAddressMap.remove(position);
        } else Log.d(LOGTAG,"No position found for address " + address + " in connectionFailed");

        TransmissionThread t = mTransmissionThreadMap.get(address);
        if (t != null) {
            t.cancel();
            mTransmissionThreadMap.remove(address);
        }
        mDeviceAddresses.remove(address);
        mPendingMessages.remove(address);
        mSocketsMap.remove(address); // Socket is cloesd by t.cancel() above
        mAddtressToNameMap.remove(address);
    }


    /**
     * Threads
     *
     * TODO: IMPORTANT !! Error handling and testing
     */


    /**
     * Thread that listens for incoming connections
     *
     */
    private class ListenThread extends Thread {

        public ListenThread() {}

        public void run()
        {
            try {
                for (UUID uuid : mUUIDs) {
                    if (uuid == null) {
                        Log.d(LOGTAG,"Somehow, uuid was null, skip");
                        continue;
                    } else Log.d(LOGTAG,"Listen with uuid " + uuid.toString());
                    BluetoothServerSocket serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME,uuid);
                    if (serverSocket == null) continue;
                    BluetoothSocket socket = serverSocket.accept();
                    serverSocket.close(); // Close now that connection has been made
                    // Connection has been made

                    String addr = socket.getRemoteDevice().getAddress();
                    String name = socket.getRemoteDevice().getName();
                    mDeviceAddresses.add(addr);
                    mSocketsMap.put(addr, socket);
                    if (name != null) mAddtressToNameMap.put(addr, name);
                    else Log.d(LOGTAG,"Remote name was null");

                    transmit(addr);
                }
                Log.d(LOGTAG,"Listened to all UUIDs");
            } catch(IOException e) {
                e.printStackTrace();
                Log.d(LOGTAG,"IOException in ListenThread");
            }
        }

        public void cancel()
        {
            Log.d(LOGTAG,"Cancel ListenThread");
            // TODO: Cleanup
        }
    }


    /**
     * Thread to make connection with remote device.
     */
    private class ConnectThread extends Thread {

        private String addr;

        public ConnectThread(String deviceAddress) {addr = deviceAddress;}

        public void run()
        {
            BluetoothDevice server = mAdapter.getRemoteDevice(addr);
            BluetoothSocket socket = null;

            // Just try UUIDs one after another until one works
            for (int i = 0; i < mUUIDs.length && socket == null; i++){
                UUID uuid = mUUIDs[i];
                if (uuid != null) Log.d(LOGTAG, "Try connecting with uuid " + uuid.toString());
                else Log.d(LOGTAG,"Somehow uuid was null while trying to connect");
                for (int j= 0 ; j < 3 && socket == null; j++) {
                    socket = getConnectedSocket(server,uuid);
                    if (socket == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {e.printStackTrace();}
                    }
                }
            }

            if (socket == null) Log.d(LOGTAG,"Tried all UUIDs but couldnt make connection");
            mSocketsMap.put(addr, socket);
            String name = socket.getRemoteDevice().getName();
            if (name != null) mAddtressToNameMap.put(addr, name);
            else Log.d(LOGTAG,"Remote name was null");
            transmit(addr);
        }

        public void cancel()
        {
            Log.d(LOGTAG,"Cancel ConnectThread");
            // TODO: Cleanup ?
        }

    }

    /**
     * Try to establish connection to given socket with given UUID. Can very well fail
     * @param deivce    Device to which we want to connect
     * @param uuidToTry Try this uuid
     * @return          Connected socket if worked
     */
    private BluetoothSocket getConnectedSocket(BluetoothDevice deivce, UUID uuidToTry)
    {
        BluetoothSocket myBSock;
        try {
            myBSock = deivce.createRfcommSocketToServiceRecord(uuidToTry);
            myBSock.connect();
            return myBSock;
        } catch (IOException e) {
            Log.d(LOGTAG, "IOException in getConnectedSocket - uuid probably already in use");
        }
        return null;
    }

    /**
     * This thread handles all incoming and outgoing transmissions
     */
    private class TransmissionThread extends Thread {

        private final InputStream mIn;
        private final OutputStream mOut;
        private final BluetoothSocket mSocket;
        private final String mAddress;

        public TransmissionThread(BluetoothSocket socket, String address)
        {
            // Set in and output stream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mSocket = socket;
            mAddress = address;
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {e.printStackTrace();}

            mIn = tmpIn;
            mOut = tmpOut;
        }

        public void run()
        {
            Log.d(LOGTAG,"Start TransmissionThread");

            // TODO: Reasonable size for the array?
            byte[] buf = new byte[1024];

            // Receiving
            while(true) {
                try {
                    int n = mIn.read(buf);
                    synchronized (mListener) {
                        mListener.onReceiveBytes(buf, n);
                    }
                } catch (IOException e) {
                    Log.d(LOGTAG, "Connection lost - Start listening again for incoming connections");
                    connectionFailed(mAddress);
                    listen(); // TODO: yes? no? Call connectionerror?
                    e.printStackTrace();
                    return;
                }
            }
        }

        /**
         * Send bytes to remote device
         * @param buf   Bytes to send
         */
        public void send(byte[] buf)
        {
            Log.d(LOGTAG, "Sending bytes to remote device");
            try {
                mOut.write(buf);
            } catch (IOException e) {e.printStackTrace();}
        }

        /**
         * Cancel the transmission thread.
         */
        public void cancel()
        {
            Log.d(LOGTAG,"Cancel TransmissionThread - closing socket.");
            try {
                mIn.close();
                mOut.close();
                mSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

}
