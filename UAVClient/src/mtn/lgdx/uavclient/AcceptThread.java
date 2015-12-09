package mtn.lgdx.uavclient;

import java.io.IOException;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class AcceptThread extends Thread {
	
	
	 private final BluetoothServerSocket mmServerSocket;
	 
	 private final String TAG = "acceptThread";
	 
	   public AcceptThread(BluetoothAdapter bluetooth,String serverName,UUID uuid) {
		
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = bluetooth.listenUsingRfcommWithServiceRecord(serverName, uuid);
	        } catch (IOException e) {
	        	//Log.e(TAG, "acceptThread() error");
	        }
	        mmServerSocket = tmp;
	    }
	 
	    public void run() {
	    	
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
	            	//Log.e(TAG, "accept() error");
	                break;
	            }
	            // If a connection was accepted,close the thread
	            if (socket != null) {
	                // Do work to manage the connection (in a separate thread)
	               // manageConnectedSocket(socket);
	                try {
						mmServerSocket.close();
					} catch (IOException e) {
						//Log.e(TAG, "close() error");
						e.printStackTrace();
					}
	                break;
	            }
	        }
	    }
	 
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { 
	        	//Log.e(TAG, "cancel() error");
	        }
	    }


}
