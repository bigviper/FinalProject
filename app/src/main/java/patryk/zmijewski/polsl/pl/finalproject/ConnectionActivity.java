package patryk.zmijewski.polsl.pl.finalproject;


import com.neurosky.connection.*;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.*;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.Set;

public class ConnectionActivity extends AppCompatActivity {

    private static final String TAG = ConnectionActivity.class.getSimpleName();
    private TgStreamReader tgStreamReader;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_connection);

        initView();

        try {
            // TODO
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Toast.makeText(
                        this,
                        "Please enable your Bluetooth and re-run this program !",
                        Toast.LENGTH_LONG).show();
                finish();
//				return;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
            return;
        }

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        if(tgStreamReader != null){
            tgStreamReader.close();
            tgStreamReader = null;
        }
        super.onDestroy();
    }

    private void initView(){
        Button deviceButton = (Button) findViewById(R.id.btn_device);

        deviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDevice();
            }
        });

        Button navigationButton = (Button) findViewById(R.id.btn_navigate);

        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConnectionActivity.this,RecordActivity.class);
                intent.putExtra("connectedBluetoothDevice",mBluetoothDevice);
                startActivity(intent);

            }
        });

    }


    //show device list while scanning
    private ListView list_select;
    private BTDeviceListAdapter deviceListApapter = null;
    private Dialog selectDialog;

    public void scanDevice(){

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        setUpDeviceListView();
        //register the receiver for scanning
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        mBluetoothAdapter.startDiscovery();
    }

    private void setUpDeviceListView(){

        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.device_selection, null);
        list_select = (ListView) view.findViewById(R.id.list_select);
        selectDialog = new Dialog(this, R.style.dialog1);
        selectDialog.setContentView(view);
        //List device dialog

        deviceListApapter = new BTDeviceListAdapter(this);
        list_select.setAdapter(deviceListApapter);
        list_select.setOnItemClickListener(selectDeviceItemClickListener);

        selectDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){

            @Override
            public void onCancel(DialogInterface arg0) {
                // TODO Auto-generated method stub
                Log.e(TAG,"onCancel called!");
                ConnectionActivity.this.unregisterReceiver(mReceiver);
            }

        });

        selectDialog.show();

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for(BluetoothDevice device: pairedDevices){
            deviceListApapter.addDevice(device);
        }
        deviceListApapter.notifyDataSetChanged();
    }


    private String deviceName;
    //Select device operation
    private OnItemClickListener selectDeviceItemClickListener = new OnItemClickListener(){

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1,int arg2, long arg3) {
            // TODO Auto-generated method stub
            Log.d(TAG, "Rico ####  list_select onItemClick     ");
            if(mBluetoothAdapter.isDiscovering()){
                mBluetoothAdapter.cancelDiscovery();
            }
            //unregister receiver
            ConnectionActivity.this.unregisterReceiver(mReceiver);

            mBluetoothDevice =deviceListApapter.getDevice(arg2);
            selectDialog.dismiss();
            selectDialog = null;

            Log.d(TAG,"onItemClick name: "+mBluetoothDevice.getName() + " , address: " + mBluetoothDevice.getAddress() );
            address = mBluetoothDevice.getAddress().toString();

            //ger remote device
            BluetoothDevice remoteDevice = mBluetoothAdapter.getRemoteDevice(mBluetoothDevice.getAddress().toString());
            deviceName = remoteDevice.getName();

            if(deviceName!=null && !deviceName.isEmpty()) {
                Toast.makeText(ConnectionActivity.this, "Connected to device "+deviceName, Toast.LENGTH_LONG).show();
                Button navigationButton = (Button) findViewById(R.id.btn_navigate);
                navigationButton.setVisibility(View.VISIBLE);
            }

            //bind and connect
            //bindToDevice(remoteDevice); // create bond works unstable on Samsung S5
            //showToast("pairing ...",Toast.LENGTH_SHORT);

            //tgStreamReader = createStreamReader(remoteDevice);
            //tgStreamReader.connectAndStart();

        }

    };

    /**
     * Check whether the given device is bonded, if not, bond it
     * @param bd
     */
    public void bindToDevice(BluetoothDevice bd){
        int ispaired = 0;
        if(bd.getBondState() != BluetoothDevice.BOND_BONDED){
            //ispaired = remoteDevice.createBond();
            try {
                //Set pin
                if(Utils.autoBond(bd.getClass(), bd, "0000")){
                    ispaired += 1;
                }
                //bind to device
                if(Utils.createBond(bd.getClass(), bd)){
                    ispaired += 2;
                }
                Method createCancelMethod=BluetoothDevice.class.getMethod("cancelBondProcess");
                boolean bool=(Boolean)createCancelMethod.invoke(bd);
                Log.d(TAG,"bool="+bool);

            } catch (Exception e) {
                // TODO Auto-generated catch block
                Log.d(TAG, " paire device Exception:    " + e.toString());
            }
        }
        Log.d(TAG, " ispaired:    " + ispaired);

    }



    //The BroadcastReceiver that listens for discovered devices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mReceiver()");
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG,"mReceiver found device: " + device.getName());

                // update to UI
                deviceListApapter.addDevice(device);
                deviceListApapter.notifyDataSetChanged();

            }
        }
    };
}
