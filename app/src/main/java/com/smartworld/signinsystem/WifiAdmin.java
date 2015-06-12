package com.smartworld.signinsystem;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;


public class WifiAdmin {

    private static final String TAG = "WifiAdmin";

    private WifiManager mWifiManager;
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表  
    private List<ScanResult> mWifiList;
    private List<WifiConfiguration> mWifiConfiguration;

    private WifiLock mWifiLock;

    private String mPasswd = "";
    private String mSSID = "";

    private Context mContext = null;

    public WifiAdmin(Context context) {

        mContext = context;

        // 取得WifiManager对象  
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象  
        mWifiInfo = mWifiManager.getConnectionInfo();

        Log.v(TAG, "getIpAddress = " + mWifiInfo.getIpAddress());
    }

    // 打开WIFI  
    public void openWifi() {
        boolean isSuccess = false;
        if (!mWifiManager.isWifiEnabled()) {
            isSuccess = mWifiManager.setWifiEnabled(true);
        }else
            isSuccess = true;
        Log.d(TAG, "openWifi:" + isSuccess);
        Log.d(TAG,"wifiEnabled:"+mWifiManager.isWifiEnabled());
    }


    public int getServerAddr(){
        return mWifiManager.getDhcpInfo().serverAddress;
    }
    // 关闭WIFI  
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }



    // 添加一个网络并连接  
    public void addNetwork(WifiConfiguration wcg) {


        WifiApAdmin.closeWifiAp(mContext);

        int wcgID = mWifiManager.addNetwork(wcg);
        boolean b = mWifiManager.enableNetwork(wcgID, true);
    }

    public static final int TYPE_NO_PASSWD = 0x11;
    public static final int TYPE_WEP = 0x12;
    public static final int TYPE_WPA = 0x13;

    public void addNetwork(String ssid, String passwd, int type) {
        if (ssid == null || passwd == null || ssid.equals("")) {
            Log.e(TAG, "addNetwork() ## nullpointer error!");
            return;
        }

        if (type != TYPE_NO_PASSWD && type != TYPE_WEP && type != TYPE_WPA) {
            Log.e(TAG, "addNetwork() ## unknown type = " + type);
        }



        addNetwork(createWifiInfo(ssid, passwd, type));
    }


    private final int STATE_REGISTRING = 0x01;
    private final int STATE_REGISTERED = 0x02;
    private final int STATE_UNREGISTERING = 0x03;
    private final int STATE_UNREGISTERED = 0x04;

    private int mHaveRegister = STATE_UNREGISTERED;






    public WifiConfiguration createWifiInfo(String SSID, String password, int type) {

        Log.v(TAG, "SSID = " + SSID + "## Password = " + password + "## Type = " + type);

        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        // 分为三种情况：1没有密码2用wep加密3用wpa加密  
        if (type == TYPE_NO_PASSWD) {// WIFICIPHER_NOPASS  
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;

        } else if (type == TYPE_WEP) {  //  WIFICIPHER_WEP   
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers
                    .set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == TYPE_WPA) {   // WIFICIPHER_WPA  
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);  
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    public static final int WIFI_CONNECTED = 0x01;
    public static final int WIFI_CONNECT_FAILED = 0x02;
    public static final int WIFI_CONNECTING = 0x03;
    /**
     * 判断wifi是否连接成功,不是network 
     *
     * @param context
     * @return
     */
    public int isWifiContected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        Log.v(TAG, "isConnectedOrConnecting = " + wifiNetworkInfo.isConnectedOrConnecting());
        Log.d(TAG, "wifiNetworkInfo.getDetailedState() = " + wifiNetworkInfo.getDetailedState());
        if (wifiNetworkInfo.getDetailedState() == DetailedState.OBTAINING_IPADDR
                || wifiNetworkInfo.getDetailedState() == DetailedState.CONNECTING) {
            return WIFI_CONNECTING;
        } else if (wifiNetworkInfo.getDetailedState() == DetailedState.CONNECTED) {
            return WIFI_CONNECTED;
        } else {
            Log.d(TAG, "getDetailedState() == " + wifiNetworkInfo.getDetailedState());
            return WIFI_CONNECT_FAILED;
        }
    }

    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"") /*&& existingConfig.preSharedKey.equals("\"" + password + "\"")*/) {
                    return existingConfig;
                }
            }
        }
        Log.d("existingConfigs","null");
        return null;
    }



    // 断开指定ID的网络  
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

    // 检查当前WIFI状态  
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // 锁定WifiLock  
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock  
    public void releaseWifiLock() {
        // 判断时候锁定  
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock  
    public void creatWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("Test");
    }

    // 得到配置好的网络  
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接  
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回  
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络  
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,
                true);
    }

    public void startScan() {
        mWifiManager.startScan();

    }

    // 得到网络列表  
    public List<ScanResult> getWifiList() {
        mWifiList = mWifiManager.getScanResults();
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
        return mWifiList;
    }

    // 查看扫描结果  
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder
                    .append("Index_" + new Integer(i + 1).toString() + ":");
            // 将ScanResult信息转换成一个字符串包  
            // 其中把包括：BSSID、SSID、capabilities、frequency、level

            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
            Log.d("scan result:",stringBuilder.toString());
        }
        return stringBuilder;
    }

    // 得到MAC地址  
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID  
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到IP地址  
    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // 得到连接的ID  
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // 得到WifiInfo的所有信息包  
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }
}  