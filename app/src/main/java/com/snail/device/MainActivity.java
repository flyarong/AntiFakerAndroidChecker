package com.snail.device;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.snail.antifake.IEmulatorCheck;
import com.snail.antifake.deviceid.AndroidDeviceIMEIUtil;
import com.snail.antifake.deviceid.IpScanner;
import com.snail.antifake.deviceid.androidid.IAndroidIdUtil;
import com.snail.antifake.deviceid.androidid.ISettingUtils;
import com.snail.antifake.deviceid.deviceid.DeviceIdUtil;
import com.snail.antifake.deviceid.deviceid.IPhoneSubInfoUtil;
import com.snail.antifake.deviceid.deviceid.ITelephonyUtil;
import com.snail.antifake.deviceid.emulator.EmuCheckUtil;
import com.snail.antifake.deviceid.macaddress.IWifiManagerUtil;
import com.snail.antifake.deviceid.macaddress.MacAddressUtils;
import com.snail.antifake.jni.EmulatorCheckService;
import com.snail.antifake.jni.EmulatorDetectUtil;
import com.snail.antifake.jni.PropertiesGet;

import java.util.Map;

import static com.snail.device.CrashHandlerApplication.getApplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class MainActivity extends AppCompatActivity {

    private Activity mActivity;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mActivity = this;

        findViewById(R.id.btn_moni).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, EmulatorCheckService.class);
                bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE);

            }
        });

        findViewById(R.id.btn_sycn_moni).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 100; i++) {
                    TextView textView = (TextView) findViewById(R.id.btn_sycn_moni);
                    textView.setText(" 是否模拟器 " + EmulatorDetectUtil.isEmulator(MainActivity.this));
                }

            }
        });

        findViewById(R.id.btn_dna).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getHInfo(view);
            }
        });
    }

    final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IEmulatorCheck emulatorCheck = IEmulatorCheck.Stub.asInterface(service);
            if (emulatorCheck != null) {
                try {
                    TextView textView = (TextView) findViewById(R.id.btn_moni);
                    textView.setText(" 是否模拟器 " + emulatorCheck.isEmulator());
                    unbindService(this);
                } catch (RemoteException e) {
                    Toast.makeText(MainActivity.this, "获取进程崩溃", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getHInfo(null);
        }
    }


       @SuppressLint("SetTextI18n")
       public void  getHInfo(View v) {

        TextView textView = (TextView) findViewById(R.id.tv_getdeviceid);
        // 不同的版本不一样，4.3之前ITelephony没有getDeviceId
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    0);
            return;
        }
        textView.setText(
                "设备信息 \n最终方法获取IMEI  : " + DeviceIdUtil.getDeviceId(mActivity)
                        + "\n最终方法获取MAC地址 : " + MacAddressUtils.getMacAddress(mActivity)
                        + "\n最终方法获取AndroidID  : " + IAndroidIdUtil.getAndroidId(mActivity)
                        + "\n是否模拟器  : " + EmuCheckUtil.mayOnEmulator(mActivity)
                        + " \n\n可Hook系统API获取Deviceid: " + ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId()
                        + "\n真实 反Hook Proxy代理获取Deviceid : " + IPhoneSubInfoUtil.getDeviceIdLevel0(mActivity)
                        + "\n真实 反Hook Proxy代理获取Deviceid level1 :" + IPhoneSubInfoUtil.getDeviceIdLevel1(mActivity)
                        + "\n真实 反Hook Proxy代理获取Deviceid level2 :" + IPhoneSubInfoUtil.getDeviceIdLevel2(mActivity)
                        + "\n真实 ITelephonyUtil反Hook 获取DeviceId : " + ITelephonyUtil.getDeviceIdLevel0(mActivity)
                        + "\n真实 ITelephonyUtil反Hook 获取DeviceId level1 : " + ITelephonyUtil.getDeviceIdLevel1(mActivity)
                        + "\n自定义ServiceManager获取getDeviceId level2  :" + ITelephonyUtil.getDeviceIdLevel2(mActivity)
//                        + "\n" + EmuCheckUtil.getCpuInfo()
                        + "\n系统架构 " + PropertiesGet.getString("ro.product.cpu.abi")
                        + "\n获取链接的路由器地址" + MacAddressUtils.getConnectedWifiMacAddress(getApplication())
        );
        textView = (TextView) findViewById(R.id.tv_all);

        textView.setText("\n系统API反射获取序列号 ： " + SysAPIUtil.getSerialNumber(mActivity)
                + "\n系统API反射获取序列号 ： " + SysAPIUtil.getJavaSerialNumber(mActivity)
                + "\n直接通过 Build Serial " + Build.SERIAL
                + "\n通过ADB Build Serial " + AndroidDeviceIMEIUtil.getSerialno()
                + "\n直接native获取  Serial " + PropertiesGet.getString("ro.serialno")
                + "\n通过系统API获取MAC地址  ： " + SysAPIUtil.getMacAddress(mActivity)
                + "\nIwifmanager 获取mac level 0  ： " + IWifiManagerUtil.getMacAddress(mActivity)
                + "\n通过NetworkInterface获取MAC地址  ： " + MacAddressUtils.getMacAddressByWlan0(mActivity)
                + "\n系统API获取手机型号 （作假）  ： " + SysAPIUtil.getPhoneManufacturer()
                //Settings.Secure.ANDROID_ID Java类可以被HOOK 并且很简单
                + "\n通过系统API获取ANDROID_ID (XPOSED可以HOOK)  ： " + SysAPIUtil.getAndroidId(mActivity)
                + "\n反射获取系统 ANDROID_IDISettingUtils  ： " + ISettingUtils.getAndroidProperty(mActivity, Settings.Secure.ANDROID_ID)
                + "\n反射获取系统 ANDROID_ID ISettingUtils level2  ： " + ISettingUtils.getAndroidPropertyLevel1(mActivity, Settings.Secure.ANDROID_ID)
                + "\nnative ro.product.manufacturer" + PropertiesGet.getString("ro.product.manufacturer")
                + "\nnative ro.product.model  " + PropertiesGet.getString("ro.product.model")
                + "\nnative ro.product.device " + PropertiesGet.getString("ro.product.device")
                + "\nnative ro.kernel.qemu " + PropertiesGet.getString("ro.kernel.qemu")
                + "\nnative ro.product.name" + PropertiesGet.getString("ro.product.name")


        );

        AndroidDeviceIMEIUtil.getMac(new IpScanner.OnScanListener() {
            @Override
            public void scan(Map<String, String> resultMap) {
                Log.v("lishang", resultMap.toString());
            }
        });
    }
}
