package com.reactnativerfidchainwayr6;

import com.facebook.react.module.annotations.ReactModule;
import com.RNrfid.rfidCustom.*;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.rscja.deviceapi.RFIDWithUHFBLE;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.ConnectionStatus;
import com.rscja.deviceapi.interfaces.ConnectionStatusCallback;
import com.rscja.deviceapi.interfaces.KeyEventCallback;
import com.rscja.deviceapi.interfaces.ScanBTCallback;


import java.util.ArrayList;
import java.util.List;


@ReactModule(name = RfidChainwayR6Module.NAME)
public class RfidChainwayR6Module extends ReactContextBaseJavaModule {
    public RFIDWithUHFBLE uhf = RFIDWithUHFBLE.getInstance();
    private static ReactApplicationContext reactcontext;
    public BluetoothAdapter mBtAdapter = null;
    private List<MyDevice> deviceList = new ArrayList<>();
    BTStatus btStatus = new BTStatus();
    private String devicesConnect = "";
    public boolean isSupportRssi=false;
    public boolean isScanning = false;
    private List<UHFTAGInfo> tempDatas = new ArrayList<>();
    private static final int ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 100;
    private boolean isExit = false;
    public static final String NAME = "RfidChainwayR6";

    public RfidChainwayR6Module(ReactApplicationContext reactContext) {
        super(reactContext);
        reactcontext = reactContext;
        uhf.init(reactContext);
        Utils.initSound(reactContext);
        isExit = false;
        // sự kiện khi ấn nút trên tay cầm sled Reader
        uhf.setKeyEventCallback(new KeyEventCallback() {
            @Override
            public void onKeyDown(int keycode) {
                if (!isExit && uhf.getConnectStatus() == ConnectionStatus.CONNECTED) {
                    if(keycode==3){
                        startScanRFID();
                    }else{
                        if(keycode==1) {
                            if (isScanning) {
                                stop();
                            } else {
                                startScanRFID();
                            }
                        }else{
                            if (isScanning) {
                                stop();
                                SystemClock.sleep(100);
                            }
                            inventory();
                        }
                    }

                }
            }

            @Override
            public void onKeyUp(int keycode) {
                stop();
            }
        });
    }

    @Override
    @NonNull
    public String getName() {
        return NAME;
    }


    //Send event sang js
    private void sendEvent(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
      reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    //Check quyền location khi scan bluetooth devices
    private boolean checkLocationEnable() {
      boolean result = true;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          if (getCurrentActivity().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
              getCurrentActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION_REQUEST);
              result = false;
          }
      }
      return result;
    }

    //Callback khi scan BLE devices
    ScanBTCallback callback = new ScanBTCallback() {
      @Override
      public void getDevices(BluetoothDevice bluetoothDevice, final int rssi, byte[] bytes) {
          try {
              MyDevice myDevice = new MyDevice(bluetoothDevice.getAddress(), bluetoothDevice.getName());
              boolean deviceFound = false;
              for (MyDevice listDev : deviceList) {
                  if (listDev.getAddress().equals(myDevice.getAddress())) {
                      deviceFound = true;
                      break;
                  }
              }
              if (!deviceFound) {
                  WritableMap payload = Arguments.createMap();
                  payload.putString("name_device", myDevice.getName());
                  payload.putString("address_device", myDevice.getAddress());
                  payload.putString("rssi", String.valueOf(rssi));
                  deviceList.add(myDevice);
                  sendEvent(reactcontext, "ScanBLEListenner", payload);
              }
          } catch (Exception e) {
              Log.d("Long check",""+ e);
          }

      }
    };

    //Sự kiện khi tắt app
    @Override
    public void onCatalystInstanceDestroy() {
      super.onCatalystInstanceDestroy();
      uhf.disconnect();
      tempDatas.clear();
      deviceList.clear();
      isExit = true;
    }

    //Sự kiện scan bluetooth devices call từ js
    @ReactMethod
    public void ScanBLE() {
      if (checkLocationEnable()) {
          deviceList.clear();
          mBtAdapter = BluetoothAdapter.getDefaultAdapter();
          if (mBtAdapter == null) {
              Log.d("long check", "Bluetooth is not available");
              return ;
          }
          if (!mBtAdapter.isEnabled()) {
              Log.d("long check", "pls turn on ble");
              return ;
          }
          uhf.startScanBTDevices(callback);
      }
    }

    //lớp lấy status connect khi kết nối bluetooth
    class BTStatus implements ConnectionStatusCallback<Object> {
      @Override
      public void getStatus(final ConnectionStatus connectionStatus, final Object device1) {
          getCurrentActivity().runOnUiThread(new Runnable() {
              public void run() {
                  BluetoothDevice device = (BluetoothDevice) device1;
                  if (connectionStatus == ConnectionStatus.CONNECTED) {
                      devicesConnect = device.getName() + "(" + device.getAddress() + ")";
                  }
              }
          });
      }
    }

    //fucn kết nối qua address devices call từ js
    @ReactMethod
    public void connectAddress(String address, final Promise promise) {
      uhf.connect(address, btStatus);
      final Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
          @Override
          public void run() {
              // Write whatever to want to do after delay specified (1 sec)
              promise.resolve(devicesConnect);
          }
      }, 3000);
    }

    //func stop scan bluetooth devices
    @ReactMethod
    public void stopScanBLE()
    {
      uhf.stopScanBTDevices();
    }

    //func stop scan bluetooth devices
//    @ReactMethod
//    public void getPowerRFID()
//    {
//      return uhf.getPower();
//    }

    //func set power RFID device
    @ReactMethod
    public void setPower(int iPower){
    int iPow = Integer.valueOf(iPower);
    //   uhf.setPower(iPow);
      if(uhf.setPower(iPow)){
        // Log.e("Minh logg","Set power RFID success!!!" + uhf.setPower(iPow) );
        Toast.makeText( getCurrentActivity() , "Set power RFID success!!!", Toast.LENGTH_SHORT).show();
    }else{
        // Log.e("Minh logg","Set power RFID error, pls try again!!!" + uhf.setPower(iPow) );
        Toast.makeText( getCurrentActivity() , "Set power RFID error, pls try again!!!", Toast.LENGTH_SHORT).show();
    }
    }

    // PHẦN ĐỌC RFID

    final int FLAG_START = 0;
    final int FLAG_STOP = 1;
    final int FLAG_UPDATE_TIME = 2;
    final int FLAG_UHFINFO = 3;
    final int FLAG_UHFINFO_LIST = 5;
    final int FLAG_SUCCESS = 10;
    final int FLAG_FAIL = 11;

    //Tạo object thông tin của 1 rfid và call event send js
    void insertTag(UHFTAGInfo info, int index, boolean exists){
      WritableMap payload = Arguments.createMap();
      if(!exists){
          payload.putString("rfid_tag", info.getEPC());
          payload.putString("rssi", info.getRssi());
          tempDatas.add(index, info);
          sendEvent(reactcontext, "ReadRFIDListenner", payload);
      }
    }

    //Check rfid đã có trong list chưa và call thêm vào list tag
    private void emitRfid(List<UHFTAGInfo> list) {
      for(int k=0; k<list.size(); k++){
          boolean[] exists = new boolean[1];
          UHFTAGInfo info=list.get(k);
          int idx=CheckUtils.getInsertIndex(tempDatas, info, exists);
          insertTag(info, idx, exists[0]);
      }
    }

    // Các event sử lý
    Handler handler = new Handler(Looper.getMainLooper()) {
      @Override
      public void handleMessage(Message msg) {
          switch (msg.what) {
              case FLAG_STOP:
                  if (msg.arg1 == FLAG_SUCCESS) {

                  } else {
                      Utils.playSound(2);
                      Toast.makeText(getCurrentActivity(), "Stop failure", Toast.LENGTH_SHORT).show();
                  }
                  break;
              case FLAG_UHFINFO_LIST:
                  List<UHFTAGInfo> list = ( List<UHFTAGInfo>) msg.obj;
                  emitRfid(list);
                  break;
              case FLAG_START:
                  if (msg.arg1 == FLAG_SUCCESS) {

                  } else {
                      Utils.playSound(2);
                  }
                  break;
              case FLAG_UPDATE_TIME:

                  break;
              case FLAG_UHFINFO:
                  UHFTAGInfo info = (UHFTAGInfo) msg.obj;
                  List list1=new ArrayList<UHFTAGInfo>();
                  list1.add(info);
                  emitRfid(list1);
                  break;
          }
      }
    };

    private synchronized List<UHFTAGInfo> getUHFInfo() {
      List<UHFTAGInfo> list = null;
      if(isSupportRssi){
          list = uhf.readTagFromBufferList_EpcTidUser();
      }else {
          list = uhf.readTagFromBufferList();
      }
      return list;
    }

    // đọc từng rfid
    private void inventory() {
      UHFTAGInfo info = uhf.inventorySingleTag();
      if (info != null) {
          Message msg = handler.obtainMessage(FLAG_UHFINFO);
          msg.obj = info;
          handler.sendMessage(msg);
      }
    }

    //Call khi dừng scan rfid
    private void stopInventory(){
      boolean result = uhf.stopInventory();
      ConnectionStatus connectionStatus = uhf.getConnectStatus();
      Message msg = handler.obtainMessage(FLAG_STOP);
      if (!result || connectionStatus == ConnectionStatus.DISCONNECTED) {
          msg.arg1 = FLAG_FAIL;
      } else {
          msg.arg1 = FLAG_SUCCESS;
      }
      handler.sendMessage(msg);
    }

    //Thread xử lý đọc Rfid
    class TagThread extends Thread {
      public void run() {
          Message msg = handler.obtainMessage(FLAG_START);
          if (uhf.startInventoryTag()) {
              msg.arg1 = FLAG_SUCCESS;
          } else {
              msg.arg1 = FLAG_FAIL;
              isScanning=false;
          }
          handler.sendMessage(msg);
          while (isScanning) {
              List<UHFTAGInfo> list = getUHFInfo();
              if(list==null || list.size()==0){
                  SystemClock.sleep(1);
              }else{
                  Utils.playSound(1);
                  handler.sendMessage(handler.obtainMessage(FLAG_UHFINFO_LIST, list));
              }
          }
          stopInventory();
      }
    }

    //Func start read rfid call từ js
    @ReactMethod
    public void startScanRFID() {
      isScanning = true;
      new TagThread().start();
    }

    //Func clear rfid call từ js
    @ReactMethod
    private void clearData(final Promise promise) {
      try {
          tempDatas.clear();
          promise.resolve(true);
      } catch (Exception e) {
          promise.reject(e);
      }
    }

    //Func stop read rfid call từ js
    @ReactMethod
    private void stop() {
      isScanning = false;
    }

    //Lớp thiết bị bluetooth
    class MyDevice {
      private String address;
      private String name;
      private int bondState;

      public MyDevice() {

      }

      public MyDevice(String address, String name) {
          this.address = address;
          this.name = name;
      }

      public String getAddress() {
          return address;
      }

      public void setAddress(String address) {
          this.address = address;
      }

      public String getName() {
          return name;
      }

      public void setName(String name) {
          this.name = name;
      }

      public int getBondState() {
          return bondState;
      }

      public void setBondState(int bondState) {
          this.bondState = bondState;
      }
    }
}
