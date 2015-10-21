package com.wifirecorder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
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
	private Button btnRefresh;
	private Button btnRecord;//记录wifi资讯
	private Button btnExit;
	private TextView txtTime;
	private Calendar time;
	private ListView listWifiResult;//显示扫描到的wifi信息
	private List<ScanResult> WifiList;//扫描到的wifi信息
	private WifiManager mWifiMngr;//管理并控制wifi
	private String[] WifiInfo;//存放wifi详细信息
	private String curTime;
	private Vector<String> WifiSelectedItem = new Vector<String>();//Vector类似于数组，长度是可变的，可以添加、删除、插入等
	private File wifiRecFile;//存放wifi数据的文件名
	private StringBuilder stringBuilder;//每次采集的wifi数据


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		//取得界面资源
		btnRefresh = (Button)findViewById(R.id.btnRefresh);
		btnRecord = (Button)findViewById(R.id.btnRecord);
		btnExit = (Button)findViewById(R.id.btnExit);
		txtTime = (TextView)findViewById(R.id.txtTime);
		listWifiResult = (ListView)findViewById(R.id.listResult);
		//设定wifi装置
		mWifiMngr = (WifiManager)this.getSystemService(Context.WIFI_SERVICE);//取得WifiManager
		//启用wifi装置
		OpenWifi();
		//取得wifi列表
		GetWifiList();
		//设定按钮功能
		btnRefresh.setOnClickListener(btnListener);
		btnRecord.setOnClickListener(btnListener);
		btnExit.setOnClickListener(btnListener);
		//设定listview选取事件
		listWifiResult.setOnItemClickListener(listListener);//短按
		listWifiResult.setOnItemLongClickListener(listLongListener);//长按
	}

	private Button.OnClickListener btnListener = new Button.OnClickListener()
	{
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId())
			{
				case R.id.btnRefresh:
					//取得wifi列表
					GetWifiList();
					break;
				case R.id.btnRecord:
					RecordCheckWindow();
					break;
				case R.id.btnExit:
					CloseWifi();
					finish();
					break;
			}
		}
	};

	private ListView.OnItemClickListener listListener = new ListView.OnItemClickListener()
	{
		int ItemSelectedInVector;
		@Override
		public void onItemClick(AdapterView<?> parent, View v, int position,
				long id) {

			//如果被勾选就加入 Vector
			if(listWifiResult.isItemChecked(position))
				WifiSelectedItem.add(WifiInfo[position]);
			//如果被取消勾选就从 Vector 移除
			else
			{
				//取得目前选取项目在Vector中的位置
				for(int i=0;i<WifiSelectedItem.size();i++)
					if(WifiSelectedItem.get(i).equals(WifiInfo[position]))
						ItemSelectedInVector = i;
				WifiSelectedItem.remove(ItemSelectedInVector);
			}
		}

	};
	private ListView.OnItemLongClickListener listLongListener = new ListView.OnItemLongClickListener()
	{

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v,
				int position, long id) {
			// TODO Auto-generated method stub
			WifiInfo(position);
			return false;
		}
	};
	private void RecordCheckWindow()
	{
		final EditText edtFileName = new EditText(WifiRecorderActivity.this);
		new AlertDialog.Builder(WifiRecorderActivity.this)
		.setTitle("确认视窗")
		.setIcon(R.drawable.ic_launcher)
		.setMessage("请输入预存档案名称:")
		.setView(edtFileName)
		.setNegativeButton("取消", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}

		})
		.setPositiveButton("确定",new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				//将选取的list记录并生成档案
				DataFormer(edtFileName.getText().toString());
			}
		}).show();
	}
	private void WifiInfo(int index)
	{
		new AlertDialog.Builder(WifiRecorderActivity.this)
		.setTitle("详细资料")
		.setIcon(R.drawable.ic_launcher)
		.setMessage(WifiInfo[index])
		.setNeutralButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub

			}

		})
		.show();
	}
	private void DataFormer(String FileName)
	{
		String WifiDatas = curTime+"\r\n";
		File directory = new File(getDirectory());
		//File wifiRecDirectory = new File(getWifiRecDirectory());
		//建立档案在SDcard里
		if(!directory.exists())//如果SD卡没此资料就建立
			directory.mkdir();
		try {
			//String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd_hhmm", new java.util.Date()).toString();
			wifiRecFile = new File(getDirectory(),String.format("%s.txt",FileName));
			doWriteToFile(wifiRecFile, WifiDatas);
			doWriteToFile(wifiRecFile,stringBuilder.toString());
			Toast.makeText(WifiRecorderActivity.this
							,FileName+".txt 已存至手机",Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(WifiRecorderActivity.this
							,FileName+"存档失败!",Toast.LENGTH_LONG).show();
		}
	}

	public String getDirectory() {
		return String.format("%s/%s", Environment.getExternalStorageDirectory().toString(), "wifiRec");
	}//获取SD卡目录，在该目录下新建一个wifimapper的子目录

	public String getWifiRecDirectory() {
		return String.format("%s/%s", getDirectory(), "mapper");
	}//

	//写入文件
	private void doWriteToFile(File file, String string) throws IOException {
		FileWriter fstream = new FileWriter(file, true);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write(string);
		out.close();
	}
	//打开wifi装置
	private void OpenWifi()
	{
		//当wifi关闭时将它启动
		if(!mWifiMngr.isWifiEnabled()){
			mWifiMngr.setWifiEnabled(true);
			Toast.makeText(WifiRecorderActivity.this,"WiFi启动中……请稍后"
						   ,Toast.LENGTH_LONG).show();
			Toast.makeText(WifiRecorderActivity.this,"请按Refresh键更新列表"
					,Toast.LENGTH_LONG).show();
		}
	}
	//关闭wifi装置
	private void CloseWifi()
	{
		//开始扫描wifi热点
		if(mWifiMngr.isWifiEnabled())
			mWifiMngr.setWifiEnabled(false);
	}
	private void GetWifiList()
	{
		//开始扫描wifi热点
		mWifiMngr.startScan();
		//得到扫描结果
		WifiList = mWifiMngr.getScanResults();
		//设定wifi阵列
		stringBuilder = new StringBuilder();
		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("$%s\n", timestamp));//记录扫描wifi序号，时间
		for (ScanResult scanResult : WifiList) {
			stringBuilder.append(String.format("%s %s %s %s\n", scanResult.SSID, scanResult.BSSID, scanResult.level,
					scanResult.frequency));//记录wifi信息
		}

		String[] Wifis = new String[WifiList.size()];
		//取得系统当前日期，年月日时分秒
		time = Calendar.getInstance();
		curTime = (time.get(Calendar.YEAR))+"/"
				+(time.get(Calendar.MONTH)+1)+"/"
				+(time.get(Calendar.DAY_OF_MONTH))+"  "
				+time.get(Calendar.HOUR_OF_DAY)+":"
				+time.get(Calendar.MINUTE)+":"
				+time.get(Calendar.SECOND);
		txtTime.setText("Time:"+curTime);
		//将wifi信息放入阵列中（多选清单用）这里是显示在屏幕上的list上的
		for(int i=0;i<WifiList.size();i++)
			Wifis[i] = "SSID:"+WifiList.get(i).SSID +"\n" //SSID
						+"讯号强度:"+WifiList.get(i).level+"dBm";//讯号强弱
		//将WifiSelectedItem中暂存的资料清空
		WifiSelectedItem.removeAllElements();
		//设定wifi清单
		SetWifiList(Wifis);
	}

	//记录正式信息
	public void addScanToFile(List<ScanResult> scanResults) throws Exception
	{
		StringBuilder stringBuilder = new StringBuilder();

		long timestamp = System.currentTimeMillis() / 1000;
		stringBuilder.append(String.format("$%s\n", timestamp));//记录扫描wifi序号，时间
		for (ScanResult scanResult : scanResults) {
			stringBuilder.append(String.format("%s %s %s %s\n", scanResult.SSID, scanResult.BSSID, scanResult.level,
					scanResult.frequency));//记录wifi信息
		}
		doWriteToFile(wifiRecFile, stringBuilder.toString());
	}
	private void SetWifiList(String[] Wifis)
	{
		//建立ArrayAdpter
		 ArrayAdapter<String> adapterWifis = new ArrayAdapter<String>(WifiRecorderActivity.this
						,android.R.layout.simple_list_item_checked,Wifis);
		//设定ListView为多选
		listWifiResult.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		//设定ListView来源
		listWifiResult.setAdapter(adapterWifis);

		//初始化WifiInfo阵列
		WifiInfo = null;
		//将wifi信息放入阵列中（记录存档中）
		WifiInfo = new String[WifiList.size()];

		for(int i=0;i<WifiList.size();i++)
			WifiInfo[i] = "SSID:"+WifiList.get(i).SSID +"\r\n"      //SSID
						+"BSSID:"+WifiList.get(i).BSSID+"\r\n"   //BSSID
						+"讯号强度:"+WifiList.get(i).level+"dBm"+"\r\n" //讯号强弱
						+"通道频率:"+WifiList.get(i).frequency+"MHz"+"\r\n"; //通道频率
	}

}
