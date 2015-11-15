package ch.ethz.inf.vs.vs_bmaret_airhockey3x;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothComm;
import ch.ethz.inf.vs.vs_bmaret_airhockey3x.communication.BluetoothCommListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, BluetoothCommListener {

    private final static String LOGTAG = "MainActivity";

    private BluetoothComm mBC;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button b = (Button) findViewById(R.id.play_btn);
        b.setOnClickListener(this);
        b = (Button) findViewById(R.id.settings_btn);
        b.setOnClickListener(this);

        mBC = new BluetoothComm(this, getApplicationContext());
        mBC.listen(); // Start listening for incoming connections
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Make buttons appear next to each other when in landscape
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
                startActivity(i0);
                break;
            case R.id.settings_btn:
                Intent i1 = new Intent(this, SettingsActivity.class);
                startActivity(i1);
                break;
        }
    }

    public void onDeviceFound(String name) {} // Callback dont needed

}
