package com.example.administrator.wifihotdemoclient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.scan)
    TextView scan;
    private List<ScanResult> wifiList;
    private WifiManager wifiManager;
    private boolean isConnected=false;
    private String name,pwd;
    /*连接到热点*/
    public void connectToHotpot(){
        WifiConfiguration wifiConfig=this.setWifiParams();
        int wcgID = wifiManager.addNetwork(wifiConfig);
        boolean flag=wifiManager.enableNetwork(wcgID, true);
        isConnected=flag;
        System.out.println("connect success? "+flag);
    }
    /*设置要连接的热点的参数*/
    public WifiConfiguration setWifiParams(){
        WifiConfiguration apConfig=new WifiConfiguration();
        apConfig.SSID="\""+name+"\"";
        apConfig.preSharedKey="\""+pwd+"\"";
        apConfig.hiddenSSID = true;
        apConfig.status = WifiConfiguration.Status.ENABLED;
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        apConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        apConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        apConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        apConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        return apConfig;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestAllPower();
        ButterKnife.bind(this);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            //setWifiEnabled接口用于开启Wifi
            wifiManager.setWifiEnabled(true);
        }
        wifiManager.startScan();
        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doScan();
            }
        });
    }

    private void doScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constant.REQ_PERM_CAMERA);
            return;
        }
        // 二维码扫码
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, Constant.REQ_QR_CODE);
    }

    public void requestAllPower() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_SETTINGS)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_SETTINGS,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.INTERNET,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CHANGE_CONFIGURATION,Manifest.permission.CHANGE_NETWORK_STATE,Manifest.permission.CHANGE_WIFI_STATE,Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.ACCESS_WIFI_STATE}, 1);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);
            Log.e("result",scanResult);
            if(scanResult.startsWith("ConnectWifi:")){
                String msg = scanResult.replace("ConnectWifi:","");
                try {
                    JSONObject json = new JSONObject(msg);
                    name = json.getString("name");
                    pwd = json.getString("pwd");
                    wifiList = wifiManager.getScanResults();
                    if(wifiList==null){
                        Toast.makeText(MainActivity.this, "未搜索到wifi列表", Toast.LENGTH_LONG).show();
                    }else{
                        if(wifiList.size()==0){
                            Toast.makeText(MainActivity.this, "请确认wifi是否开启", Toast.LENGTH_LONG).show();
                        }else{
                            boolean boo = false;
                            for (ScanResult rst:wifiList){
                                if(rst.SSID.contains(name)){
                                    boo = true;
                                }
                            }
                            if(boo){
                                connectToHotpot();
                            }else{
                                Toast.makeText(MainActivity.this, "请确认wifi是否开启", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this,"jsonerror:"+e.getMessage(),Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    doScan();
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}
