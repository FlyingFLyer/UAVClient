package mtn.lgdx.uavclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class ConnectServer extends Thread {
	
	private Socket mSocket;
	private String ip;
	private int port;
	private Handler mHandler = null;
	private static final String TAG = "ConnectServer";
	
	public ConnectServer(String ip,int port,Handler handler) {
		this.ip = ip;
		this.port = port;
		this.mHandler = handler;

	}
	
	@Override
	public void run() {
		
		try {
			mSocket = new Socket(ip, port);
			sendMessagetoHandler(1);
			int i = 0;
			while ((i=mSocket.getInputStream().read()) != -1) {
				sendMessagetoHandler(2, i);	
			}
		} catch (UnknownHostException e) {
			sendMessagetoHandler(3);
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	  public void write(String str) {
	        try {
	        	if (mSocket!=null) {
					mSocket.getOutputStream().write(str.getBytes());
					mSocket.getOutputStream().flush();
				}
	        } catch (IOException e) {
	        	Log.e(TAG, "write error");
	        }
	    }
	  
	  public void cancel() {
		  if (mSocket!=null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	  
	  private void sendMessagetoHandler(int code, String s){
		 if (mHandler != null) {
			 Message msg = new Message();
			 msg.what = code;
			 Bundle dataBundle = new Bundle();
			 dataBundle.putString("MSG",s);
			 msg.setData(dataBundle);
			 mHandler.sendMessage(msg);
		}
		return; 
	  }
	  
	  private void sendMessagetoHandler(int what){
		  if (mHandler != null) {
			  mHandler.sendEmptyMessage(what);
		  }
		  return; 	
	  }
	  
	  private void sendMessagetoHandler(int what , int oneByte){
		  if (mHandler != null) {
			Message msg = new Message();
			msg.what = what;
			Bundle dataBundle = new Bundle();
			dataBundle.putInt("REMOTE_CMD", oneByte);
			msg.setData(dataBundle);
			mHandler.sendMessage(msg);
		  }
		  return; 
	  }


}
