package mtn.lgdx.uavclient;

import java.util.UUID;

import mtn.lgdx.bluetooth.R;

import org.apache.http.Header;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;


public class MainActivity extends Activity implements OnItemClickListener, OnClickListener {
	
	private BluetoothAdapter bluetoothAdapter;
	private Button btnFindDevice;	//查找设备按钮
	private TextView tvinfo;		//显示提示信息
	private ListView listView;		//找到的设备列表
	private ProgressDialog progDialog = null; //
	
	private static final String URL = "http://uavuav.oicp.net/ip.php";
	private static final String TAG = "UAVClient";
	private static final String serverName  = "bluetoothserver";
	private static final int DISCOVERY_REQUEST = 1;
	private static final int PORT = 1111;				//服务器程序默认端口号
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");	//对串口操作的UUID
    
    private ConnectThread mConnectThread = null;
    private ConnectServer connectServer = null;
    
    private ArrayAdapter<DeviceList> deviceList;
    
    private String  deviceName = "";		//蓝牙设备名字
    private String ipaddr = "";		//保存服务器IP地址
    
    private Intent gpsServiceIntent;	//用来启动GPS服务
	
    @SuppressLint("HandlerLeak") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        btnFindDevice = (Button) findViewById(R.id.btnFindDevice);
        btnFindDevice.setOnClickListener(this);
        tvinfo = (TextView) findViewById(R.id.tvInfo);
        listView = (ListView) findViewById(R.id.DeviceList);
        
        deviceList = new ArrayAdapter<DeviceList>(this, android.R.layout.simple_list_item_1);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //如果蓝牙未开启则打开蓝牙
        if (!bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.enable()) {
    			tvinfo.setText("蓝牙已开启");
    		}else {
    			tvinfo.setText("蓝牙未开启,请先开启蓝牙");
    		}
            
		}else {
			tvinfo.setText("蓝牙已开启");
		}
        
        listView.setAdapter(deviceList);
        listView.setOnItemClickListener(this);
        
        gpsServiceIntent = new Intent(this,GpsService.class);
		startService(gpsServiceIntent);		//启动GPS服务
		
		GetServerIP();		//向服务器发送http请求获取服务器IP地址，然后根据IP地址连接服务器程序
    }
    
    @Override
    protected void onDestroy() {
    	if (mConnectThread!=null) {
    		mConnectThread.cancel();		//停止蓝牙线程
		}
    	if (bluetoothAdapter!=null) {
    		bluetoothAdapter.disable();		//关闭蓝牙
		}
    	stopService(gpsServiceIntent);		//停止GPS服务
    	
    	super.onDestroy();
    }
    
  

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @SuppressLint("HandlerLeak")
	private  Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            int remote_cmd = 0;
            switch (msg.what) {
            case 1:
				Toast.makeText(MainActivity.this, "成功连接到服务器", Toast.LENGTH_SHORT).show();
				break;
			case 2:
				//提取从服务器发过来的数据，然后发送给蓝牙设备
				remote_cmd = msg.getData().getInt("REMOTE_CMD");
				SendToBluetooth(mConnectThread, remote_cmd);
				break;
			case 3:
				ShowFailedConnectServerDialog();		//如果连接服务器失败，则显示对话框
				break;
			case 4:
                   Toast.makeText(MainActivity.this, "蓝牙设备连接成功", Toast.LENGTH_SHORT).show();
                   tvinfo.setText(String.format("已经与蓝牙设备%s建立好连接", deviceName));
                   deviceList.clear();
				break;
			default:
				break;
			}
        }
    };
    
    /**
     * 设备列表的单击响应函数，点击列表项，开始与该蓝牙设备建立连接
    */
	@SuppressLint("HandlerLeak")
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		DeviceList remoteDeviceList = deviceList.getItem(position);
		deviceName = remoteDeviceList.getRemoteDeviceName();
		
		bluetoothAdapter.cancelDiscovery(); //停止查找设备
		AcceptThread aThread = new AcceptThread(bluetoothAdapter, serverName, MY_UUID);		
		aThread.start();		//启动服务器线程
		ConnectThread cThread = new ConnectThread(remoteDeviceList.getRemoteDevice(), MY_UUID,handler);		
		cThread.start();		//启动客户端线程
		mConnectThread = cThread;
		tvinfo.setText(String.format("正在与%s建立连接...", deviceName));
		//注册一个Broadcast Receiver来监听BluetoothDevice.ACTION_ACL_DISCONNECTED,即与远程设备建立连接失败
		registerReceiver(discoveryResult,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
	} 
    
	/////////////////////////////////////////////////////////////  
    /**
     * 	Discovering remote Bluetooth Devices
     */
    private void startDiscovery() {
    	 
    	registerReceiver(discoveryResult,
                       new IntentFilter(BluetoothDevice.ACTION_FOUND));
    	if (bluetoothAdapter.isEnabled()) {
    		
			if (!bluetoothAdapter.isDiscovering()) {
			      bluetoothAdapter.startDiscovery();	//开始查找设备
			      tvinfo.setText("正在查找设备...");
			}else {
				tvinfo.setText("已经在查找设备，请稍后");
				return;
			}
		}
    	else {
    		tvinfo.setText("蓝牙还未开启，请开启蓝牙后重试");
    		return;
		}
    }

    /**
     * 广播接收
    */
    private final BroadcastReceiver discoveryResult = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
    	  
    	  String action = intent.getAction();
    	  
    	  //如果找到远程设备
    	  if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        	  
    	        String remoteDeviceName = 
    	          intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

    	        BluetoothDevice remoteDevice =  
    	          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    	        
    	      //找到设备后将其添加到列表显示，包括设备名和设备地址
    	        deviceList.add(new DeviceList(remoteDevice, remoteDeviceName));		
		}else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
			tvinfo.setText(String.format("与%s建立连接失败，请重新连接", deviceName));
		}
      }
    };
    
	/**
	 * 
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnFindDevice:
			startDiscovery();				//开始扫描蓝牙设备
			break;
		default:
			break;
		}
	}
	
	private void SendToBluetooth(ConnectThread thread,int cmd){
		if (thread != null) {
			thread.write(cmd);
		}
	}
	
	/**
	 * 显示进度框
	 */
	private void showProgressDialog() {
		if (progDialog == null)
			progDialog = new ProgressDialog(this);
		progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDialog.setIndeterminate(false);
		progDialog.setCancelable(false);
		progDialog.setMessage("正在查询服务器的IP地址");
		progDialog.show();
	}

	/**
	 * 隐藏进度框
	 */
	private void dissmissProgressDialog() {
		if (progDialog != null) {
			progDialog.dismiss();
		}
	}

	/**
	 * 向服务器发送http请求，查询服务器的IP地址。发送http请求使用了开源框架android-async-http
	 */
	private void GetServerIP() {
		
		showProgressDialog();	

		AsyncHttpClient client = new AsyncHttpClient();
		client.get(URL, new TextHttpResponseHandler() {		//向服务器发送http请求，查询服务器的IP地址

			@Override
			public void onSuccess(int arg0, Header[] arg1, String response) {
				dissmissProgressDialog();
				ipaddr = response;			//服务器返回的IP地址赋给该变量
				ConnectToServer();			//连接服务器
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, String arg2,
					Throwable arg3) {
				dissmissProgressDialog();
				ShowFailedGetAddrDialog();
			}
		});

	}

	/**
	 * 获取服务器IP地址失败时显示该对话框
	 */
	private void ShowFailedGetAddrDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("无法获取服务器地址！");

		builder.setPositiveButton("重试",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						GetServerIP();		//重新向服务器发送请求获取其IP地址
					}
				});
		builder.setNegativeButton("退出",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();		//退出程序
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * 连接服务器失败时显示该对话框
	 */
	private void ShowFailedConnectServerDialog() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("无法连接到服务器！");

		builder.setPositiveButton("重试",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						ConnectToServer();		//重新尝试连接服务器
					}
				});
		builder.setNegativeButton("退出",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();			//退出程序
					}
				});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	/**
	 * 创建连接服务器线程去连接服务器
	 */
	private void ConnectToServer() {
		connectServer = new ConnectServer(ipaddr, PORT,	handler);
		connectServer.start();
	}
}






















