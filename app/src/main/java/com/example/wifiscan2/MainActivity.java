package com.example.wifiscan2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.WriteAbortedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.impl.conn.Wire;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.R.xml;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView wifiText;
	private WifiManager wifiManager;
	//private Point point;
	private ArrayList<Point> totalPoints = new ArrayList<Point>();// 所有的测试点集合
	Map<String, Integer> minLevel = new HashMap<String, Integer>();// 检测到所有的AP点的level的最小值集合

	private Point tempPoint;
	double minDistance;//最后一个Point与前面点的最小距离值
	//int mini;//最小距离值点的下标

	private EditText X, Y, timesField, intervalField, fileNameEditText;
	private int count = 0;// 扫描次数

	public String fileName;

	String info = "";
	
	// settings
	private int interval = 50;
	private int times = 20;


	//private Timer timer;
	//private Runnable done;	// 用于timer完成后回调的自定义函数调用

	
	private Map<String,ArrayList<AP>> scan(Map<String,ArrayList<AP>> tempAPs) {
		wifiManager.startScan();
		List<ScanResult> wifiList = wifiManager.getScanResults();

		
		//将所有扫描到的AP点强度取平均值作为AP的level		
		System.out.println("wifiList " + wifiList.size());
		System.out.println("minLevel " + minLevel.size());
		
		tempPoint.aps.clear();
		info += String.valueOf(count) + "\n";
		
		for (int i = 0; i < wifiList.size(); i++) {

			ScanResult ret = wifiList.get(i);			
			String BSSID = ret.BSSID;
			String SSID = ret.SSID;		
			AP ap;
			ArrayList<AP> tempAPsList = new ArrayList<AP>();
			
			ap = new AP();			
			ap.SSID = SSID;
			ap.BSSID = BSSID;
			ap.level = ret.level;			
			
			//搜索原来的tempAPs中是否含有本次扫描到的AP点
			if(!tempAPs.keySet().contains(BSSID)) {

				tempAPsList.add(ap);				
				tempAPs.put(BSSID, tempAPsList);
			} else{
				
				tempAPs.get(BSSID).add(ap);				
			}
			
			//tempPoint.aps.add(ap);
			
			//收集最小AP强度集合				
			if (minLevel.keySet().contains(BSSID)) {
				if (minLevel.get(BSSID) > ret.level)
					minLevel.put(BSSID, ret.level);
			} else {
				minLevel.put(BSSID, ret.level);
			}
			
			info += "BSSID:" + BSSID
					+ "  level:" + ret.level + " "
					+ "frequency" + ret.frequency
					+ "\n";
			// write("BSSID:" + wifiList.get(i).BSSID +
			// "  level:"
			// + wifiList.get(i).level + " ");
		}
		// ps.println();
		info += "\n";
		return tempAPs;
		// write("\n");
/*		if (tempPoint.aps.size() < wifiList.size()) {
			tempPoint.aps.clear();
			info += String.valueOf(count) + "\n";
			
			for (int i = 0; i < wifiList.size(); i++) {

				ScanResult ret = wifiList.get(i);				
				String BSSID = ret.BSSID;
				String SSID = ret.SSID;				
				AP ap = new AP();

				if (minLevel.keySet().contains(BSSID)) {
					if (minLevel.get(BSSID) > ret.level)
						minLevel.put(BSSID, ret.level);
				} else {
					minLevel.put(BSSID, ret.level);
				}
				
				ap.SSID = SSID;
				ap.BSSID = BSSID;
				ap.level = ret.level;
				tempPoint.aps.add(ap);
				
				info += "BSSID:" + BSSID
						+ "  level:" + ret.level + " "
						+ "frequency" + ret.frequency
						+ "\n";
				// write("BSSID:" + wifiList.get(i).BSSID +
				// "  level:"
				// + wifiList.get(i).level + " ");
			}
			// ps.println();
			info += "\n";
			// write("\n");
		} else {
			info += String.valueOf(count) + "\n";
			// write(String.valueOf(count)+"\n");
			for (int i = 0; i < wifiList.size(); i++) {
				ScanResult ret = wifiList.get(i);
				String BSSID = ret.BSSID;

				info += "BSSID:" + BSSID
						+ "  level:" + ret.level + " "
						+ "frequency" + ret.frequency
						+ "\n";
				// write("BSSID:" + wifiList.get(i).BSSID +
				// "  level:"
				// + wifiList.get(i).level + " ");
			}
			// ps.println();
			info += "\n";
			// write("\n");
		}*/
		
	}
	
	private Handler handler = new Handler() {
		/**
		 * 此方法(1)将从每次扫描的结果中AP点最多的一次，并将此点信息加入到totalPoints中，
		 * (2)同时构造AP点的level的最小值集合,(3)将检测的结果信息写入文件中
		 */
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == 0x123) {
				// not finish
				//scan函数改变，参数添加
				scan(new HashMap<String,ArrayList<AP>>());
			} else if (msg.what == 0x124) {
				// finish
				info += "\n";
				writeToFile(fileName, info);
				info = "";
				Toast.makeText(MainActivity.this, "扫描完成！", Toast.LENGTH_LONG).show();

			}
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		wifiText = (TextView) findViewById(R.id.wifiText);
		Button start = (Button) findViewById(R.id.add);
		Button calculate = (Button) findViewById(R.id.cal);
		Button scan = (Button) findViewById(R.id.scan);
		Button set = (Button) findViewById(R.id.set);
		X = (EditText) findViewById(R.id.x);
		Y = (EditText) findViewById(R.id.y);
		timesField = (EditText) findViewById(R.id.times);
		intervalField = (EditText) findViewById(R.id.duration);
		fileNameEditText = (EditText) findViewById(R.id.file);
		
		// display the default value
		timesField.setText(String.valueOf(times));
		intervalField.setText(String.valueOf(interval));
		fileNameEditText.setText(String.valueOf("wifi.txt"));
		

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		//timer = new Timer();
		
		set.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				interval = Integer.valueOf((intervalField.getText().toString()));
				times = Integer.valueOf((timesField.getText().toString()));
			}
			
		});
		
		scan.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tempPoint = new Point();
				//参数改变
				//scan(new HashMap<String,ArrayList<AP>>());
				getNowPoint(10,200);
				String str = "";
				for(int i = 0; i < tempPoint.aps.size(); i ++) {
					str += tempPoint.aps.get(i).SSID + ": " + String.valueOf(tempPoint.aps.get(i).level) + "\n";
				}
				wifiText.setText(str);
			}
		});

		start.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
/*				tempPoint = new Point();

				tempPoint.aps.clear();
				tempPoint.x = -1;
				tempPoint.y = -1;

				tempPoint.x = Integer.valueOf(X.getText().toString());
				tempPoint.y = Integer.valueOf(Y.getText().toString());

				timer = new Timer();
				fileName = fileNamEditText.getText().toString();

				wifiManager.startScan();
				wifiText.setText("\nStarting Scan...\n");

				info += (new Date().toLocaleString());
				info += " X= " + X.getText().toString() + " Y= "
						+ Y.getText().toString() + " 次数："
						+ times.getText().toString() + " 间隔："
						+ interval.getText().toString() + "ms\n";

				timer.schedule(new TimerTask() {//定时器

					@Override
					public void run() {
						if (count < timesValue) {
							count ++;
							handler.sendEmptyMessage(0x123);	
						} else {
							timer.cancel();
							X.getText().clear();
							Y.getText().clear();
							times.getText().clear();
							interval.getText().clear();
							count = 0;
							info += "\n";
							writeToFile(fileName, info);
							info = "";

							Toast.makeText(MainActivity.this, "扫描完成！", 9000).show();
						}
					}
				}, 0, Integer.parseInt(interval.getText().toString()));*/
				getNowPoint(times,interval);
				
				System.out.println(tempPoint.aps.size());
				totalPoints.add(tempPoint);

			}
		});

		calculate.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {

				Point nearestPoint = calculate(); //计算结果
				//打印结果
				StringBuilder sBuilder = new StringBuilder();
				
/*				int i;					
			for (i = 0; i < totalPoints.size() - 1; i++) {
					sBuilder.append("\n");
					sBuilder.append("Point" + (i + 1) + "X= "
							+ totalPoints.get(i).x + " Y= "
							+ totalPoints.get(i).y + "\n");
					ArrayList<AP> aps = totalPoints.get(i).aps;
					for (int j = 0; j < aps.size(); j++) {
						sBuilder.append(aps.get(j).SSID + " "
								+ aps.get(j).level + "\n");
					}
					sBuilder.append("Distance:" + distance[i] + "\n\n");
				}

				sBuilder.append("The measure point: X= " + totalPoints.get(i).x
						+ " Y= " + totalPoints.get(i).y + "\n");
				ArrayList<AP> aps = totalPoints.get(i).aps;
				for (int j = 0; j < aps.size(); j++) {
					sBuilder.append(aps.get(j).SSID + " " + aps.get(j).level
							+ "\n");
				}
*/
				sBuilder.append("\nSo the nearestPoint is :Point: " + nearestPoint.x + ", " + nearestPoint.y 
						+ "\n distance is:" + minDistance);

				wifiText.append(sBuilder.toString());

			}

		});

	}
	
	/**
	 * 获取当前的点的各个AP强度level
	 */
	public void getNowPoint(final int timesValue,int interval) {
		tempPoint = new Point();
		fileName = fileNameEditText.getText().toString();	

		tempPoint.aps.clear();
		//区别采样取点和当前测试取点
		if(!X.getText().toString().equals("") && !Y.getText().toString().equals("")){
		tempPoint.x = -1;
		tempPoint.y = -1;
		tempPoint.x = Integer.valueOf(X.getText().toString());
		tempPoint.y = Integer.valueOf(Y.getText().toString());		

		info += (new Date().toString());
		info += " X= " + X.getText().toString() + " Y= "
				+ Y.getText().toString() + " 次数："
				+ timesField.getText().toString() + " 间隔："
				+ interval + "ms\n";
		}
		wifiText.setText("\nStarting Scan...\n");
		
//		// 清空timer里面的所有任务
//		timer.purge();
//		// 重新布置timer任务
//		timer.schedule(new TimerTask() {//定时器
//
//			@Override
//			public void run() {
//				if (count < timesValue) {
//					count ++;
//					handler.sendEmptyMessage(0x123);	
//				} else {
//					// 取消当前的timerTask
//					// Java doc: If the task has been scheduled for repeated execution, it will never run again. 
//					this.cancel();
//					count = 0;
//
//					handler.sendEmptyMessage(0x124);
//				}
//			}
//		}, 0, interval);
		
		Map<String,ArrayList<AP>> tempAPs = new HashMap<String,ArrayList<AP>>();
		// 切换为非线程的while循环。。
		while (count ++ < timesValue) {
			tempAPs = scan(tempAPs);
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		
		//遍历tempAPs，取各个AP强度的平均值写入tempPoint

		Set<String> keySet = tempAPs.keySet();

		for(String key : keySet) {
			
			AP ap = new AP();			
			ap.BSSID = String.valueOf(key);
			ap.SSID = tempAPs.get(key).get(0).SSID;
			Map<Double,Integer> levelTimes = new HashMap<Double, Integer>();//用于记录同一个BSSID下，不同强度出现次数
	    	double sum = 0;
			int APtimes = tempAPs.get(key).size() ;//同一个AP采集次数
			
/*			//计算平均强度
			for(int i = 0;i < APtimes; i++) {
			sum += tempAPs.get(key).get(i).level;
			}
			
			ap.level = sum / APtimes; //定义平均强度*/

			for(int i = 0 ; i<tempAPs.get(key).size(); i++){

				double tempLevel = tempAPs.get(key).get(i).level;
				    //初始化一个强度出现次数
				if(!levelTimes.keySet().contains(tempLevel)){

					levelTimes.put(tempLevel,1);
				}
				if (levelTimes.keySet().contains(tempLevel)){
					//出现一次，则增加次数
					int j = levelTimes.get(tempLevel) + 1;
					levelTimes.put(tempLevel,j);
				}
			}
			//如果次数超越设定阈值，则用于计算平均值
			for(Double levelkey : levelTimes.keySet()){
				if(levelTimes.get(levelkey) > 2) {
					sum += levelkey * levelTimes.get(levelkey);
					APtimes += levelkey;
				}
			}

			ap.level = sum / APtimes;
			tempPoint.aps.add(ap);
		}
		
		
	//	tempPoint.aps.add(object);
		// finish
		count = 0;
		info += "\n";
		writeToFile(fileName, info);
		info = "";

		Toast.makeText(MainActivity.this, "扫描完成！", Toast.LENGTH_LONG).show();
		
	}
	
	/**
	 * 计算距离，并且找出最小距离的点和值。
	 */
	private Point calculate() {
		minDistance = Double.MAX_VALUE;
		int mini = -1;
		double tempDistance;

		// TODO: 这个地方， getNowPoint调用了一个Timer，在timer完成之前已经返回了
		// 因此此处返回的mytempPoint的ap应该是空的，后面的计算就会出错！
		getNowPoint(times, interval);
		
		System.out.println(tempPoint.aps.size());
		
		for (int i = 0; i < totalPoints.size(); i++) {
			tempDistance = calculate_Distance(tempPoint, totalPoints.get(i));
			
			//System.out.println("No."+i+"tempDistance:"+tempDistance);
			wifiText.append("No."+i+" tempDistance:"+tempDistance + "\n");

			if (tempDistance <= minDistance) {
				minDistance = tempDistance;
				mini = i;
			}
		}
		wifiText.append("minDistance:"+minDistance+"\n");
		wifiText.append("mini:"+mini+"\n");
		System.out.println("minDistance:"+minDistance);
		System.out.println("mini:"+mini);
		return totalPoints.get(mini);

	}

	/**
	 * 计算两点之间的距离
	 * @param point1 First point
	 * @param point2 Second point
	 * @return double
	 */
	private double calculate_Distance(Point point1, Point point2) {
		float result = 0.0f;
		Map<String, Double> tempMap1 = new HashMap<String, Double>();
		Map<String, Double> tempMap2 = new HashMap<String, Double>();

		int i, j;

		for (j = 0; j < point2.aps.size(); j++) {
			tempMap2.put(point2.aps.get(j).BSSID, point2.aps.get(j).level);
		}

		for (i = 0; i < point1.aps.size(); i++) {
			tempMap1.put(point1.aps.get(i).BSSID, point1.aps.get(i).level);
		}

		for (String str : minLevel.keySet()) {
			if (tempMap1.containsKey(str) && tempMap2.containsKey(str)) {
				result += (tempMap1.get(str) - tempMap2.get(str))
						* (tempMap1.get(str) - tempMap2.get(str));
			}

			if (tempMap1.containsKey(str) && !tempMap2.containsKey(str)) {
				result += (tempMap1.get(str) - minLevel.get(str))
						* (tempMap1.get(str) - minLevel.get(str));
			}

			if (!tempMap1.containsKey(str) && tempMap2.containsKey(str)) {
				result += (tempMap2.get(str) - minLevel.get(str))
						* (tempMap2.get(str) - minLevel.get(str));
			}
		}

		return Math.sqrt(result);
	}

	
	/**
	 * 将Wifif扫描的信息写入文件
	 * @param fileName the file name
	 * @param content content to be written
	 */
	private void writeToFile(String fileName, String content) {

		/*
		 * File targetFile = new File("/download/" + fileName); if
		 * (!targetFile.exists()) { // 文件不存在、 Just创建
		 * 
		 * targetFile.createNewFile(); } OutputStreamWriter osw = null; osw =
		 * new OutputStreamWriter(new FileOutputStream("/download/" + fileName,
		 * true)); osw.write(content); System.out.println(content); osw.close();
		 */

		try {

			File file = new File("/mnt/sdcard", fileName);
			if (!file.exists()) {
				file.createNewFile();
			}
			OutputStream out = new FileOutputStream(file, true);
			out.write(content.getBytes());
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Refresh");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		wifiManager.startScan();
		wifiText.setText("Starting Scan");
		return super.onMenuItemSelected(featureId, item);
	}

}

