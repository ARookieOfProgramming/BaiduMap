package com.zhouzhou.attendance.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.View;
import android.widget.EditText;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

/**
 * author : ZhouZhou
 * e-mail : zhou.zhou@sim.com
 * date   : 19-11-20下午2:01
 * desc   :
 * version: 1.0
 */
public class LocationService {
    private static LocationClient client = null;
    private static LocationClientOption defaultOption;
    private static LocationClientOption DIYOption;
    private Object lock;
    private Context mContext;

    public LocationService(Context context) {
        lock = new Object();
        synchronized (lock){
            if (client == null){
                client = new LocationClient(context,getDefaultOption());
                this.mContext = context;
            }
        }
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-20
     *@Deecribe：百度地图监听注册
     *@Params:[listener]
     *@Return:boolean 是否注册成功
     *@Email：zhou.zhou@sim.com
     */
    public boolean registerListener(BDAbstractLocationListener listener){

        Boolean isSuccess = false;
        if (listener != null){
            client.registerLocationListener(listener);
            isSuccess = true;
        }
        return isSuccess;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-20
     *@Deecribe：百度地图监听注销
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */

    public void unRegisterListener(BDAbstractLocationListener listener){
        if (listener != null){
            client.unRegisterLocationListener(listener);
        }
    }



    /*
    *@Author: zhouzhou
    *@Date: 19-11-20
    *@Deecribe：设置定位参数
    *@Params:[option]
    *@Return:boolean
    *@Email：zhou.zhou@sim.com
    */
    public static boolean setLocationOption(LocationClientOption option) {
        boolean isSuccess = false;
        if (option != null) {
            if (client.isStarted()) {
                client.stop();
            }
            DIYOption = option;
            client.setLocOption(option);
        }
        return isSuccess;
    }

    /*
    *@Author: zhouzhou
    *@Date: 19-11-20
    *@Deecribe：获得option
    *@Params:
    *@Return:
    *@Email：zhou.zhou@sim.com
    */
    public LocationClientOption getOption() {
        if (DIYOption == null) {
            DIYOption = new LocationClientOption();
        }
        return DIYOption;
    }
    /*
    *@Author: zhouzhou
    *@Date: 19-11-20
    *@Deecribe：默认的定位相关设置
    *@Params:[LocationClientOption]定位相关设置
    *@Return:LocationClientOption
    *@Email：zhou.zhou@sim.com
    */
    private LocationClientOption getDefaultOption() {
        if (defaultOption == null){
            defaultOption = new LocationClientOption();
            defaultOption = new LocationClientOption();
            defaultOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            defaultOption.setCoorType( "bd09ll" ); // 可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            defaultOption.setScanSpan(1000); // 可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
            defaultOption.setIsNeedAddress(true); // 可选，设置是否需要地址信息，默认不需要
            defaultOption.setIsNeedLocationDescribe(true); // 可选，设置是否需要地址描述
            defaultOption.setNeedDeviceDirect(false); // 可选，设置是否需要设备方向结果
            defaultOption.setLocationNotify(false); // 可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            defaultOption.setIgnoreKillProcess(true); // 可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop
            defaultOption.setIsNeedLocationDescribe(true); // 可选，默认false，设置是否需要位置语义化结果，可以在BDLocation
            defaultOption.setIsNeedLocationPoiList(true); // 可选，默认false，设置是否需要POI结果，可以在BDLocation
            defaultOption.SetIgnoreCacheException(false); // 可选，默认false，设置是否收集CRASH信息，默认收集
            defaultOption.setOpenGps(true); // 可选，默认false，设置是否开启Gps定位
            defaultOption.setIsNeedAltitude(false); // 可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
        }
        return defaultOption;
    }


    public boolean isStart(){
        return client.isStarted();
    }

    public void stop(){
        synchronized (lock){
            if (client != null && client.isStarted()){
                client.stop();
            }
        }
    }
    public void start(){
        synchronized (lock){
            if (client != null && !client.isStarted()){
                client.start();
            }
        }
    }
}
