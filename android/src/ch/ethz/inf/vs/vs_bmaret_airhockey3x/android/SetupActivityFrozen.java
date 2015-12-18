package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.message.ACKSetupMessage;
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


public class SetupActivityFrozen extends AppCompatActivity
        implements View.OnClickListener, BluetoothCommListener {

    private final static String LOGTAG = "SetupActivityFrozen";
    public final static String INVITER_POS = "inviter";
    public final static String INVITER_NAME = "inviter_name";

    private Game mGame;
    private int mInviter = -1; // The player which went first into the setup screen (only not -1 if !mActive)
    private BluetoothComm mBC;
    private ImageButton[] mImageButtons = new ImageButton[3];
    //private int mReadyCounter = 0;
    private boolean[] mReadyCounters = new boolean[4];



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        Log.d(LOGTAG, "onCreate");
        setContentView(R.layout.activity_setup_activity_frozen);

        /*
        ImageButton b1 = (ImageButton) findViewById(R.id.player1_btn);
        mImageButtons[0] = b1;
        b1.setOnClickListener(this);
        ImageButton b2 = (ImageButton) findViewById(R.id.player2_btn);
        mImageButtons[1] = b2;
        b2.setOnClickListener(this);
        ImageButton b3 = (ImageButton) findViewById(R.id.player3_btn);
        mImageButtons[2] = b3;
        b3.setOnClickListener(this);
        */


        CheckBox cb = (CheckBox) findViewById(R.id.ready_ckbox);
        cb.setOnClickListener(this);

        // DEBUG - Test message button - remove later
        Button b = (Button) findViewById(R.id.test_msg_btn1);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.test_msg_btn3);
        b.setOnClickListener(this);
        CheckBox cbD = (CheckBox) findViewById(R.id.discoverable_ckbox);
        cbD.setOnClickListener(this);

        initGame(3);

        mBC = BluetoothComm.getInstance();
        mBC.setNoConnections(mGame.getNrPlayer());
        //mBC.registerListener(this);

        /*
        if (mBC.isDiscoverable()) b.setEnabled(false);
        else b.setEnabled(true);
        */


        mInviter = getIntent().getIntExtra(INVITER_POS, -1);
        //String inviterName = getIntent().getStringExtra(INVITER_NAME);
        //if (inviterName == null || inviterName.equals("")) inviterName = getString(R.string.no_name);
        mGame.getPlayer(mInviter).setConnected(true);
        Message msg = new ACKSetupMessage(mInviter,ACKSetupMessage.ENTERED_SETUP_ACTIVITY);
        mBC.sendMessage(msg); // Send ACK

    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(LOGTAG, "onResume");

        mBC.registerListener(this);

        CheckBox cb = (CheckBox) findViewById(R.id.discoverable_ckbox);
        if (mBC.isDiscoverable()) cb.setChecked(true);
        else cb.setChecked(false);

        ImageButton b1 = (ImageButton) findViewById(R.id.player1_btn);
        mImageButtons[0] = b1;
        b1.setOnClickListener(this);
        ImageButton b2 = (ImageButton) findViewById(R.id.player2_btn);
        mImageButtons[1] = b2;
        b2.setOnClickListener(this);
        ImageButton b3 = (ImageButton) findViewById(R.id.player3_btn);
        mImageButtons[2] = b3;
        b3.setOnClickListener(this);

        // Change button color of inviter to greem
        TextView nameField = null;
        switch (mInviter) {
            case 1:
                b1.setImageResource(R.drawable.occupied_selector);
                nameField = (TextView) findViewById(R.id.player1_name);
                break;
            case 2:
                b2.setImageResource(R.drawable.occupied_selector);
                nameField = (TextView) findViewById(R.id.player2_name);
                break;
            case 3:
                b3.setImageResource(R.drawable.occupied_selector);
                nameField = (TextView) findViewById(R.id.player3_name);
                break;
        }
        String inviterName = getIntent().getStringExtra(INVITER_NAME);
        if (inviterName == null || inviterName.equals("")) inviterName = getString(R.string.no_name);
        nameField.setText(inviterName);
        mGame.getPlayer(mInviter).setName(inviterName);

        TextView ownName = (TextView)findViewById(R.id.player0_name);
        String name = mBC.getDeviceName();
        ownName.setText(name);
        mGame.getPlayer(0).setName(name);
        mGame.resetScores();

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
        Log.d(LOGTAG, "onStop");
        mBC.unregisterListener(this);

        for (int i = 0; i < 4; i++) setReady(false,i);
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
        switch (b.getId()) {
            case R.id.player1_btn:
                break;
            case R.id.player2_btn:
                break;
            case R.id.player3_btn:
                break;
            case R.id.discoverable_ckbox:
                if (((CheckBox) b).isChecked()) mBC.discoverable(true);
                else mBC.discoverable(false);
                break;
            case R.id.ready_ckbox:
                ReadyMessage msg;
                if (((CheckBox) b).isChecked()) {
                    setReady(true,0);
                    msg = new ReadyMessage(Message.BROADCAST, true);
                }
                else {
                    setReady(false,0);
                    msg = new ReadyMessage(Message.BROADCAST, false);
                }
                mBC.sendMessage(msg);
                break;

            // DEBUG - Send test messages
            case R.id.test_msg_btn1:
                Message msg0 = new TestMessage(1);
                mBC.sendMessage(msg0);
                break;
            case R.id.test_msg_btn3:
                Message msg1 = new TestMessage(3);
                mBC.sendMessage(msg1);
                break;
        }
    }

    /**
     *
     * BluetoothComm callbacks
     *
     */

    /**
     * If not discoverable anymore may enable button to make discoverable again
     */
    public void onNotDiscoverable()
    {
        CheckBox cbD = (CheckBox) findViewById(R.id.discoverable_ckbox);
        cbD.setChecked(false);
    }

    public void onDeviceFound(String name, String address) {Log.d(LOGTAG, "Unused callback called");}

    /**
     * Scan is done. Adjust button and progress bar.
     */
    public void onScanDone() {Log.d(LOGTAG, "Unused callback called");}

    /**
     * Start connecting. -> Display progress bar
     */
    public void onStartConnecting() {
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
    public void onPlayerConnected(final int pos, final String name)
    {
        Log.d(LOGTAG,"Player " + name + " connected at pos " + pos);
        // Let progressbar disappear
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar);
                bar.setVisibility(View.GONE);
            }
        });

        mGame.getPlayer(pos).setConnected(true);
        mGame.getPlayer(pos).setName(name);
        ImageButton b = null;
        TextView nameField = null;
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

        // Change button color
        if (b!= null) {
            final ImageButton button = b;
            final TextView nameF = nameField;
            final int position = pos;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(LOGTAG, "Changing background of button...");
                        button.setImageResource(R.drawable.occupied_selector);
                        button.setSelected(false);
                        nameF.setText(name);
                        mGame.getPlayer(position).setName(name);
                    }
                });
            } catch (NullPointerException e) {e.printStackTrace();}
        }

        // Check if we are connected to everyone - if so send ack to inviter
        List<Player> players = mGame.getAllPlayers();
        int connectedPlayers = 1; // We consider ourselves as connected
        for (Player p : players) {
            if (p.isConnected()) connectedPlayers++;
        }
        if (connectedPlayers == mGame.getNrPlayer()) {
            Log.d(LOGTAG, "We are connected to all other players.");

            // Could do stuff here

            // Send ACK that all are connected to inviter
            ACKSetupMessage ack = new ACKSetupMessage(mInviter, ACKSetupMessage.ALL_CONNECTED);
            mBC.sendMessage(ack);
            mBC.listen(true);
        }
    }

    public void onPlayerDisconnected(int pos)
    {
        final int position = pos;
        mGame.getPlayer(pos).setConnected(false);
        mGame.getPlayer(pos).setName(null);

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
                AlertDialog alertDialog = new AlertDialog.Builder(SetupActivityFrozen.this).create();
                alertDialog.setTitle(R.string.connection_lost_title);
                String errorMsg = getString(R.string.connection_lost_message1) + " player "
                        + Integer.toString(position) + getString(R.string.connection_lost_message3);
                alertDialog.setMessage(errorMsg);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (position == mInviter) {
                                    Log.d(LOGTAG, "Inviter has quit, go back to MainActivity too");
                                    mBC.stop();
                                    Intent i0 = new Intent(SetupActivityFrozen.this, MainActivity.class);
                                    startActivity(i0);
                                    dialog.dismiss();
                                }
                            }
                        });
                alertDialog.show();
                try {
                    b.setImageResource(R.drawable.vacant_selector);
                    nameF.setText("");
                    mGame.getPlayer(position).setName(null);
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
        if (readyPlayers == 3) { // TODO: Change for more players
            mGame.startWithPuck = false; // Only the leader starts with a puck
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
        // TODO: Is this alright? Should we handle something in here?

        if (ack.getAckCode() == ACKSetupMessage.ALL_CONNECTED) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CheckBox cb = (CheckBox) findViewById(R.id.ready_ckbox);
                    cb.setEnabled(true);
                }
            });
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
                AlertDialog alertDialog = new AlertDialog.Builder(SetupActivityFrozen.this).create();
                alertDialog.setTitle("DEBUG");
                alertDialog.setMessage("Got a message !! Receiver at pos: "
                        + Integer.toString(finalMsg.getSender()) + " Message type: " +
                        finalMsg.getType());
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });
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
