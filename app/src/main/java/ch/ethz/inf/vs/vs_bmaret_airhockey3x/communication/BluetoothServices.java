package ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Player;

/**
 * Created by Valentin on 15/11/15.
 *
 * This class handles all the connections with Bluetooth devices. This includes
 * connecting to devices, listening for incoming connections and transmitting data.
 */
public class BluetoothServices {

    private final String LOGTAG = "BluetoothServices";

    private static final String NAME = "AirHockey3X";

    private BluetoothServicesListener mListener;
    private final BluetoothAdapter mAdapter;

    private ConnectThread mConnectThread;
    private TransmissionThread mTransmissionThread;
    private ListenThread mListenThread;

    /**
     * Array to store a different UUID for each connection
     * TODO: Additional UUIDs needed?
     * Order:
     * From     To
     * 0        1
     * 0        2
     * 0        3
     * 1        2
     * 1        3
     * 2        3
     */
    private static UUID[] mUUIDs;

    private TransmissionThread[] mTransmissionThreads;
    private BluetoothSocket[] mSockets;
    private BluetoothDevice[] mDevices;

    private List<String> mDeviceAddresses;
    private HashMap<String,BluetoothSocket> mSocketsMap;
    private HashMap<String,TransmissionThread> mTransmissionThreadMap;


    // TODO: The state is not locked properly
    // TODO: Is it necessary?
    // TODO: Not properly updated
    //private int mState = STATE_NONE;

    // Current connection state
    // TODO: This changes also when have multiple connections?
    /*
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    */
    public BluetoothServices(BluetoothServicesListener listener)
    {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mListener = listener;

        //mTransmissionThreads = new TransmissionThread[4];
        mSockets = new BluetoothSocket[4];
        mDevices = new BluetoothDevice[4];

        // Try storing socket for address
        mSocketsMap = new HashMap<>();
        mDeviceAddresses = new ArrayList<>();
        mTransmissionThreadMap = new HashMap<>();

        mUUIDs = new UUID[6];
        mUUIDs[0] = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a60");
        mUUIDs[1] = UUID.fromString("503c7430-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7431-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7432-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7433-bc23-11de-8a39-0800200c9a66");
        mUUIDs[1] = UUID.fromString("503c7434-bc23-11de-8a39-0800200c9a66");
        }

    /**
     * Listen for incoming connections
     */
    public synchronized void listen()
    {
        Log.d(LOGTAG,"Start listening for connections");
/*
        // Cancel any threads currently trying to establish a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }
*/
        if (mListenThread == null) {
            mListenThread = new ListenThread();
            mListenThread.start();
        } else Log.d(LOGTAG,"Listen thread already running");

        //mState = STATE_LISTEN;
    }

    /**
     * Initiate new connection to remote device.
     * @param device    Device to connect to
     */
    public synchronized void connect(BluetoothDevice device, Player player)
    {
        Log.d(LOGTAG,"Attempting to connect to " + device.getName());
/*
        // Cancel any threads currently trying to establish connection to other
        if (mState == STATE_CONNECTING && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }
*/


        mConnectThread = new ConnectThread(device,player);
        mConnectThread.start();
    }

    /**
     * Start handling connection
     * @param   deviceAddr address od other device
     */
    //public synchronized void transmit(int playerPosition)
    public synchronized void transmit(String deviceAddr)
    {
        Log.d(LOGTAG,"Connected");

        /*mTransmissionThread = new TransmissionThread(mSockets[playerPosition]);
        mTransmissionThread.start();

        mTransmissionThreads[playerPosition] = mTransmissionThread;
        mTransmissionThread = null;

        mListener.onConnected(mSockets[playerPosition].getRemoteDevice());
        //mState = STATE_CONNECTED;*/

        mTransmissionThread = new TransmissionThread(mSocketsMap.get(deviceAddr));
        mTransmissionThread.start();
        mTransmissionThreadMap.put(deviceAddr,mTransmissionThread);
    }

    /**
     * Cancel all threads - Careful also the listening gets stopped
     */
    public synchronized void stop()
    {
        Log.d(LOGTAG,"Disconnect - Stop all threads");

        // Cancel any threads currently trying to establish connection to other
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel all threads doing transmissions
        if (mTransmissionThread != null) {
            mTransmissionThread.cancel();
            mTransmissionThread = null;
        }
        // Cancel all threads that are listening
        if (mListenThread != null) {
            mListenThread.cancel();
            mListenThread = null;
        }
        //mState = STATE_NONE;
    }

    /**
     * Send bytes to remote device
     * If receiver is null, send to connected device if any, if receiver is not null,
     * establish connection then send.
     * @param player  receiver of message
     * @param bytes       Bytes to send
     */
    public void send(Player player, byte[] bytes)
    {
        Log.d(LOGTAG, "Sending bytes");
        //mTransmissionThreads[player.getPosition()].send(bytes);
        String remoteDeviceAddr = player.getBDevice().getAddress();
        TransmissionThread transmissionThread = mTransmissionThreadMap.get(remoteDeviceAddr);
        transmissionThread.send(bytes);
    }

    // Let client unregister
    public void unregisterListener(BluetoothServicesListener listener)
    {
        if (mListener == listener) { // Must be same mListener of course
            mListener = null;
        }
    }

    private void connectionFailed()
    {
        Log.d(LOGTAG,"Connection failed");
    }


    /**
     *
     * Threads
     *
     */


    /**
     * Thread that listens for incoming connections
     *
     * TODO: Let this thread listen for multiple connections or have multiple of this threads
     */
    private class ListenThread extends Thread {

        private BluetoothServerSocket mServerSocket;

        public ListenThread() {}

        public void run()
        {
            try {
                for (UUID uuid : mUUIDs) {
                    BluetoothServerSocket serverSocket = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME,uuid);
                    BluetoothSocket socket = serverSocket.accept();
                    serverSocket.close(); // Close now that connection has been made
                    // Connection has been made

                    String addr = socket.getRemoteDevice().getAddress();
                    mDeviceAddresses.add(addr);
                    mSocketsMap.put(addr, socket);

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

        }
    }




    /**
     * Thread to make connection with remote device.
     */
    private class ConnectThread extends Thread {

            private int playerPosition;
            private String deviceAddress;

            public ConnectThread(BluetoothDevice device, Player player)
            {
                playerPosition = player.getPosition();
                deviceAddress = device.getAddress();

            }

            public void run()
            {
                BluetoothDevice server = mAdapter.getRemoteDevice(deviceAddress);
                BluetoothSocket socket = null;

                for (int i=0; i < mUUIDs.length && socket == null; i++){
                    UUID uuid = mUUIDs[i];
                    for (int j= 0 ; j < 3 && socket == null; j++) {
                        try {
                            socket = server.createInsecureRfcommSocketToServiceRecord(uuid);
                            socket.connect();
                        }catch (IOException e) {
                            Log.d(LOGTAG,"Could not connect with this UUID");
                        }
                        if (socket == null) {
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {e.printStackTrace();}
                        }
                    }
                }

                if (socket == null) Log.d(LOGTAG,"Tried all UUIDs but couldnt make connection");
                //mSockets[playerPosition] = socket;
                mSocketsMap.put(deviceAddress, socket);
                mDeviceAddresses.add(deviceAddress);
                TransmissionThread transmissionThread = new TransmissionThread(socket);
                transmissionThread.start();
                mTransmissionThreadMap.put(deviceAddress,transmissionThread);
            }




        public void cancel()
        {

        }

    }

    /**
     * This thread handles all incoming and outgoing transmissions
     */
    private class TransmissionThread extends Thread {

        private final BluetoothSocket mSocket;
        private final InputStream mIn;
        private final OutputStream mOut;

        public TransmissionThread(BluetoothSocket socket)
        {
            mSocket = socket;

            // Set in and output stream
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
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
                    // Tell listener about received bytes
                    mListener.onReceiveBytes(buf, n);
                }
            } catch (IOException e) {
                Log.d(LOGTAG, "Connection lost - Start listening again for incoming connections");
                //mState = STATE_LISTEN;
                listen();
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
         * Close the socket.
         */
        public void cancel()
        {
            Log.d(LOGTAG,"Cancel TransmissionThread - closing socket.");
            try {
                if (mSockets != null) mSocket.close();
            } catch (IOException e) {e.printStackTrace();}
        }
    }

}
