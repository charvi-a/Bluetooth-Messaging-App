package com.example.bluetoothmessagingapp;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

//This class will help us to get the state of the message

public class Conversation extends AppCompatActivity{
    private Context context;
    private Handler handler;
    private BluetoothAdapter bluetoothAdapter;

    private Connect connection_thread;
    private Accept accept_thread;
    private SendReceive sendReceive;

    public static final int NONE = 1;
    public static final int CONNECTING = 2;
    public static final int LISTENING = 3;
    public static final int CONNECTED = 4;

    private final UUID UUID_MSG = UUID.fromString("47ec7ee1-b3e7-4de5-8fc2-9005179fb97c");
    private final String NAME_APP = "MessagingApp";
    private int state;

    public Conversation(Context context, Handler handler ){
        this.context = context;
        this.handler = handler;
        state = NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public synchronized void setState(int state){
        this.state = state;
        handler.obtainMessage(MainActivity.STATE_MESSAGE,state,-1).sendToTarget();
    }
    public int getState(){
        return state;
    }

    public synchronized void start(){
        if(accept_thread == null){
            accept_thread = new Accept();
            accept_thread.start();
        }
        if(connection_thread != null){
            connection_thread.cancel();
            connection_thread = null;
        }
        if (sendReceive != null){
            sendReceive.cancel();
            sendReceive = null;
        }
        setState(LISTENING);
    }

    public synchronized void end(){
        if(accept_thread != null){
            accept_thread.cancel();
            accept_thread = null;
        }
        if(connection_thread != null){
            connection_thread.cancel();
            connection_thread = null;
        }
        if(sendReceive != null){
            sendReceive.cancel();
            sendReceive = null;
        }
        setState(NONE);
    }

    public void write(byte[] msg_buffer){
        SendReceive connectThread1;
        synchronized (this){
            if(state != CONNECTED){
                return;
            }
            connectThread1 = sendReceive;
        }
        connectThread1.write(msg_buffer);
    }
    
    //server side
    //we need to accept the incoming connection requests
    private class Accept extends Thread{
        private final BluetoothServerSocket serverSoc;

        public Accept(){
            BluetoothServerSocket temp = null;
            try{
                //listenUsingRfcommWithServiceRecord() creates a listening secure RFCOMM Bluetooth socket with Service Record
                //communication on this socket will be encrypted
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_APP,UUID_MSG);
            } catch (IOException e) {
                Log.e("AcceptThread", "Unable to get a server socket: " + e.toString());
            }
            serverSoc = temp;
        }

        public void run(){
            BluetoothSocket socket = null;

            try{
                socket = serverSoc.accept();
            }catch (IOException e){
                Log.e("AcceptThread","Unable to accept connection request: " + e.toString());
                try{
                    serverSoc.close();
                }catch (IOException el){
                    Log.e("AcceptThread","Unable to close server socket: "+ el.toString());
                }
            }
            if(socket != null){
                if (state == LISTENING || state == CONNECTING ){
                    check_connect(socket,socket.getRemoteDevice());
                }
                else if (state == NONE || state == CONNECTED){
                    try{
                        socket.close();
                    }catch (IOException e){
                        Log.e("AcceptThread","Unable to close server socket: "+ e.toString());
                    }
                }
            }

        }

        public void cancel(){
            try{
                serverSoc.close();
            }catch (IOException e){
                Log.e("AcceptThread","Unable to close server socket: "+ e.toString());
            }
        }
    }

    //one device will open the server socket and the other would be the client and communicate using
    //RFCOMM(BluetoothSocket) sockets
    //By using a UUID we can listen to the incoming Bluetooth connections and accept them.
    //client side
    private class Connect extends Thread{
        private BluetoothSocket soc;
        private final BluetoothDevice dev;

        public Connect(BluetoothDevice dev) {
            this.dev = dev;
            BluetoothSocket temp = null;
            try {
                //createRfcommSocketToServiceRecord() will create a RFCOMM Bluetooth socket to start an outgoing
                //connection to device using UUID.
                temp = dev.createRfcommSocketToServiceRecord(UUID_MSG);
            } catch (IOException e) {
                Log.e("ThreadConnect", "Unable to create RFCOMM socket: " + e.toString());
            }
            soc = temp;
        }

        public void run(){
            //now once we have created a socket, we need to connect() in order to create a connection
            //with the BluetoothDevice via the RFCOMM socket.
            //cancel discovery so that it does not slow down the connection
            bluetoothAdapter.cancelDiscovery();
            try{
                //initiate outgoing connection request from client
                soc.connect();
            } catch (IOException e) {
                Log.e("ThreadConnect", "Unable to connect to device: " + e.toString());
                //then we close the connection
                try {
                    //change soc.mPort to 1 since when it is -1 is does not work for android >= 4.2
                    soc = (BluetoothSocket) dev.getClass().getMethod("createRfcommSocket", new Class[]{int.class}).invoke(dev, 1);
                    soc.connect();
                } catch (Exception e2) {
                    Log.e("ThreadConnect", "Couldn't establish Bluetooth connection!");
                }
            }
            //resetting the connection_thread after completion
            synchronized (Conversation.this){
                connection_thread =null;
            }
            check_connect(soc,dev);
        }

        public void cancel(){
            try{
                soc.close();
            }catch (IOException e){
                Log.e("ThreadConnect","Unable to close connection: "+ e.toString());
            }
        }
    }

    private class SendReceive extends Thread{
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket){
            this.socket = socket;
            InputStream temp_in = null;
            OutputStream temp_out = null;
            try{
                temp_in = socket.getInputStream();
                temp_out = socket.getOutputStream();
            }catch (IOException e){
                Log.e("SendReceive","Unable to send and receive messages: "+ e.toString());
            }
            inputStream = temp_in;
            outputStream = temp_out;
        }

        public void run(){
            byte[] msg_buffer = new byte[1024];
            int bytes_read;
            try{
                bytes_read = inputStream.read(msg_buffer);
                handler.obtainMessage(MainActivity.READ_MESSAGE,bytes_read,-1,msg_buffer).sendToTarget();
            }catch (IOException e){
                failed_connection();
            }
        }

        public void write(byte[] msg_buffer){
            try{
                outputStream.write(msg_buffer);
                handler.obtainMessage(MainActivity.WRITE_MESSAGE,-1,-1,msg_buffer).sendToTarget();
            }catch (IOException e){
                Log.e("SendReceive","Unable to write message to output stream: "+ e.toString());
            }
        }

        public void cancel(){
            try{
                socket.close();
            }catch (IOException e){
                Log.e("SendReceive","Unable to close socket: "+ e.toString());
            }
        }
    }


    public synchronized void connect(BluetoothDevice device){
        if(state == CONNECTING){
            connection_thread.cancel();
            connection_thread = null;
        }
        connection_thread = new Connect(device);
        connection_thread.start();

        if(sendReceive != null){
            sendReceive.cancel();
            sendReceive = null;
        }
        setState(CONNECTING);
    }



    public synchronized void check_connect(BluetoothSocket socket, BluetoothDevice device) {
        if (connection_thread != null){
            connection_thread.cancel();
            connection_thread = null;
        }

        if(sendReceive != null){
            sendReceive.cancel();
            sendReceive = null;
        }
        sendReceive = new SendReceive(socket);
        sendReceive.start();

        Message msg = handler.obtainMessage(MainActivity.DEVICE_MESSAGE);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE,device.getName());
        msg.setData(bundle);
        handler.sendMessage(msg);
        setState(CONNECTED);
    }

    private synchronized void failed_connection(){
        Message msg = handler.obtainMessage(MainActivity.TOAST_MESSAGE);
        //bundle can be used to pass data between activities. (similar to Intent)
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST,"Unable to connect to device.");
        msg.setData(bundle);
        handler.sendMessage(msg);
        setState(NONE);
        //start listening again
        Conversation.this.start();
    }
}
