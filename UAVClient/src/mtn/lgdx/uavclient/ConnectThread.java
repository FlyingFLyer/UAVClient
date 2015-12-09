package mtn.lgdx.uavclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectThread extends Thread {
	private static final int MESSAGE_READ = 0;
	private final BluetoothSocket mmSocket;
  //  private final BluetoothDevice mmDevice;
 //   private BluetoothAdapter mBluetoothAdapter;
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

    private final String TAG = "ConnectThread";
    private Handler handler;
   
 
    public ConnectThread(BluetoothDevice device,UUID myUUID,Handler handler) {
        // Use a temporary object that is later assigned to mmSocket,
        // because mmSocket is final
        BluetoothSocket tmpSocket = null;
        
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        this.handler = handler;
 
        // Get a BluetoothSocket to connect with the given BluetoothDevice
        try {
            // MY_UUID is the app's UUID string, also used by the server code
        	tmpSocket = device.createRfcommSocketToServiceRecord(myUUID);
            tmpIn = tmpSocket.getInputStream();
            tmpOut = tmpSocket.getOutputStream();
        } catch (IOException e) { }
        
        mmSocket = tmpSocket;
        mmInStream = tmpIn;
        mmOutStream = tmpOut;
    }
 
    public void run() {
    	
    	 byte[] buffer = new byte[128];  // buffer store for the stream
	     int bytes; // bytes returned from read()
 
        try {
            // Connect the device through the socket. This will block
            // until it succeeds or throws an exception
            mmSocket.connect();
          // Log.i(TAG, "succeed to connect the client");
           Message message = new Message();
           message.what = 4;
           handler.sendMessage(message);
	        while (true) {
	            try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                        .sendToTarget();
	            } catch (IOException e) {
	            	//Log.e(TAG, "read error");
	                break;
	            }
	        }
        } catch (IOException connectException) {
            // Unable to connect; close the socket and get out
            try {
            	
                mmSocket.close();
            } catch (IOException closeException) {
            	//Log.e(TAG, "close error");
            }
            return;
        }
 
        // Do work to manage the connection (in a separate thread)
      //  manageConnectedSocket(mmSocket);
    }
    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);
        } catch (IOException e) {
        	Log.e(TAG, "write error");
        }
    }
    
    public void write(int oneByte){
    	try {
    		//因为收到服务器的一个字节就发送一个字节给蓝牙，所以这里就直接发送低八位就行了
			mmOutStream.write(oneByte & 0xff);
			//mmOutStream.write((oneByte & 0xffff)>>8);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
 
    /** Will cancel an in-progress connection, and close the socket */
    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        	Log.e(TAG, "cancel error");
        }
    }

}
