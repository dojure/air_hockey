package ch.ethz.inf.vs.vs_bmaret_airhockey3x.android;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import ch.ethz.inf.vs.vs_bmaret_airhockey3x.android.communication.BluetoothComm;


/**
 * The most important reason for this Activity up until now is to store the players user name to
 * SharedPreferences. Further settings?
 *
 * TODO: Everything
 */


public class SettingsActivity extends AppCompatActivity {

    final static String LOGTAG = "SettingsActivity";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private BluetoothComm mBC;

    private String DEFAULT_NAME;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

//        Button btn_save = (Button)findViewById(R.id.btn_save);
//        btn_save.setOnClickListener(this);

        EditText inputName = (EditText) findViewById(R.id.input_device_name);
        TextView.OnEditorActionListener okListener = new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Log.d(LOGTAG, "saved");
                    String deviceName = ((EditText) v).getText().toString();
                    editor.putString("DEVICE_NAME", deviceName);
                    editor.apply();
                    mBC.changeDeviceName(deviceName);

                    Toast toast = Toast.makeText(getApplicationContext(), "SAVED", Toast.LENGTH_SHORT);
                    toast.show();
                }
                return false;
            }
        };

        inputName.setOnEditorActionListener(okListener);

        mBC = BluetoothComm.getInstance();
        preferences = getSharedPreferences("Settings", MODE_PRIVATE);
        editor = preferences.edit();

        DEFAULT_NAME = mBC.getDeviceName();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();


        String device_name = preferences.getString("DEVICE_NAME", DEFAULT_NAME);
        ((EditText) findViewById(R.id.input_device_name)).setText(device_name);
    }
}
