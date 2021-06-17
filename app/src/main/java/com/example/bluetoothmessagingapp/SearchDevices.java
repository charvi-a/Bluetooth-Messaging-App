package com.example.bluetoothmessagingapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Set;

public class SearchDevices extends AppCompatActivity {
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ArrayAdapter bonded_devices_list;
    private ListView bonded_devices;
    private ListView available_devices;
    private ArrayAdapter available_devices_list;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String ac = intent.getAction();
            //if we have found a device then we add it to the adapter
            if (ac.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice dev = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //check if the device found is not a bonded device, if it is not then add it to available devices
                if (BluetoothDevice.BOND_BONDED != dev.getBondState()) {
                    available_devices_list.add(dev.getName() + "/n");
                }
            }
            //if the process of searching for devices is done
            else if (ac.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                if (available_devices_list.getCount() == 0){
                    available_devices_list.add("No devices found.");
                }
                else{
                    Toast.makeText(context, "Click the device to start messaging", Toast.LENGTH_SHORT);
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_devices);
        context = getApplicationContext();

        bonded_devices_list = new ArrayAdapter(context,R.layout.device_list);
        available_devices_list = new ArrayAdapter(context, R.layout.device_list);

        bonded_devices = findViewById(R.id.list_bonded_devices);
        available_devices = findViewById(R.id.list_available_devices);

        bonded_devices.setAdapter(bonded_devices_list);
        available_devices.setAdapter(available_devices_list);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

        //check if there are bonded devices
        if (devices.size() > 0){
            for (BluetoothDevice dev : devices){
                bonded_devices_list.add(dev.getName() + "/n");
            }

        }
        else{
            bonded_devices_list.add("No paired devices.");
        }

        //IntentFilter is used to match against actions in an intent
        IntentFilter intent1 = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver,intent1);
        IntentFilter intent2 = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,intent2);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_devices_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.scan_devices_menu){
            search_devices();
            return true;
        }else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void search_devices(){
        //we need to first clear the adapter with old devices so that we can see the
        //the latest devices which are in range of the device

        available_devices_list.clear();

        Toast.makeText(context, "Scanning for devices...", Toast.LENGTH_SHORT);
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

    }


}