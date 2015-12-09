package mtn.lgdx.uavclient;

import android.bluetooth.BluetoothDevice;

public class DeviceList {
	private BluetoothDevice remoteDevice;
	private String remoteDeviceName;
	
	
	public DeviceList(BluetoothDevice btDevice,String deviceName) {
		this.remoteDevice = btDevice;
		this.remoteDeviceName = deviceName;
	}


	public BluetoothDevice getRemoteDevice() {
		return remoteDevice;
	}


	public void setRemoteDevice(BluetoothDevice remoteDevice) {
		this.remoteDevice = remoteDevice;
	}


	public String getRemoteDeviceName() {
		return remoteDeviceName;
	}


	public void setRemoteDeviceName(String remoteDeviceName) {
		this.remoteDeviceName = remoteDeviceName;
	}
	
	@Override
	public String toString() {
		return remoteDevice.getName()+'\n'+remoteDevice.getAddress();
	}
	

}
