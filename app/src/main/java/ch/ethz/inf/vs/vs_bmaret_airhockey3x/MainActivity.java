package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothCommListener;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.MessageFactory;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.message.Message;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.game.Player;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothCommListener {

    private final static String LOGTAG = "MainActivity";

    private BluetoothComm mBC;
    //private MessageFactory mMF = new MessageFactory();


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b = (Button) findViewById(R.id.play_btn);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.settings_btn);
        b.setOnClickListener(this);

        CheckBox cb = (CheckBox) findViewById(R.id.join_check_box);
        cb.setOnClickListener(this);

        mBC = BluetoothComm.getInstance();
        mBC.init(this, getApplicationContext()); // Must only be done once in entire app
        //mBC = new BluetoothComm(this, getApplicationContext());
        mBC.listen(true); // Start listening for incoming connections
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Make buttons appear next to each other when in landscape -> Important if we start off with
        // Landscape
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.button_layout);
        // Checks the orientation of the screen
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            //  portrait
            linearLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mBC.unregisterListener(this);
    }

    @Override
    public void onConfigurationChanged(Configuration conf)
    {
        super.onConfigurationChanged(conf);

        // Make buttons appear next to each other when in landscape
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.button_layout);
        // Checks the orientation of the screen
        if (conf.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // landscape
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        } else if (conf.orientation == Configuration.ORIENTATION_PORTRAIT){
            //  portrait
            linearLayout.setOrientation(LinearLayout.VERTICAL);
        }
    }

    public void onClick(View b)
    {
        switch (b.getId()) {
            case R.id.play_btn:
                Intent i0 = new Intent(this, SetupActivity.class);
                i0.putExtra("active",true); // Store key somewhere intelligent
                startActivity(i0);
                break;
            case R.id.settings_btn:
                Intent i1 = new Intent(this, SettingsActivity.class);
                startActivity(i1);
                break;
            case R.id.join_check_box:
                // We need to make ourselves discoverable if we are not paired yet
                if (((CheckBox) b).isChecked()) {
                    mBC.discoverable();
                } else {
                    // TODO: Cancel discoverability
                    // TODO: This might be impossible
                }
        }
    }

    public void onDeviceFound(String name,String address) {Log.d(LOGTAG,"Unused callback called");} // Callback not needed
    public void onStartConnecting() {Log.d(LOGTAG,"Unused callback called");}
    public void onPlayerConnected(int pos)
    {
        // TODO: Show dialog which asks user first if he want to participate
        Intent i0 = new Intent(this, SetupActivity.class);
        i0.putExtra("active",false); // Store key somewhere intelligent
        i0.putExtra("pos_inviter",pos);
        startActivity(i0);
    }

    public void onReceiveMessage(final Message msg) {}
}
