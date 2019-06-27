package com.lansent.udp_demo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class FlowManager {
    private static Context context;
    private static FlowManager flowManager;

    public static FlowManager getFlowManager() {
        if (flowManager == null) return getInstance();
        return flowManager;
    }

    private String strFilePathFull = "/mnt/sdcard/Adx/NetFlowLogTest/Flowlog.txt";

    private NetWorkPacket lastNetWorkPackets = null;
    //差值包
    private NetWorkPacket realNetWorkPackets = new NetWorkPacket();
    private NetWorkPacket needSetNetWorkPackets = new NetWorkPacket();

    private FlowManager() {

    }

    public static boolean register(Context mContext) {
        if (context != null) {
            return false;
        }

        context = mContext;
        keepFlow();

        return false;
    }

    public NetWorkPacket getFlow() {

        if (context == null) {

            return null;
        }
        NetWorkPacket netWorkPacket =  strToObjNetPackets(readLastLine(null));


        return netWorkPacket;
    }

    public boolean clear() {
        if (context == null) {
            return false;
        }

        //写入一个空的NetWorkPackets
        NetWorkPacket nullNetWorkPacket = new NetWorkPacket();
        nullNetWorkPacket.bucketTime = 0;
        nullNetWorkPacket.packets = 0;
        nullNetWorkPacket.upPacket = 0;
        nullNetWorkPacket.downPacket = 0;

        if (writeTraffic(nullNetWorkPacket)) {

            lastNetWorkPackets = getAppFlowInfo(context.getPackageName(), context);
            Log.e("FlowManager", "lastNetWorkPackets" + "更新lastNetWorkPackets " + lastNetWorkPackets);
            return true;
        }

        return false;
    }


    //计算流量包
    private boolean addRealTimeTraffic(NetWorkPacket realFlowInfo) {

        packetsResult(realFlowInfo);

        if (writeTraffic(needSetNetWorkPackets)) {
            //Log.e("FlowManager", "lastNetWorkPackets" + "更新lastNetWorkPackets");
            //lastNetWorkPackets = needSetNetWorkPackets;
            return true;
        }

        return false;
    }


    private void packetsResult(NetWorkPacket netWorkPackets) {
        NetWorkPacket fileNetWorkPackets;
        //Log.e("FlowManager", "packetsResult");

        if (lastNetWorkPackets == null) {
            //Log.e("FlowManager", "  get  lastNetWorkPackets  " + "fail");
            lastNetWorkPackets = netWorkPackets;
            needSetNetWorkPackets = null;
            return;
        } else {
            //Log.e("FlowManager", "  get  lastNetWorkPackets  " + lastNetWorkPackets.toString());
            //如果可以计算出流量包
            realNetWorkPackets.bucketTime = netWorkPackets.bucketTime;
            realNetWorkPackets.upPacket = netWorkPackets.upPacket - lastNetWorkPackets.upPacket;
            realNetWorkPackets.downPacket = netWorkPackets.downPacket - lastNetWorkPackets.downPacket;
            realNetWorkPackets.packets = netWorkPackets.packets - lastNetWorkPackets.packets;
            //Log.e("FlowManager", "  get  realNetWorkPackets  " + realNetWorkPackets.toString());
            lastNetWorkPackets = netWorkPackets;
        }


        String lastLineDataJson = readLastLine(null);
        //Log.e("FlowManager", "lastLineDataJson  : " + lastLineDataJson);
        fileNetWorkPackets = strToObjNetPackets(lastLineDataJson);

        if (fileNetWorkPackets == null || fileNetWorkPackets.bucketTime == 0) {
            //Log.e("FlowManager", "  get  fileNetWorkPackets" + "fail");
            // 获取原始数据失败
            //写入已经计算的流量包
            needSetNetWorkPackets = realNetWorkPackets;
            return;

        } else {
            if (needSetNetWorkPackets == null) {
                needSetNetWorkPackets = new NetWorkPacket();
            }
            needSetNetWorkPackets.bucketTime = realNetWorkPackets.bucketTime;
            needSetNetWorkPackets.upPacket = fileNetWorkPackets.upPacket + realNetWorkPackets.upPacket;
            needSetNetWorkPackets.downPacket = fileNetWorkPackets.downPacket + realNetWorkPackets.downPacket;
            needSetNetWorkPackets.packets = fileNetWorkPackets.packets + realNetWorkPackets.packets;
            //Log.e("FlowManager", "  get  needSetNetWorkPackets  " + needSetNetWorkPackets.toString());

        }


    }



    private String readLastLine(String charset) {

        String s = null;
        RandomAccessFile raf = null;
        try {
            File file = getLogFile();
            raf = new RandomAccessFile(file, "r");

            long len = raf.length();
            if (len == 0L) {
                return "";
            } else {
                raf.seek(0);
                s = raf.readLine();
            }
        } catch (Exception e) {
            return "";
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception e2) {
                }
            }
        }

        return s;
    }

    private synchronized boolean writeTxtToFile(String packetsJson) {

        RandomAccessFile raf = null;

        try {
            File file = getLogFile();
            Log.e("hanpackets", " to  file :" + packetsJson);

            raf = new RandomAccessFile(file, "rwd");
            raf.setLength(0);

            raf.seek(0);
            raf.writeBytes(packetsJson);
            //test
            // raf.seek(0);
            // Log.e("hanpackets", "strcontent :" + raf.readLine());
            raf.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (Exception e2) {
                }
            }
        }
    }

    private NetWorkPacket strToObjNetPackets(String lastLineDataJson) {

        NetWorkPacket p = new NetWorkPacket();
        if (lastLineDataJson == null || lastLineDataJson.isEmpty()) {
            return null;
        } else {
            try {
                JsonObject jsonObject = (JsonObject) new JsonParser().parse(lastLineDataJson);
                p.bucketTime = jsonObject.get("time").getAsLong();
                p.upPacket = jsonObject.get("upPackets").getAsLong();
                p.downPacket = jsonObject.get("downPackets").getAsLong();
                p.packets = jsonObject.get("packets").getAsLong();
                return p;
            } catch (Exception e) {
                return null;
            }
        }
    }

    private File getLogFile() throws IOException {

        File file = new File(strFilePathFull);
        if (!file.exists()) {
            file.getParentFile().mkdir();
            file.createNewFile();


        } else {
            return file;
        }
        RandomAccessFile r = null;
        try {
            r = new RandomAccessFile(file, "rwd");
            //r.setLength(1000);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return file;

    }


    private boolean writeTraffic(NetWorkPacket netWorkPackets) {
        if (netWorkPackets == null) {
            return false;
        }
        //{"time":123456,"packets":12345}


        long realTime = netWorkPackets.bucketTime;
        long realUpPackets = netWorkPackets.upPacket;
        long realDownPackets = netWorkPackets.downPacket;
        long realPackets = netWorkPackets.packets;

        // 每次写入1行
        String packetsJson = "{\"time\":" + realTime +
                ",\"packets\":" + realPackets +
                ",\"upPackets\":" + realUpPackets +
                ",\"downPackets\":" + realDownPackets +
                "}";
        return writeTxtToFile(packetsJson);
    }


    private static void keepFlow() {
        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    for (; ; ) {
                        if (context == null) {
                            return;
                        }
                        NetWorkPacket appFlowInfoStart = getAppFlowInfo(context.getPackageName(), context);
                        getInstance().addRealTimeTraffic(appFlowInfoStart);
                        sleep(20000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();


    }


    private static FlowManager getInstance() {
        Log.d("FlowManager", "getInstance :");
        if (flowManager != null) {
            return flowManager;
        } else {
            synchronized (FlowManager.class) {
                if (flowManager == null) {
                    flowManager = new FlowManager();
                }
            }
        }
        return flowManager;
    }


    private static NetWorkPacket getAppFlowInfo(String pakageName, Context context) {

        PackageManager pms = context.getPackageManager();
        List<PackageInfo> packinfos = pms
                .getInstalledPackages(PackageManager.GET_PERMISSIONS);
        NetWorkPacket flowInfo = new NetWorkPacket();
        for (PackageInfo packinfo : packinfos) {
            String appName = packinfo.packageName;
            if (!TextUtils.isEmpty(appName)) {
                if (appName.equals(pakageName)) {
                    int uid = packinfo.applicationInfo.uid;

                    flowInfo.upPacket = TrafficStats.getUidRxBytes(uid);
                    flowInfo.downPacket = TrafficStats.getUidTxBytes(uid);
                    flowInfo.packets = flowInfo.upPacket+flowInfo.downPacket;
                    flowInfo.bucketTime = System.currentTimeMillis();
                    break;
                }
            }
        }
        return flowInfo;
    }

}
