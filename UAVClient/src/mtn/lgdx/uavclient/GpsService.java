package mtn.lgdx.uavclient;

import org.apache.http.Header;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

public class GpsService extends Service {

	private LocationManager locationManager;
	private Location location = null;
	private final Criteria criteria = new Criteria();
	private static int minUpdateTime = 1000; 		// 最小更新时间 ms
	private static int minUpdateDistance = 1; 		// 最小更新距离 m
	private double latitude = 0.0; 					// 纬度
	private double longitude = 0.0; 				// 经度
	private double speed = 0.0; 					// 速度
	private double altitude = 0.0; 					// 高度
	private double bearing = 0.0; 					// 方向
	private final String URL = "http://uavuav.oicp.net/gps/get_gps.php"; // 服务器上接收GPS信息的PHP脚本

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		//获得对Location Manager的引用
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		//指定Location Provider的条件
		criteria.setAccuracy(Criteria.ACCURACY_FINE); //高精度
		criteria.setPowerRequirement(Criteria.POWER_LOW); //低耗电量
		criteria.setAltitudeRequired(true); // 需要海拔高度
		criteria.setBearingRequired(true); // 需要方向
		criteria.setSpeedRequired(true); // 需要速度
		criteria.setCostAllowed(true); // 允许服务提供商收取费用
		criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH); //水平方向(经纬度)要求高精度
		criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH); // 垂直方向(海拔)要求高精度
		criteria.setBearingAccuracy(Criteria.ACCURACY_HIGH); //方向要求高精度
		criteria.setSpeedAccuracy(Criteria.ACCURACY_HIGH); // 速度要求高精度

		String provider = locationManager.getBestProvider(criteria, true);
		location = locationManager.getLastKnownLocation(provider);
		if (provider != null) {
			locationManager.requestLocationUpdates(provider, minUpdateTime,
					minUpdateDistance, locationListener);
		}
	}

	private LocationListener locationListener = new LocationListener() {

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			sendGPS(locationManager.getLastKnownLocation(provider));
		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			sendGPS(location);
		}
	};

	private void sendGPS(Location location) {

		if (location != null) {
			latitude 	= location.getLatitude();
			longitude 	= location.getLongitude();
			altitude 	= location.getAltitude();
			speed 		= location.getSpeed();
			bearing 	= location.getBearing();

			AsyncHttpClient client = new AsyncHttpClient();
			RequestParams params = new RequestParams();
			params.put("longitude", "" + longitude);
			params.put("latitude", "" + latitude);
			params.put("altitude", "" + altitude);
			params.put("speed", "" + speed);
			params.put("bearing", "" + bearing);

			client.get(URL, params, new TextHttpResponseHandler() {
				@Override
				public void onSuccess(int arg0, Header[] arg1, String arg2) {
				}

				@Override
				public void onFailure(int arg0, Header[] arg1, String arg2,
						Throwable arg3) {
				}
			});
		}
	}

}
