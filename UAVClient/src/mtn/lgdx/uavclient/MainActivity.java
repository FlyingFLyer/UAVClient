package mtn.lgdx.uavclient;

import java.util.UUID;

import javax.security.auth.PrivateCredentialPermission;

import mtn.lgdx.bluetooth.R;
import android.R.bool;
import android.R.integer;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity implements OnItemClickListener, OnSeekBarChangeListener, OnLongClickListener, OnClickListener {
	
	private BluetoothAdapter bluetoothAdapter;
	
	private EditText ipAddress;			//IP��ַ�����
	private Button btnConnectServer;	//���ӷ�����
	
	private Button btnFindDevice;	//�����豸��ť
	private TextView tvinfo;		//��ʾ��ʾ��Ϣ
	private ListView listView;		//�ҵ����豸�б�
	
	private ImageButton btnStartButton;		//������ť
	
	private SeekBar skb_accelerator;		//����
	private SeekBar skb_FB;					//ǰ������
	private SeekBar skb_RL;					//��ת��ת
	private SeekBar skb_rotate;				//��ת
	
    protected static final String TAG = "BLUETOOTH";
    protected static final int DISCOVERY_REQUEST = 1;
    
    private static final String IP_DNS = "uavuav.oicp.net";		//����������
	private static final int PORT = 1111;						//������Ĭ�϶˿ں�
    
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");	//�Դ��ڽ��в�����UUID
    private final String serverName  = "bluetoothserver";
    
    private  Handler handler = null;
    
    private String info = "";		//������״̬��ʾ��Ϣ
    
    private ConnectThread mConnectThread = null;
    private ConnectServer connectServer = null;
    
    private ArrayAdapter<DeviceList> deviceList;
    
    private static final int ACCELERATE_MAX 		= 1523;			//���ŵ����ֵ
    private static final int RIGHT_LEFT_MAX 		= 1523;			//���ҵ����ֵ
    private static final int ROTATE_MAX 			= 1523;			//��ת�����ֵ
    private static final int FORWARD_BACK_MAX	= 1523;			//ǰ������ֵ
    
    private static final int ACCELERATE_MIN 		= 523;			//���ŵ���Сֵ
    private static final int RIGHT_LEFT_MIN 		= 523;			//���ҵ���Сֵ
    private static final int ROTATE_MIN 			= 523;			//��ת����Сֵ
    private static final int FORWARD_BACK_MIN	= 523;			//ǰ�����Сֵ
    
    private static final int SEEKBAR_MAX = 1000;					//���������ֵ
    
    private String ipaddr = "";		//���������IP��ַ������
    
    SharedPreferences preferences;
    SharedPreferences.Editor preferences_editor;
    
    private Intent gpsServiceIntent;	//��������GPS����
	
    @SuppressLint("HandlerLeak") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        
        ipAddress = (EditText) findViewById(R.id.ipAddress);
        btnConnectServer = (Button) findViewById(R.id.btnConnectServer);
        btnConnectServer.setOnClickListener(this);
        
        btnFindDevice = (Button) findViewById(R.id.btnFindDevice);
        btnFindDevice.setOnClickListener(this);
      
        tvinfo = (TextView) findViewById(R.id.tvInfo);
        listView = (ListView) findViewById(R.id.DeviceList);
        
        btnStartButton = (ImageButton) findViewById(R.id.btnStart);
        btnStartButton.setOnLongClickListener(this);
        btnStartButton.setOnClickListener(this);
  
        skb_accelerator = (SeekBar) findViewById(R.id.skb1);
        skb_FB 			= (SeekBar) findViewById(R.id.skb4);
        skb_RL			= (SeekBar) findViewById(R.id.skb2);
        skb_rotate		= (SeekBar) findViewById(R.id.skb3);
        
        skb_accelerator.setMax(SEEKBAR_MAX);
        skb_accelerator.setProgress(SEEKBAR_MAX/2);
        skb_RL.setMax(SEEKBAR_MAX);
        skb_RL.setProgress(SEEKBAR_MAX/2);
        skb_rotate.setMax(SEEKBAR_MAX);
        skb_rotate.setProgress(SEEKBAR_MAX/2);
        skb_FB.setMax(SEEKBAR_MAX);
        skb_FB.setProgress(SEEKBAR_MAX/2);
        
        skb_accelerator.setOnSeekBarChangeListener(this);
        skb_FB.setOnSeekBarChangeListener(this);
        skb_RL.setOnSeekBarChangeListener(this);
        skb_rotate.setOnSeekBarChangeListener(this);
        
        setEnable(false);		//���û�������������豸������ü��������ؼ�
        
        deviceList = new ArrayAdapter<DeviceList>(this, android.R.layout.simple_list_item_1);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //�������δ�����������
        if (!bluetoothAdapter.isEnabled()) {
            if (bluetoothAdapter.enable()) {
    			tvinfo.setText("�����ѿ���");
    		}else {
    			tvinfo.setText("����δ����,���ȿ�������");
    		}
            
		}else {
			tvinfo.setText("�����ѿ���");
		}
        
        listView.setAdapter(deviceList);
        listView.setOnItemClickListener(this);
        
        handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                super.handleMessage(msg);
                
                int remote_cmd = 0;
                
                switch (msg.what) {
                case 1:
					Toast.makeText(MainActivity.this, "�ɹ����ӵ�������", Toast.LENGTH_SHORT).show();
					break;
				case 2:
					//��ȡ�ӷ����������������ݣ�Ȼ���͸������豸
					remote_cmd = msg.getData().getInt("REMOTE_CMD");
					SendToBluetooth(mConnectThread, remote_cmd);
					break;
				case 3:
					Toast.makeText(MainActivity.this, "�޷����ӵ�������", Toast.LENGTH_SHORT).show();
					break;
				case 4:
	                   Toast.makeText(MainActivity.this, "�����豸���ӳɹ�", Toast.LENGTH_SHORT).show();
	                   tvinfo.setText(String.format("��%s������", info));
	                   deviceList.clear();
	                   //setEnable(true);
					break;
				default:
					break;
				}
            }
        };
        
        preferences = getSharedPreferences("ipaddr", Context.MODE_PRIVATE);
        preferences_editor = preferences.edit();
        if (preferences != null) {
			ipaddr = preferences.getString("ipaddr", null);
			ipAddress.setText(ipaddr);
		}
        
        gpsServiceIntent = new Intent(this,GpsService.class);
		startService(gpsServiceIntent);		//����GPS����
    }
    
    @Override
    protected void onDestroy() {
    	if (mConnectThread!=null) {
    		mConnectThread.cancel();		//ֹͣ�����߳�
		}
    	if (bluetoothAdapter!=null) {
    		bluetoothAdapter.disable();		//�ر�����
		}
    	stopService(gpsServiceIntent);		//ֹͣGPS����
    	
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
    
    /**
     * �豸�б�ĵ�����Ӧ����������б�������￪ʼ���豸��������
    */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		DeviceList remoteDeviceList = deviceList.getItem(position);
		info = remoteDeviceList.getRemoteDeviceName();
		
		bluetoothAdapter.cancelDiscovery(); //ֹͣ�����豸
		AcceptThread aThread = new AcceptThread(bluetoothAdapter, serverName, MY_UUID);		
		aThread.start();		//�����������߳�
		ConnectThread cThread = new ConnectThread(remoteDeviceList.getRemoteDevice(), MY_UUID,handler);		
		cThread.start();		//�����ͻ����߳�
		mConnectThread = cThread;
		//ע��һ��Broadcast Receiver������BluetoothDevice.ACTION_ACL_DISCONNECTED,����Զ���豸��������ʧ��
		registerReceiver(discoveryResult,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
		tvinfo.setText(String.format("������%s����������...", info));
	} 
    
	/////////////////////////////////////////////////////////////  
    /**
     * 	Discovering remote Bluetooth Devices
     */
    private void startDiscovery() {
    	 
    	registerReceiver(discoveryResult,
                       new IntentFilter(BluetoothDevice.ACTION_FOUND));
//    	registerReceiver(discoveryResult,
//                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));	//
    	if (bluetoothAdapter.isEnabled()) {
    		
			if (!bluetoothAdapter.isDiscovering()) {
			      bluetoothAdapter.startDiscovery();	//��ʼ�����豸
			      tvinfo.setText("���ڲ����豸...");
			}else {
				tvinfo.setText("�Ѿ��ڲ����豸�����Ժ�...");
				return;
			}
		}
    	else {
    		tvinfo.setText("������δ�������뿪������������...");
    		return;
		}
    }

    /**
     * �㲥����
    */
    private final BroadcastReceiver discoveryResult = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
    	  
    	  String action = intent.getAction();
    	  
    	  //����ҵ�Զ���豸
    	  if (BluetoothDevice.ACTION_FOUND.equals(action)) {
        	  
    	        String remoteDeviceName = 
    	          intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

    	        BluetoothDevice remoteDevice =  
    	          intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    	        
    	      //�ҵ��豸������ӵ��б���ʾ�������豸�����豸��ַ
    	        deviceList.add(new DeviceList(remoteDevice, remoteDeviceName));		
		}else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
			tvinfo.setText(String.format("��%s��������ʧ�ܣ�����������", info));
		}
      }
    };
    
    /**
     * �÷�������enable/disable�⼸���ؼ���ֻ�����������豸�����������Ժ���Щ�ؼ��ſ���
    */
    private void setEnable(boolean enabled){
    	skb_accelerator.setEnabled(enabled);
    	skb_RL.setEnabled(enabled);
    	skb_FB.setEnabled(enabled);
    	skb_rotate.setEnabled(enabled);
    	btnStartButton.setEnabled(enabled);
    }
    
    /**
     * SeekBar.OnSeekBarChangeListener 
     */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		int cmd_data = 0;
		int cmd_type = 0;
		switch (seekBar.getId()) {
		case R.id.skb1:		//����
			cmd_type = UAVCmd.ACCELERATOR;
			cmd_data = (int)(ACCELERATE_MIN+progress);
			break;
		case R.id.skb2:		//����
			cmd_type = UAVCmd.RIGHT_LEFT;
			cmd_data = (int)(RIGHT_LEFT_MIN+progress);
			break;	
		case R.id.skb3:		//��ת
			cmd_type = UAVCmd.ROTATE;
			cmd_data = (int)(ROTATE_MIN+progress);
			break;
		case R.id.skb4:		//ǰ��
			cmd_type = UAVCmd.FORWARD_BACK;
			cmd_data = (int)(FORWARD_BACK_MIN+progress);
			break;
		default:
			break;
		}
		SendToBluetooth(mConnectThread, UAVCmd.FormatCmd(cmd_type, cmd_data));

	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
		int cmd_type = 0;
		seekBar.setProgress(seekBar.getMax()/2);
		switch (seekBar.getId()) {
		case R.id.skb1:		//����
			cmd_type = UAVCmd.ACCELERATOR;
			break;
		case R.id.skb2:		//����
			cmd_type = UAVCmd.RIGHT_LEFT;
			break;
		case R.id.skb3:		//��ת
			cmd_type = UAVCmd.ROTATE;
			break;
		case R.id.skb4:		//ǰ��
			cmd_type = UAVCmd.FORWARD_BACK;
			break;
		default:
			break;
		}
		SendToBluetooth(mConnectThread, UAVCmd.FormatCmd(cmd_type, UAVCmd.MEDIAN));
	}

	/**
	 * ��ť�����¼���������
	 */
	@Override
	public boolean onLongClick(View v) {
		
		switch (v.getId()) {
		case R.id.btnStart:				//�������˻���������
			SendStartCmd();
			//SendToBluetooth(mConnectThread, UAVCmd.START<<12);
			break;
		default:
			break;
		}
		return true;
	}
	
	/**
	 * ��ť����¼���������
	 */
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btnStart:				
			
			break;
		case R.id.btnConnectServer:			//���ӷ�����
			connectServer = new ConnectServer(ipAddress.getText().toString(), PORT, handler);	
			connectServer.start();
			//����IP��ַ
			preferences_editor.putString("ipaddr", ipAddress.getText().toString());
			preferences_editor.commit();
			break;
		case R.id.btnFindDevice:
			startDiscovery();				//��ʼɨ�������豸
			break;
		default:
			break;
		}
	}
	
	
	private void SendToBluetooth(ConnectThread thread,int cmd){
		
		if (thread != null) {
			thread.write(cmd);
		}else {
			Toast.makeText(MainActivity.this, "�����߳�δ����", Toast.LENGTH_SHORT).show();
		}
	}
	
	private void SendStartCmd(){
		if (mConnectThread != null) {
			mConnectThread.write(UAVCmd.START<<12);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(500);
						mConnectThread.write(UAVCmd.START_DELAY<<12);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}else {
			Toast.makeText(MainActivity.this, "�����߳�δ����", Toast.LENGTH_SHORT).show();
			return ;
		}
	}
}






















