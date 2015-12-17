package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.ACKSetupMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.InviteMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.Message;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.ReadyMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.TestMessage;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game.Game;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.game.Player;

/**
 * Created by Valentin on 14/11/15.
 *
 * The setup phase takes place in here. There are two major states: mActive = true and mActive = false.
 * True is only the one which enters the screen via the PLAY button. The others see a frozen
 * setup screen. The functionality varies. For example with mActive = false the user cannot press
 * any buttons
 *
 * Until now this is programmed for three players. We could do it for 2-4 but this would be a considerable effort
 *
 *
 * Protocol (Real world):
 *
 * Up until now only this works -> Must still make it fool proof
 * 1. All users mutst have bluetooth enabled
 * 2. All users open app
 * 3. One (!) user presses PLAY button and invites the other two
 *      the other do nothing and wait until all is good
 *
 *
 * TODO: IMPORTANT !!
 * 1. Test cases where users press buttons out of the ordinary protocol
 * 2. Test case where two users go to the setup screen and try to invite other users -> Can only have one leader
 * 3. Test pairing within the app -> does not always work. In worst case can still pair everyone before
 *        starting the app
 * 4. When everyone ticket the ready checkbox, go to gameActivity
 * 5. The buttons are ImageButtons up until now. It suffices for them to be normal buttons
 * 6. When leaving the setupScreen all progress is lost on the screen (but not in BluetootComm
 * since it is a singleton) Do something about that
 */


public class SetupActivityLeader extends AppCompatActivity
        implements View.OnClickListener, BluetoothCommListener {


    private final static String LOGTAG = "SetupActivityLeader";

    private Game mGame;
    private BluetoothComm mBC;
    private ListView mDevicesListView;
    private ArrayAdapter<String> mAdapter;
    private ImageButton[] mImageButtons = new ImageButton[3];
    private Player mCurrentPlayer = null;
    private boolean[] mSetupEnteredAcks = new boolean[4];
    private boolean[] mSetupAllConnectedAcks = new boolean[4];
    private boolean[] mReadyCounters = new boolean[4];
    //private int mSetupEnteredACKReceived = 0;
    //private int mSetupAllConnectedAcksReceived = 0;
    //private int mReadyCounter = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "onCreate");
        setContentView(R.layout.activity_setup_activity_leader);

        ImageButton b1 = (ImageButton) findViewById(R.id.player1_btn);
        mImageButtons[0] = b1;
        b1.setOnClickListener(this);
        ImageButton b2 = (ImageButton) findViewById(R.id.player2_btn);
        mImageButtons[1] = b2;
        b2.setOnClickListener(this);
        ImageButton b3 = (ImageButton) findViewById(R.id.player3_btn);
        mImageButtons[2] = b3;
        b3.setOnClickListener(this);

        CheckBox cb = (CheckBox) findViewById(R.id.ready_ckbox);
        cb.setOnClickListener(this);

        // DEBUG - Test message button - remove later
        Button b = (Button) findViewById(R.id.test_msg_btn1);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.test_msg_btn3);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.scan_button);
        b.setOnClickListener(this);

        b = (Button) findViewById(R.id.broadcast_button);
        b.setOnClickListener(this);

        // TODO: Ask user how many players -> only when want to do more than 3
        //showDialog();

        // Initialize the ListView
        // Callback for clicking on ListView
        mDevicesListView = (ListView) findViewById(R.id.devices_list);
        mDevicesListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View arg1, int position, long arg3) {
                        String entry = (String) parent.getItemAtPosition(position);
                        if (mCurrentPlayer != null) {
                            // Invite other player
                            Log.d(LOGTAG, "Check onItemClickListener of List");
                            scan(false); // Stop scan to make it faster

                            // Invite player if not already invited
                            if (!mGame.existsName(entry)) {
                                mBC.invite(mCurrentPlayer.getPosition(), entry);
                            } else {
                                Log.d(LOGTAG,"There is already a player with name " + entry +
                                        " -> not going to invite again");
                                showDialog(ALREADY_INVITED_DIALOG,entry);
                            }
                        } else Log.d(LOGTAG, "mCurrentPlayer is null - cannot invite");
                    }

                });

        initGame(3);

        mBC = BluetoothComm.getInstance();
        mBC.setNoConnections(mGame.getNrPlayer());
        mBC.registerListener(this);

        /*
        //TODO: this is for onResume()
        TextView ownName = (TextView)findViewById(R.id.player0_name);
        ownName.setText(mBC.getDeviceName());
        */

        scan(true); // Scan for devices
        setEnableListView(false);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOGTAG, "onResume");
        TextView ownName = (TextView) findViewById(R.id.player0_name);
        ownName.setText(mBC.getDeviceName());

        mBC.registerListener(this);

        CheckBox ready = (CheckBox) findViewById(R.id.ready_ckbox);
        ready.setChecked(false);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy");
        mBC.stop();

        // TODO: More cleanup ?
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        Log.d(LOGTAG, "onDestroy");
        scan(false);
        mBC.unregisterListener(this);

        for (int i = 0; i < 4; i++) setReady(false,i);
    }


    @Override
    public boolean onNavigateUp() {
        Log.d(LOGTAG, "onNavigateUo()");
        mBC.stop();
        return super.onNavigateUp();
    }

    /**
     * The idea is that the user clicks on one of the other players squares and that he can then
     * select one of the devices on the list. We need then to establish the connection to the other
     * etc..
     *
     * Players sit like this
     *    2
     *  1   3
     *    0
     */
    public void onClick(View b)
    {
        // Deselect all others
        for (ImageButton ib : mImageButtons) {
            if(!b.equals(ib)) ib.setSelected(false);
        }
        switch (b.getId()) {
            case R.id.player1_btn:
                if (!b.isSelected()) {
                    setEnableListView(true); // Now the user can select the the device for this position
                    b.setSelected(true);
                    setCurrentPlayer(mGame.getPlayer(1));
                } else b.setSelected(false);
                break;
            case R.id.player2_btn:
                if (!b.isSelected()) {
                    setEnableListView(true); // Now the user can select the the device for this position
                    b.setSelected(true);
                    setCurrentPlayer(mGame.getPlayer(2));
                } else b.setSelected(false);
                break;
            case R.id.player3_btn:
                if (!b.isSelected()) {
                    setEnableListView(true); // Now the user can select the the device for this position
                    b.setSelected(true);
                    setCurrentPlayer(mGame.getPlayer(3));
                } else b.setSelected(false);
                break;
            case R.id.scan_button:
                    scan(true);
                break;
            case R.id.ready_ckbox:
                ReadyMessage msg;
                if (((CheckBox) b).isChecked()) {
                    setReady(true, 0);
                    msg = new ReadyMessage(Message.BROADCAST, true);
                }
                else {
                    setReady(false,0);
                    msg = new ReadyMessage(Message.BROADCAST, false);
                }
                mBC.sendMessage(msg);
                break;

            // DEBUG
            case R.id.test_msg_btn1:
                Message msg0 = new TestMessage(1);
                mBC.sendMessage(msg0);
                break;
            case R.id.test_msg_btn3:
                Message msg1 = new TestMessage(3);
                mBC.sendMessage(msg1);
                break;
            case R.id.broadcast_button:
                Message msg2 = new TestMessage(Message.BROADCAST);
                mBC.sendMessage(msg2);
                break;
        }
        // Check if no button is selected -> need to disable list if none is and invalidate current player
        boolean sel = false;
        for (ImageButton ib : mImageButtons) {
            if(ib.isSelected()) sel = true;
        }
        if (!sel) {
            setEnableListView(false);
            setCurrentPlayer(null);
        }
    }

    /**
     *
     * BluetoothComm callbacks
     *
     */

    public void onNotDiscoverable() {Log.d(LOGTAG, "Unused callback called");}

    /**
     * TODO: Could also only put name into list. This would also imply changes in BluetoothComm
     *
     * New device was found -> need to put into list
     * @param name     Name
     * @param address   Address
     */
    public void onDeviceFound(String name, String address)
    {
        if (mAdapter == null) {
            // First call -> initialize Listadapter
            List<String> entries = new ArrayList<>();
            //String entry = name + " " + address;
            Log.d(LOGTAG, "initialize Listadapter and clear entries");
            entries.clear();
            //entries.add(entry);
            entries.add(name);
            mAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,entries);
            mDevicesListView.setAdapter(mAdapter);
        } else {
            // Use Listadapter which is already initialized
            Log.d(LOGTAG,"add entry to list adapter");
            //String entry = name + " " + address;
            //mAdapter.add(entry);
            mAdapter.add(name);
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Scan is done. Adjust button and progress bar.
     */
    public void onScanDone()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button b = (Button) findViewById(R.id.scan_button);
                b.setEnabled(true);
                ProgressBar p = (ProgressBar) findViewById(R.id.progress_scan);
                p.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Start connecting. -> Display progress bar
     */
    public void onStartConnecting()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
                p.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Player is connected. Invite him into game
     * @param pos   Position where the other player is located
     */
    public void onPlayerConnected(int pos, final String name)
    {
        // Let progressbar disappear
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Deselect all buttons
                for (ImageButton ib : mImageButtons) ib.setSelected(false);
                setEnableListView(false); // Disable list
                ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
                bar.setVisibility(View.GONE);
            }
        });

        // Send invite message to connected player (if mActive)
        mGame.getPlayer(pos).setConnected(true);
        mGame.getPlayer(pos).setName(name);
        ImageButton b = null;
        TextView nameField = null;
        Message msg = new InviteMessage(pos);
        mBC.sendMessage(msg);
        switch (pos) {
            case 1:
                b = (ImageButton) findViewById(R.id.player1_btn);
                nameField = (TextView) findViewById(R.id.player1_name);
                break;
            case 2:
                b = (ImageButton) findViewById(R.id.player2_btn);
                nameField = (TextView) findViewById(R.id.player2_name);
                break;
            case 3:
                b = (ImageButton) findViewById(R.id.player3_btn);
                nameField = (TextView) findViewById(R.id.player3_name);
                break;
        }

        // Change button color and add name
        if (b!= null && nameField != null) {
            final ImageButton button = b;
            final TextView nameF = nameField;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOGTAG, "Changing background of button...");
                        button.setImageResource(R.drawable.occupied_selector);
                        button.setSelected(false);
                        button.setClickable(false);
                        nameF.setText(name);
                    }
                });
            } catch (NullPointerException e) {e.printStackTrace();}
        }

        // Check if we are connected to everyone - if so send ack to inviter (Relevant for mActive = false)
        List<Player> players = mGame.getAllPlayers();
        int connectedPlayers = 1; // We consider ourselves as connected
        for (Player p : players) {
            if (p.isConnected()) connectedPlayers++;
        }
        if (connectedPlayers == mGame.getNrPlayer()) {
            Log.d(LOGTAG, "We are connected to all other players.");
        }
    }

    public void onPlayerDisconnected(final int pos)
    {
        final int position = pos;
        mGame.getPlayer(pos).setConnected(false);
        mGame.getPlayer(pos).setName(null);

        // TODO: Think about this
        //mReadyCounter = Math.max(0,mReadyCounter -1);
        //mSetupAllConnectedAcksReceived = Math.max(0,mSetupAllConnectedAcksReceived -1);
        //mSetupEnteredACKReceived = Math.max(0,mSetupEnteredACKReceived -1);
        mSetupAllConnectedAcks[pos] = false;
        mSetupEnteredAcks[pos] = false;
        mReadyCounters[pos] = false;

        ImageButton button = null;
        TextView nameField = null;

        switch (pos){
            case 1:
                button = (ImageButton) findViewById(R.id.player1_btn);
                nameField = (TextView) findViewById(R.id.player1_name);
                break;
            case 2:
                button = (ImageButton) findViewById(R.id.player2_btn);
                nameField = (TextView) findViewById(R.id.player2_name);
                break;
            case 3:
                button = (ImageButton) findViewById(R.id.player3_btn);
                nameField = (TextView) findViewById(R.id.player3_name);
                break;
        }

        final ImageButton b = button;
        final TextView nameF = nameField;


        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                showDialog(CONNECTION_LOST_DIALOG,mGame.getPlayer(position).getName());

                try {
                    b.setImageResource(R.drawable.vacant_selector);
                    b.setClickable(true);
                    nameF.setText("");
                    CheckBox ready = (CheckBox) findViewById(R.id.ready_ckbox);
                    ready.setChecked(false);
                    ready.setEnabled(false);
                } catch (NullPointerException e) {e.printStackTrace();}
            }
        });


    }

    /**
     * Handle incoming message
     * @param msg   Message
     */
    public void onReceiveMessage(Message msg)
    {
        if (msg == null) {
            Log.d(LOGTAG, "Received null message");
            return;
        }
        String msgType = msg.getType();
        Log.d(LOGTAG, "Received message with type " + msgType);
        switch (msgType) {
            case Message.TEST_MSG:
                showMessage(msg);
                break;
            case Message.ACK_SETUP_MSG:
                ACKSetupMessage ack = new ACKSetupMessage(msg);
                handleAckMessage(ack);
                break;
            case Message.READY_MSG:
                ReadyMessage rmsg = new ReadyMessage(msg);
                setReady(rmsg.getReady(), rmsg.getSender());
                break;
            case Message.INVITE_MSG:
            case Message.INVITE_REMOTE_MSG:
            default:
        }

    }

    public void onBluetoothNotSupported() {Log.d(LOGTAG,"Called unused callback onBluetoothNotSupported");}

    /**
     *
     * Helper functions
     *
     */

    private void setReady(boolean ready, int pos)
    {
        //mReadyCounter++;
        mReadyCounters[pos] = ready;
        //Log.d(LOGTAG,"Increment ready counter is now " + mReadyCounter);
        int readyPlayers = 0; // We consider ourselves as connected
        for (boolean b : mReadyCounters) {
            if (b) readyPlayers++;
        }
        Log.d(LOGTAG,Integer.toString(readyPlayers) + " players are ready");
        if (readyPlayers == 3) { // TODO: Change for more players
            Intent i2 = new Intent(this, AndroidLauncher.class);
            startActivity(i2);
        }
    }

    /**
     * Handle incoming ACKSetupMessage
     * @param ack   Received message
     */
    private void handleAckMessage(ACKSetupMessage ack)
    {
        int sender = ack.getSender();
        if (ack.getAckCode() == ACKSetupMessage.ENTERED_SETUP_ACTIVITY) {
            Log.d(LOGTAG,"Received ENTERED_SETUP_ACTIVITY ack from player " + ack.getSender());
            //mSetupEnteredACKReceived++;
            mSetupEnteredAcks[sender] = true;
            int ackno = 0;
            for (boolean a : mSetupEnteredAcks) {
                if(a) ackno++;
            }
            if (ackno == mGame.getNrPlayer()-1) {
                Log.d(LOGTAG,"All other players have entered the setup screen -> start remote inviting");
                mBC.remoteInvite(1,3); // TODO: Make general this is only for three players
            }
        } else if (ack.getAckCode() == ACKSetupMessage.ALL_CONNECTED) {
            Log.d(LOGTAG,"Received ALL_CONNECTED ack from player " + ack.getSender());
            //mSetupAllConnectedAcksReceived++;
            mSetupAllConnectedAcks[sender] = true;
            int ackno1 = 0;
            for (boolean a : mSetupEnteredAcks) {
                if(a) ackno1++;
            }
            if (ackno1 == mGame.getNrPlayer()-1) {
                Log.d(LOGTAG,"All other players have all their connections ready");
                // Broadcast ALL_CONNECTED s.t. the others can also makre their ready checkbox visible
                ACKSetupMessage ack1 = new ACKSetupMessage(Message.BROADCAST,ACKSetupMessage.ALL_CONNECTED);
                mBC.sendMessage(ack1);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        CheckBox cb = (CheckBox) findViewById(R.id.ready_ckbox);
                        cb.setEnabled(true);
                    }
                });
            }
        }
    }

    private final int PLACEHOLDER_DIALOG = 0;
    private final int ALREADY_INVITED_DIALOG = 1;
    private final int CONNECTION_LOST_DIALOG = 2;
    private void showDialog(int dialogId, String arg)
    {
        switch (dialogId) {
            case PLACEHOLDER_DIALOG:
                AlertDialog.Builder dialog  = new AlertDialog.Builder(this);
                dialog.setMessage(getString(R.string.pl_holder_no_participants));
                dialog.setTitle("PLACEHODLER");
                dialog.setPositiveButton("OK", null);
                dialog.setCancelable(true);
                dialog.create().show();
                break;
            case ALREADY_INVITED_DIALOG:
                AlertDialog alertDialog0 = new AlertDialog.Builder(SetupActivityLeader.this).create();
                alertDialog0.setTitle(getString(R.string.already_invited_title));
                alertDialog0.setMessage(getString(R.string.already_invited_message1) + arg +
                        getString(R.string.already_invited_message2));
                alertDialog0.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Deselect all buttons
                                for (ImageButton ib : mImageButtons) ib.setSelected(false);
                                setEnableListView(false); // Disable list
                                dialog.dismiss();
                            }
                        });
                alertDialog0.show();
                break;
            case CONNECTION_LOST_DIALOG:
                AlertDialog alertDialog1 = new AlertDialog.Builder(SetupActivityLeader.this).create();
                alertDialog1.setTitle(R.string.connection_lost_title);
                String errorMsg = getString(R.string.connection_lost_message1) + arg + getString(R.string.connection_lost_message2);
                alertDialog1.setMessage(errorMsg);
                alertDialog1.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Deselect all buttons
                                for (ImageButton ib : mImageButtons) ib.setSelected(false);
                                setEnableListView(false); // Disable list
                                dialog.dismiss();
                            }
                        });
                alertDialog1.show();
                break;
        }
    }

    /**
     * Show given message in a dialog to user
     * @param msg   Message to display
     */
    private void showMessage(Message msg)
    {
        final Message finalMsg = msg;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog alertDialog = new AlertDialog.Builder(SetupActivityLeader.this).create();
                alertDialog.setTitle("DEBUG");
                alertDialog.setMessage("Got a message !! Receiver at pos: "
                        + Integer.toString(finalMsg.getSender()) + " Message type: " +
                        finalMsg.getType());
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                setEnableListView(false);
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
    }

    /**
     * Enables or disables list view. Can only select items if enabled
     * @param enable    Enable or disable
     */
    private void setEnableListView(boolean enable)
    {
        mDevicesListView.setEnabled(enable);
        if (enable) mDevicesListView.setAlpha((float) 1);
        else mDevicesListView.setAlpha((float) 0.5); // Must do manually
    }

    /**
     * Start or stop scan. Also adjsut button and progress bar
     * @param enable    Start/stop
     */
    private void scan(boolean enable)
    {
        Button b = (Button)findViewById(R.id.scan_button);
        ProgressBar p = (ProgressBar) findViewById(R.id.progress_scan);
        if (enable) {
            p.setVisibility(View.VISIBLE);
            b.setEnabled(false);
        } else {
            p.setVisibility(View.GONE);
            b.setEnabled(true);
        }

        // clear device list before start a new scan
        if(mAdapter != null && enable){
            Log.d(LOGTAG, "clear device list");
            mAdapter.clear();
        }
        mBC.scan(enable);
    }

    /**
     * Sets current player. A player is considered 'current' iff the corresponding button is selected
     * Disconnect devices of all when none is current.
     * @param p     Current player
     */
    private void setCurrentPlayer(Player p)
    {
        if (p != null)  Log.d(LOGTAG,"Setting current player: " + Integer.toString(p.getPosition()));
        else Log.d(LOGTAG, "Set current player to null");

        // TODO: Do we still need special functionality for this
        mCurrentPlayer = p; // Even if -1
    }

    /**
     * Initializes game with given number of players
     * @param nrPlayers Number of players that take part in game
     */
    private void initGame(int nrPlayers)
    {
        mGame = Game.getInstance();
        mGame.setNrPlayer(nrPlayers);
        mGame.addPlayer(new Player(0)); // Self
        switch (nrPlayers) {
            case 2:
                mGame.addPlayer(new Player(2));
                break;
            case 3:
                mGame.addPlayer(new Player(1));
                mGame.addPlayer(new Player(3));
                break;
            case 4:
                mGame.addPlayer(new Player(1));
                mGame.addPlayer(new Player(2));
                mGame.addPlayer(new Player(3));
                break;
        }
    }

}
