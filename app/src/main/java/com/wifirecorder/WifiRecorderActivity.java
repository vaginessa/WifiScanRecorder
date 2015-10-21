package com.wifirecorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class WifiRecorderActivity extends Activity
{
	private static final String PREF_DELAY = "delay";
	private static final String PREF_SCANS = "scans";
	private static final String WIFI_TIMER = "wifi_timer";
	public static final long WIFI_SCAN_DELAY = 5000;
	private static final String TAG = "WifiMapper";
	private static final int SCANS_DEFAULT = 5;
	private static final int DELAY_DEFAULT = 5;

	private Button btnStop;
	private Button btnStart;//记录wifi资讯
	private Button btnExit;
	private TextView wifiData;//界面显示wifi数据
	private TextView scanProgress;//界面显示wifi数据
	private TextView viewScanNum;
	private TextView viewDelayNum;
	private EditText editScanNum;
	private EditText eidtDelayNum;
	private WifiManager wifiManager;//管理并控制wifi
	private File wifiRecFile;//存放wifi数据的文件名
	private Timer wifiTimer;
	private WifiBroadcastReceiver wifiScanReciver;
	private ConnectivityManager connectivityManager;
	private int scanCount=0;//扫描次数
	private EditText edtFileName;//wifi保存文件名
	private boolean isScan=false;//是否开始采集，record按下时变为true

	private int scans = SCANS_DEFAULT;
	private int delay = DELAY_DEFAULT;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "On create");
		setContentView(R.layout.main);
		//取得界面资源
		btnStop = (Button)findViewById(R.id.btnStop);
		btnStart = (Button)findViewById(R.id.btnStart);
		btnExit = (Button)findViewById(R.id.btnExit);
		wifiData = (TextView)findViewById(R.id.wifiData);
		scanProgress = (TextView)findViewById(R.id.ScanProgress);
		viewScanNum = (TextView)findViewById(R.id.viewScanNum);
		viewDelayNum = (TextView)findViewById(R.id.viewDelayNum);
		editScanNum = (EditText)findViewById(R.id.editScanNum);
		eidtDelayNum = (EditText)findViewById(R.id.editDelayNum);
		//设定wifi装置
		wifiManager = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);//取得WifiManager
		wifiTimer = new Timer(WIFI_TIMER); //Timer
		wifiScanReciver = new WifiBroadcastReceiver(); //
		connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		//启用wifi装置
		doStartWifi();
		//取得wifi列表
		//GetWifiList();
		//设定按钮功能
		btnStart.setOnClickListener(btnListener);
		btnStop.setOnClickListener(btnListener);
		btnExit.setOnClickListener(btnListener);

		btnStart.setEnabled(true);
		btnStop.setEnabled(false);
	}

	private Button.OnClickListener btnListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId())
			{
				case R.id.btnStop:
					//取得wifi列表
					doStopScan();
					break;
				case R.id.btnStart:
					RecordCheckWindow();
					break;
				case R.id.btnExit:
					doStopWifi();
					finish();
					break;
			}
		}
	};

	private void RecordCheckWindow()
	{
		Log.d(TAG, "Starting record");
				try {
					doStartScan();
				} catch (Exception e) {
					e.printStackTrace();
				}
	}

	public void CreatFile() throws Exception
	{
		Log.d(TAG, "Create File");
		String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
		wifiRecFile = new File(getDirectory(),String.format("%s.txt",dateTime));
		//String firstLine = String.format("#%d|%d|%s\n", scans, delay, dateTime);
		//doWriteToFile(wifiRecFile, firstLine);
	}

	public void AddtoFile(List<ScanResult> scanResults) throws Exception {
		Log.d(TAG, "Start addData");
		StringBuilder stringBuilder = new StringBuilder();
		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("$%s\n", timestamp));//记录扫描wifi序号，时间
		for (ScanResult scanResult : scanResults) {
			stringBuilder.append(String.format("%s %s %s %s\n", scanResult.SSID, scanResult.BSSID, scanResult.level,
					scanResult.frequency));//记录wifi信息
		}
		doWriteToFile(wifiRecFile, stringBuilder.toString());
		wifiData.setText(String.format("%s", stringBuilder));
	}

	public String getDirectory() {
		return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "wifiRec");
	}//获取SD卡目录，在该目录下新建一个wifimapper的子目录

	//写入文件//可以直接追加在文末
	private void doWriteToFile(File file, String string) throws IOException {
		FileWriter fstream = new FileWriter(file, true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(string);
		out.close();
	}
	//打开wifi装置
	private void doStartWifi()
	{
		Log.d(TAG, "Starting wifi");
		if (wifiManager.isWifiEnabled()) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
			Log.d(TAG, "Register wifi reciever");
			registerReceiver(wifiScanReciver, intentFilter);
			wifiManager.startScan();
		} else {
			Log.d(TAG, "Wifi is not enabled");
		}
	}
	//关闭wifi装置
	private void doStopWifi()
	{
		Log.d(TAG, "Stopping wifi");
		if (wifiManager.isWifiEnabled()) {
			Log.d(TAG, "Unregister wifi reciever");
			unregisterReceiver(wifiScanReciver);
		}
	}

	private void doStartScan() throws Exception//开始扫描
	{
		Log.d(TAG, "Start scan");
		isScan=true;
		scanCount=0;
		btnStart.setEnabled(false);
		btnStop.setEnabled(true);
		delay = Integer.parseInt(eidtDelayNum.getText().toString());
		scans = Integer.parseInt(editScanNum.getText().toString());
        scanProgress.setText("ScanCount: " + scanCount + "/" + scans);
		CreatFile();
	}

	private void doStopScan()//扫描停止
	{
		Log.d(TAG, "Stop scan");
		if(isScan)
		{
			isScan=false;
			btnStart.setEnabled(true);
			btnStop.setEnabled(false);
		}
	}

	public void doNotify(String message) {
		doNotify(message, false);
	}

	public void doNotify(String message, boolean longMessage) {
		(Toast.makeText(this, message, longMessage ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)).show();
		Log.d(TAG, "Notify: " + message);
	}

	private void GetWifiList(List<ScanResult> scanResults) //获取wifi扫描结果并保存
    {
        if(isScan) {
            try {
                scanCount++;
                scanProgress.setText("ScanCount: " + scanCount + "/" + scans);
                AddtoFile(scanResults);
                Log.d(TAG, "Handled wifi scan: #" + scans + ", count: " + this.scanCount + " scans: " + scanResults.size());
				if (scanCount>=scans)
				{
					doStopScan();
				}
			}catch (Exception e){
				Log.e(TAG, e.getMessage(), e);
				doNotify("Error while adding scan to file");
				doStopScan();
			}

		}

	}

	public boolean isWifiConnected() {
		NetworkInfo connectivityNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return connectivityNetworkInfo.isConnected();
	}

	class WifiBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
				GetWifiList(wifiManager.getScanResults());
			if (isWifiConnected()) {
				wifiTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						wifiManager.startScan();
					}
				}, delay * 1000);
			}
		}

	}

}
