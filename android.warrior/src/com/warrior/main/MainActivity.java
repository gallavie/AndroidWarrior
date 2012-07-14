package com.warrior.main;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews.ActionException;
import android.widget.TextView;
import android.widget.Toast;

import com.gal.bluetooth1.R;

public class MainActivity extends Activity implements OnClickListener, OnItemClickListener,IDataReceive {

	private Button butEnable,butClose,butDisable,butScan,butSync,butOpenServer;
	private ListView lv;
	private Bluetooth bt;
	private TextView tv;
	private ArrayAdapter<String> devicesNamesAdapter;
	private List<BluetoothDevice>devicesList;
	private boolean isServer = true;
	private int state = 0;
	private Long sendTime;
	private Long receiveTime = (long) 0;
	private CommHandler commHandler;
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        bt = new Bluetooth(this);        
        devicesNamesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        devicesList = new ArrayList<BluetoothDevice>();
        buildViewReferece();
        buildIntentFilter();
        BuildEvents();
    }
    private void buildViewReferece()
    {
    	  butEnable = (Button)findViewById(R.id.butEnable);
          butClose = (Button)findViewById(R.id.butClose);
          butDisable = (Button)findViewById(R.id.butDisable);
          butScan = (Button)findViewById(R.id.butScan);
          butSync = (Button)findViewById(R.id.butSync);
          butOpenServer = (Button)findViewById(R.id.butOpenServer);
          tv = (TextView)findViewById(R.id.tv);
          lv = (ListView)findViewById(R.id.lv);
          lv.setEmptyView((TextView)findViewById(R.id.tvEmpty));
    }
    private void buildIntentFilter()
    {
    	IntentFilter iDeviceFound = new IntentFilter(BluetoothDevice.ACTION_FOUND);
  		IntentFilter iConnectedBluetooth = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
  		IntentFilter iDisConnectedBluetooth = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
  		IntentFilter iStateBluetoothChanged = new IntentFilter(bt.getInstanceAdapter().ACTION_STATE_CHANGED);
  		IntentFilter iDiscoveryFinished = new IntentFilter(bt.getInstanceAdapter().ACTION_DISCOVERY_FINISHED);
  		registerReceiver(ReceiverBluetooth, iDeviceFound); 
  		registerReceiver(ReceiverBluetooth, iConnectedBluetooth);
  		registerReceiver(ReceiverBluetooth, iDisConnectedBluetooth);
  		registerReceiver(ReceiverBluetooth, iStateBluetoothChanged);
  		registerReceiver(ReceiverBluetooth, iDiscoveryFinished);
    }
    private void BuildEvents()
    {
    	   lv.setOnItemClickListener(this);
           butEnable.setOnClickListener(this);
           butClose.setOnClickListener(this);
           butDisable.setOnClickListener(this);
           butScan.setOnClickListener(this);
           butSync.setOnClickListener(this);
           butOpenServer.setOnClickListener(this);
    }
	public void onClick(View v) {
		if(v.equals(butEnable))
		{
			bt.enableBluetooth();
		}	
		else if(v.equals(butDisable))
		{
			bt.disableBluetooth();
		}
		else if(v.equals(butClose))
		{
			try {
				commHandler.closeConnection();
			} catch (IOException e) {
				Log.d("gal",e.getMessage());
			}
		}
		else if(v.equals(butScan))
		{
			devicesNamesAdapter.clear();
			bt.startScanning();
			Toast.makeText(this,"the bluetooth is scanning",Toast.LENGTH_SHORT).show();
		}
		else if(v.equals(butSync))
		{
			if (isServer)
			{
				sendTime = getTime();
				commHandler.writeToRmoteDevice(sendTime);
				Log.d("gal","time server sends data: " + sendTime);
				state = 1; // sent T0 to client device
			}
			else
			{
				 //shouldn't have gotten here. 
			}
		}
		else if(v.equals(butOpenServer))
		{
			if(!bt.getInstanceServer().getRunning())
			{
    			try {
					bt.getInstanceServer().createListen();
					Toast.makeText(this,"the server is open",Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	protected void onDestroy() {
		this.unregisterReceiver(ReceiverBluetooth);
		super.onDestroy();
	}
	// this is turn on the client side
	public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
		if(bt.getInstanceClient() != null)
		{
			if(!bt.getInstanceClient().getRunning())
			{
				bt.getInstanceClient().connectionToServer(devicesList.get(position));
				isServer = false;
			}
		}
	}
	// Create a BroadcastReceiver
	private final BroadcastReceiver ReceiverBluetooth = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
	        // When discovery finds a device
		    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		    	// Get the BluetoothDevice object from the Intent
		        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		        devicesList.add(device);
		        // Add the name and address to an array adapter to show in a ListView
		        devicesNamesAdapter.add(device.getName() + "\n" + device.getAddress());
		        lv.setAdapter(devicesNamesAdapter);
		    }
		    
		    else if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action))
		    {
		    	BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		    	Toast.makeText(context, "you connected to " + device.getName(), Toast.LENGTH_SHORT).show();
		    	 try {
			    	if(isServer)
			    	{
			    		while(bt.getInstanceServer().getSocket() == null){}
			    		commHandler = new CommHandler(MainActivity.this, bt.getInstanceServer().getSocket());
			    		if(!commHandler.getRunning())
			    		{
			    			commHandler.execute();
			    		}
			    		butSync.setEnabled(true);
			    	}
			    	else
			    	{
			    		commHandler = new CommHandler(MainActivity.this, bt.getInstanceClient().getSocket());
			    		if(!commHandler.getRunning())
			    		{
			    			commHandler.execute();
			    		}
			    	}
		    	 } catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
		    	butClose.setEnabled(true);
			    butOpenServer.setEnabled(false);
			   	butDisable.setEnabled(false);
			   	butScan.setEnabled(false);
			   	devicesList.clear();
		    }
		    else if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action))
		    {
		    	Toast.makeText(context,"the connection is closed",Toast.LENGTH_SHORT).show();
		    	butSync.setEnabled(false);
		    	butDisable.setEnabled(true);
		    }
		    else if(bt.getInstanceAdapter().ACTION_STATE_CHANGED.equals(action))
		    {
		    	if(bt.getInstanceAdapter().isEnabled())
		    	{
		    		butDisable.setEnabled(true);
					butScan.setEnabled(true);
					butEnable.setEnabled(false);
					butOpenServer.setEnabled(true);
		    		Toast.makeText(context,"the bluetooth is enable",Toast.LENGTH_SHORT).show();
		    	}
		    	else
		    	{
		    		devicesList.clear();
					butDisable.setEnabled(false);
					butScan.setEnabled(false);
					butSync.setEnabled(false);
					butClose.setEnabled(false);
					butEnable.setEnabled(true);
					butOpenServer.setEnabled(false);
		    		Toast.makeText(MainActivity.this,"the bluetooth is disable",Toast.LENGTH_SHORT).show();
		    	}
		    }
		    else if(bt.getInstanceAdapter().ACTION_DISCOVERY_FINISHED.equals(action))
			{
		    	Toast.makeText(context, "the discovery is finished",Toast.LENGTH_SHORT).show();
			}
		}
		
	};
	public void dataReceive(Long value) {
		tv.setText(String.valueOf(value));
		if (isServer)
		{
			switch(state)
			{
				case 0:
				{
					// do nothing. Main activity will send initial time stamp
					Log.d("gal","is server" + String.valueOf(state));
					break;
				}
				case 1: 
				{
					Long deltaInClientSide = value;
					Long deltaNsRoundTrip = (receiveTime-sendTime);
					Long airTimeTotal = deltaNsRoundTrip - deltaInClientSide;
					String serverText = "Delta found by server: " + String.valueOf(airTimeTotal) + 
							". Delta roundTrip was " + deltaNsRoundTrip + " delta on Client side was: " + deltaInClientSide;
					Toast.makeText(this, serverText , Toast.LENGTH_SHORT).show();
					state = 2;
					break;

				}
			}
		}
		else // CLIENT SIDE: 
		{
			Log.d("gal", "is client and the state is " + String.valueOf(state));
			switch(state)
			{
				case 0:
				{// received first sync message. receiveTime should contain time T1 (when T0 message was received) 	

					sendTime=getTime();
 					long deltaInsideClient = sendTime - receiveTime;
					
					commHandler.writeToRmoteDevice(deltaInsideClient);
					Long tDelta = sendTime - receiveTime;
					String clientText = "sent in Client the delta: " + tDelta + 
							" and tSend, tReceive were: " + sendTime + " , " + receiveTime;
					Log.d("gal", "sent message to server in time:  " + String.valueOf(sendTime));

					Toast.makeText(this, clientText , Toast.LENGTH_SHORT).show();
					break;

				}
				case 1: 
				{
					Log.d("gal","is client" + String.valueOf(state));
					break;

				}
			}

			
		}
	}
	public enum ClientStates {
		WAIT_FOR_SYNC_START,
		FIRST_SYNC_PKT_RECEIVED;
		}
	public enum ServerStates {
		WAIT_FOR_SYNC_START,
		START_SYNC_PROCESS,
		SECOND_SYNC_PKT_RECEIVED;
		}

	private long convertBytesToLong(byte[] data)
	{
		Long value = (long) 0;
		for (int i = 0; i < data.length; i++)
		{
		   value += ((long) data[i] & 0xffL) << (8 * i);
		}
		return value;
	}
	public void setReceiveTime(long time) {
		// TODO Auto-generated method stub
		this.receiveTime = time;
	}
	public long getReceiveTime() {
		// TODO Auto-generated method stub
		return this.receiveTime;
	}
	public long getTime() {
		return System.currentTimeMillis();// nanoTime();
	}
}