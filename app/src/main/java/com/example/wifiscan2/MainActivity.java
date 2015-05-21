package com.example.wifiscan2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.graphics.PointF;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.content.Context;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements MainFragment.OnFragmentInteractionListener,
        SettingFragment.OnFragmentInteractionListener {

    // save the fragment
    ArrayList<Fragment> mFragments;

    private ViewPager mPager;

    private PagerAdapter mPagerAdapter;

	private WifiManager wifiManager;
	//private Point point;

	private ArrayList<Point> totalPoints; // 所有的测试点集合
	private Map<String, Integer> minLevel; // 检测到所有的AP点的level的最小值集合

    // 计算or扫描用的临时点
	private Point tempPoint;
    // 计算得到的最近点
    private Point nearestPoint;
    // 计算得到的最短差异度
	private double minDistance;//最后一个Point与前面点的最小距离值

    private final int DONE = 0x124;
    private final int ADD = 0x122;
    private final int LOG = 0x123;

	private String info = "";
	
	// settings
	private int interval = 50;
	private int times = 20;
    private String pointFileName = "wifi";
    private String minFileName = "minlevel";
    // using for postDelayed delay time
    private int delay;

    // 展示信息用
    private TextLogger logger;
	
	private Map<String,ArrayList<AP>> scan(Map<String,ArrayList<AP>> tempAPs) {
		wifiManager.startScan();
		List<ScanResult> wifiList = wifiManager.getScanResults();

        //将所有扫描到的AP点强度取平均值作为AP的level
        tempPoint.aps.clear();

        ArrayList<AP> tempAPsList;
		
		for (int i = 0; i < wifiList.size(); i++) {

			ScanResult ret = wifiList.get(i);			
			String BSSID = ret.BSSID;
			String SSID = ret.SSID;

			AP ap = new AP();
			ap.SSID = SSID;
			ap.BSSID = BSSID;
			ap.level = ret.level;

			//搜索原来的tempAPs中是否含有本次扫描到的AP点
			if(!tempAPs.keySet().contains(BSSID)) {
                tempAPsList = new ArrayList<AP>();
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
		}

		return tempAPs;
	}
	
	private Handler handler = new Handler() {
		/**
		 * 此方法(1)将从每次扫描的结果中AP点最多的一次，并将此点信息加入到totalPoints中，
		 * (2)同时构造AP点的level的最小值集合,(3)将检测的结果信息写入文件中
		 */
		@Override
		public void handleMessage(Message msg) {
			if (msg.what == DONE) {
				// finish
				Toast.makeText(MainActivity.this, "扫描完成！", Toast.LENGTH_LONG).show();

			}
            if (msg.what == LOG) {
                logger.log((String)msg.obj);
            }
            if (msg.what == ADD) {
                totalPoints.add(tempPoint);
            }
		}
	};

    @Override
    protected void onResume() {
        super.onResume();

        totalPoints = (ArrayList<Point>)readFromFile(pointFileName);
        minLevel = (HashMap<String, Integer>)readFromFile(minFileName);

        if (totalPoints == null) {
            totalPoints = new ArrayList<Point>();
        }

        if (minLevel == null) {
            minLevel = new HashMap<String, Integer>();
        }
    }

    private void log(String message) {
        Message msg = Message.obtain();
        msg.what = LOG;
        msg.obj = message;
        handler.sendMessage(msg);
    }


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        mFragments = new ArrayList<Fragment>(2);

        Bundle bundle = new Bundle();
        bundle.putString("pointFileName", pointFileName);
        bundle.putInt("interval", interval);
        bundle.putInt("times", times);

        // add in two fragments
        MainFragment mainFrag = new MainFragment();

        // setup the logger
        logger = mainFrag;

        mFragments.add(mainFrag);

        SettingFragment settingFrag = new SettingFragment();
        settingFrag.setArguments(bundle);
        mFragments.add(settingFrag);

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new SettingPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	}

    @Override
    protected void onPause() {
        super.onPause();

        writeToFile(pointFileName, totalPoints);
        writeToFile(minFileName,minLevel);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * 设置参数
     * @param interval 扫描时间间隔
     * @param times 扫描次数
     * @param fileName 保持的文件名字
     */
    @Override
    public void onSet(int interval, int times, String fileName) {
        this.interval = interval;
        this.times = times;
        this.pointFileName = fileName;

        Toast.makeText(this, "修改成功", Toast.LENGTH_LONG).show();
    }

    /**
     * 一次扫描的测试函数
     */
    @Override
    public void onScan() {
        delay = 0;
        // 临时点的x y默认为 -1 -1.
        getNowPoint(-1, -1, times, interval);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String str = "";
                for (String key : tempPoint.aps.keySet()) {
                    str += tempPoint.aps.get(key).SSID + ": " + String.valueOf(tempPoint.aps.get(key).level) + "\n";
                }
                log(str);
            }
        }, delay);
    }

    /**
     * 添加新的点
     * @param x 点的x坐标
     * @param y 点的y坐标
     */
    @Override
    public void onAdd(int x, int y) {
        delay = 0;
        getNowPoint(x, y, times,interval);
        handler.sendEmptyMessage(ADD);
    }

    @Override
    public void onCal() {
        delay = 0;
        getNowPoint(-1, - 1, times, interval);
        calculate(); //计算结果

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                StringBuilder sBuilder = new StringBuilder();

                sBuilder.append("\nSo the nearestPoint is :Point: " + nearestPoint.x + ", " + nearestPoint.y
                        + "\n distance is:" + minDistance + "\n");
                sBuilder.append("*********************************************");
                log(sBuilder.toString());
            }
        }, delay);
    }

    private class SettingPagerAdapter extends FragmentStatePagerAdapter {


        public SettingPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }
    }

	/**
	 * 获取当前的点的各个AP强度level
	 */
	public void getNowPoint(int x, int y, final int timesValue,final int interval) {
		tempPoint = new Point();

		tempPoint.aps.clear();
		//区别采样取点和当前测试取点

		tempPoint.x = x;
		tempPoint.y = y;

		info += (new Date().toString());
		info += " X= " + x + " Y= "
				+ y + " 次数："
				+ timesValue + " 间隔："
				+ interval + "ms\n";

		Toast.makeText(this, "Start scanning", Toast.LENGTH_LONG).show();

        final Map<String,ArrayList<AP>> tempAPs = new HashMap<String,ArrayList<AP>>();

        // 每次扫描点的Runable，用于置入消息队列中。
        final Runnable scan = new Runnable() {
            @Override
            public void run() {
                scan(tempAPs);
            }
        };
        // 扫描点后的加工回调函数
        final Runnable tact = new Runnable() {
            @Override
            public void run() {
                Set<String> keySet = tempAPs.keySet();
                //遍历tempAPs，取各个AP强度的平均值写入tempPoint
                for(String key : keySet) {

                    AP ap = new AP();
                    ap.BSSID = String.valueOf(key);
                    ap.SSID = tempAPs.get(key).get(0).SSID;
                    Map<Double,Integer> levelTimes = new HashMap<Double, Integer>();//用于记录同一个BSSID下，不同强度出现次数
                    double sum = 0;
                    int APtimes = 0;//同一个AP采集次数

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
                        if(levelTimes.get(levelkey) > 1) {
                            sum += levelkey * levelTimes.get(levelkey);
                            APtimes += levelTimes.get(levelkey);
                        }
                    }
                    System.out.println(sum+"zong de sum zhi\n");
                    System.out.println(APtimes+"zong de APtimes zhi\n");
                    ap.level = (int)Math.round(sum / APtimes);
                    tempPoint.aps.put(ap.BSSID, ap);
                }
            }
        };

        int count = timesValue;
        while (count -- > 0) {
            handler.postDelayed(scan, delay);
            delay += interval;
        }
        handler.postDelayed(tact, delay);
        handler.sendEmptyMessage(DONE);
	}
	
	/**
	 * 计算距离，并且找出最小距离的点和值。
	 */
	private void calculate() {
		handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                minDistance = Double.MAX_VALUE;
                int mini = -1;
                double tempDistance;

                for (int i = 0; i < totalPoints.size(); i++) {
                    tempDistance = calculate_Distance(tempPoint, totalPoints.get(i));

                    //System.out.println("No."+i+"tempDistance:"+tempDistance);
                    if(i==0){
                        logger.log("\nNo." + i + " tempDistance:" + tempDistance + "\n");
                    }else {
                        logger.log("No." + i + " tempDistance:" + tempDistance + "\n");
                    }

                    if (tempDistance <= minDistance) {
                        minDistance = tempDistance;
                        mini = i;
                    }
                }
                logger.log("minDistance:" + minDistance + "\n");
                logger.log("mini:" + mini + "\n");

                nearestPoint = totalPoints.get(mini);

            }
        }, delay);
	}

	/**
	 * 计算两点之间的距离
	 * @param point1 First point
	 * @param point2 Second point
	 * @return double
	 */
	private double calculate_Distance(Point point1, Point point2) {

        HashMap<String, AP> p1Aps = point1.aps;
        HashMap<String, AP> p2Aps = point2.aps;

        //return geometryDistance(p1Aps, p2Aps);
        return sinDistance(p1Aps, p2Aps);
	}

    /**
     * 使用几何距离来表示相似度
     * @param vec1 第一个向量
     * @param vec2 第二个向量
     * @return 相似度
     */
    private double geometryDistance(HashMap<String, AP> vec1, HashMap<String, AP> vec2) {
        double ret = 0;

        for (String str : minLevel.keySet()) {
            if (vec1.containsKey(str) && vec2.containsKey(str)) {
                ret += (vec1.get(str).level - vec2.get(str).level)
                        * (vec1.get(str).level - vec2.get(str).level);
            }

            if (vec1.containsKey(str) && !vec2.containsKey(str)) {
                ret += (vec1.get(str).level - minLevel.get(str))
                        * (vec1.get(str).level - minLevel.get(str));
            }

            if (!vec1.containsKey(str) && vec2.containsKey(str)) {
                ret += (vec2.get(str).level - minLevel.get(str))
                        * (vec2.get(str).level - minLevel.get(str));
            }
        }

        return Math.sqrt(ret);
    }

    /**
     * 采用余弦相似性来匹配
     * @param vec1 第一个点
     * @param vec2 第二个点
     * @return 差异度 [0,1] 越小差异度越高
     */
    private double sinDistance(HashMap<String, AP> vec1, HashMap<String, AP> vec2) {

        double RSSI1, RSSI2;
        double sigmaA = 0, sigmaB = 0, sigmaMutiply = 0;

        for (String str : minLevel.keySet()) {
            if (vec1.containsKey(str)) {
                RSSI1 = vec1.get(str).level;
            } else {
                RSSI1 = minLevel.get(str);
            }

            if (vec2.containsKey(str)) {
                RSSI2 = vec2.get(str).level;
            } else {
                RSSI2 = minLevel.get(str);
            }

            sigmaA += RSSI1 * RSSI1;
            sigmaB += RSSI2 * RSSI2;
            sigmaMutiply += RSSI1 * RSSI2;
        }

        return 1.0d - sigmaMutiply / Math.sqrt(sigmaA * sigmaB);
    }

	
	/**
	 * 将Wifif扫描的信息写入文件
	 * @param fileName the file name
	 * @param target Object to be written
	 */
	private void writeToFile(String fileName, Object target) {

		try {

			File file = new File("/mnt/sdcard", fileName + ".dat");
			if (!file.exists()) {
				file.createNewFile();
			}
			OutputStream out = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(out);

            oos.writeObject(target);

			oos.close();
            out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    private Object readFromFile(String fileName) {
        Object ret = null;

        try {

            File file = new File("/mnt/sdcard", fileName + ".dat");
            if (file.exists()) {
                InputStream is = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(is);
                ret = ois.readObject();

                ois.close();
                is.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, 0, 0, "Refresh");
		return super.onCreateOptionsMenu(menu);
	}

	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		wifiManager.startScan();
		Toast.makeText(this, "Start scanning", Toast.LENGTH_LONG).show();
		return super.onMenuItemSelected(featureId, item);
	}

}

