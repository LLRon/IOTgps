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
	private ArrayList<Point> totalPoints = new ArrayList<Point>();// ���еĲ��Ե㼯��
	Map<String, Integer> minLevel = new HashMap<String, Integer>();// ��⵽���е�AP���level����Сֵ����

	private Point tempPoint;
	double minDistance;//���һ��Point��ǰ������С����ֵ
	//int mini;//��С����ֵ����±�

	private Button start;
	private Button calculate;
	private Button scan;
	private Button set;
	
	private EditText X, Y, timesField, intervalField, fileNameEditText;


	private int count = 0;// ɨ�����
	//private int APcount;// �����ĸ���;

	public String fileName;
	PrintStream ps;

	String info = "";
	
	// settings
	private int interval = 50;
	private int times = 20;
	private String path = "wifi.txt";

	private Timer timer;
	private Runnable done;	// ����timer��ɺ�ص����Զ��庯������
	
	private void scan() {
		wifiManager.startScan();
		List<ScanResult> wifiList = wifiManager.getScanResults();
		
		// �����ǰ��ɨ�赽��AP��������һ��ɨ��Ľ��
		// ����ǰ���AP�滻Ϊ�µ�ɨ����
		
		System.out.println("wifiList " + wifiList.size());
		System.out.println("minLevel " + minLevel.size());
		
		if (tempPoint.aps.size() < wifiList.size()) {
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
		}
	}
	
	private Handler handler = new Handler() {
		/**
		 * �˷���(1)����ÿ��ɨ��Ľ����AP������һ�Σ������˵���Ϣ���뵽totalPoints�У�
		 * (2)ͬʱ����AP���level����Сֵ����,(3)�����Ľ����Ϣд���ļ���
		 */
		public void handleMessage(Message msg) {
			if (msg.what == 0x123) {
				// not finish
				scan();
			} else if (msg.what == 0x124) {
				// finish
				info += "\n";
				writeToFile(fileName, info);
				info = "";
				Toast.makeText(MainActivity.this, "ɨ����ɣ�", 4000).show();
			}
		}
	};


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		wifiText = (TextView) findViewById(R.id.wifiText);
		start = (Button) findViewById(R.id.add);
		calculate = (Button) findViewById(R.id.cal);
		scan = (Button) findViewById(R.id.scan);
		set = (Button) findViewById(R.id.set);
		X = (EditText) findViewById(R.id.x);
		Y = (EditText) findViewById(R.id.y);
		timesField = (EditText) findViewById(R.id.times);
		intervalField = (EditText) findViewById(R.id.duration);
		fileNameEditText = (EditText) findViewById(R.id.file);
		
		// display the default value
		timesField.setText(String.valueOf(times));
		intervalField.setText(String.valueOf(interval));
		fileNameEditText.setText(String.valueOf(path));
		

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		timer = new Timer();
		
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
				scan();
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
						+ Y.getText().toString() + " ������"
						+ times.getText().toString() + " �����"
						+ interval.getText().toString() + "ms\n";

				timer.schedule(new TimerTask() {//��ʱ��

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

							Toast.makeText(MainActivity.this, "ɨ����ɣ�", 9000).show();
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

				Point nearestPoint = calculate(); //������
				//��ӡ���
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
	 * ��ȡ��ǰ�ĵ�ĸ���APǿ��level
	 * @return
	 */
	public void getNowPoint(final int timesValue,int interval) {
		tempPoint = new Point();
		fileName = fileNameEditText.getText().toString();	
		
		tempPoint.aps.clear();
		//�������ȡ��͵�ǰ����ȡ��
		if(!X.getText().toString().equals("") && !Y.getText().toString().equals("")){
		tempPoint.x = -1;
		tempPoint.y = -1;
		tempPoint.x = Integer.valueOf(X.getText().toString());
		tempPoint.y = Integer.valueOf(Y.getText().toString());		

		info += (new Date().toLocaleString());
		info += " X= " + X.getText().toString() + " Y= "
				+ Y.getText().toString() + " ������"
				+ timesField.getText().toString() + " �����"
				+ interval + "ms\n";
		}
		wifiText.setText("\nStarting Scan...\n");
		
//		// ���timer�������������
//		timer.purge();
//		// ���²���timer����
//		timer.schedule(new TimerTask() {//��ʱ��
//
//			@Override
//			public void run() {
//				if (count < timesValue) {
//					count ++;
//					handler.sendEmptyMessage(0x123);	
//				} else {
//					// ȡ����ǰ��timerTask
//					// Java doc: If the task has been scheduled for repeated execution, it will never run again. 
//					this.cancel();
//					count = 0;
//
//					handler.sendEmptyMessage(0x124);
//				}
//			}
//		}, 0, interval);
		
		// �л�Ϊ���̵߳�whileѭ������
		while (count ++ < timesValue) {
			scan();
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// finish
		count = 0;
		info += "\n";
		writeToFile(fileName, info);
		info = "";
		Toast.makeText(MainActivity.this, "ɨ����ɣ�", 4000).show();
		
	}
	
	/**
	 * ������룬�����ҳ���С����ĵ��ֵ��
	 * @return
	 */
	private Point calculate() {
		minDistance = Double.MAX_VALUE;
		int mini = -1;
		double tempDistance;
		
		double[] distance = new double[totalPoints.size()];
		
		// TODO: ����ط��� getNowPoint������һ��Timer����timer���֮ǰ�Ѿ�������
		// ��˴˴����ص�mytempPoint��apӦ���ǿյģ�����ļ���ͻ����
		getNowPoint(times, interval);
		
		System.out.println(tempPoint.aps.size());
		
		for (int i = 0; i < totalPoints.size(); i++) {
			tempDistance = calculate_Distance(tempPoint, totalPoints.get(i));
			
			//System.out.println("No."+i+"tempDistance:"+tempDistance);
			wifiText.append("No."+i+" tempDistance:"+tempDistance + "\n");
			
			distance[i] = tempDistance;
			if (tempDistance <= minDistance) {
				minDistance = tempDistance;
				mini = i;
			}
		}
		wifiText.append("minDistance:"+minDistance);
		wifiText.append("mini:"+mini);
		System.out.println("minDistance:"+minDistance);
		System.out.println("mini:"+mini);
		return totalPoints.get(mini);

	}

	/**
	 * ��������֮��ľ���
	 * @param point1 
	 * @param point2
	 * @return
	 */
	private double calculate_Distance(Point point1, Point point2) {
		float result = 0.0f;
		String str;
		Map<String, Integer> tempMap1 = new HashMap<String, Integer>();
		Map<String, Integer> tempMap2 = new HashMap<String, Integer>();

		int i, j;

		for (j = 0; j < point2.aps.size(); j++) {
			tempMap2.put(point2.aps.get(j).BSSID, point2.aps.get(j).level);
		}

		for (i = 0; i < point1.aps.size(); i++) {
			tempMap1.put(point1.aps.get(i).BSSID, point1.aps.get(i).level);
		}

		Iterator<String> iterator = minLevel.keySet().iterator();
		while (iterator.hasNext()) {
			str = iterator.next();
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
	 * ��Wififɨ�����Ϣд���ļ�
	 * @param fileName
	 * @param content
	 */
	private void writeToFile(String fileName, String content) {

		/*
		 * File targetFile = new File("/download/" + fileName); if
		 * (!targetFile.exists()) { // �ļ������ڡ� Just����
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

