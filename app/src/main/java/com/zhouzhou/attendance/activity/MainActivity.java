package com.zhouzhou.attendance.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDNotifyListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.zhouzhou.attendance.R;
//import com.zhouzhou.attendance.service.LocationService;
import com.zhouzhou.attendance.service.utils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.baidumap)
    MapView baidumap;
    @BindView(R.id.design_navigation_view)
    NavigationView designNavigationView;
    @BindView(R.id.design_drawer_view)
    DrawerLayout designDrawerView;

    private static LocationClient client = null;
    private static LocationClientOption defaultOption;
    private static LocationClientOption DIYOption;
    private BaiduMap mBaiduMap;
    private LinkedList<LocationEntity> locationList = new LinkedList<LocationEntity>(); // 存放历史定位结果的链表，最大存放当前结果的前5次定位结果
    private double lat;//纬度
    private double lon;//经度
    private Marker marker;
    private boolean isFirst;
    private BDNotifyListener myListener = new MyNotifyListener();
    private Vibrator mVibrator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getPerssions();
        initToolBar();

        client = new LocationClient(getApplicationContext());
        client.registerLocationListener(mListener);
        setDefaultOption();

        mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
        client.start();

        mBaiduMap = baidumap.getMap();
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18f));
        mBaiduMap.setMyLocationEnabled(true);


    }
    private Handler notifyHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            myListener.SetNotifyLocation(lat,lon, 3000,"ww");//4个参数代表要位置提醒的点的坐标，具体含义依次为：纬度，经度，距离范围，坐标系类型(gcj02,gps,bd09,bd09ll)
        }
    };


    private void getPerssions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> perssionList = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perssionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                perssionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (perssionList.size() > 0) {
                requestPermissions(perssionList.toArray(new String[perssionList.size()]), utils.REQUESTCODE);
            }
        }

    }
    BDAbstractLocationListener mListener = new BDAbstractLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation location) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            if (location != null && (location.getLocType() == 161 || location.getLocType() == 66)) {
//                lat = location.getLatitude();
//                lon = location.getLongitude();
                //Message locMsg = locHander.obtainMessage();
                //Bundle Data;
                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(location.getDirection()).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();
                mBaiduMap.setMyLocationData(locData);
                if (!isFirst){
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(point));
                    isFirst = true;
                }
            }
            notifyHandler.sendEmptyMessage(0);
        }
    };

    
    private void initToolBar() {
        toolbar.setLogo(R.drawable.sign_in);
        toolbar.setNavigationIcon(R.drawable.menu);
        toolbar.setTitle("考勤打卡");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        setSupportActionBar(toolbar);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case utils.REQUESTCODE:
                Toast.makeText(this, "hah", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tool_bar, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        View view =  getLayoutInflater().inflate(R.layout.edit_input,null);
        EditText editText = view.findViewById(R.id.et_input);
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_location:
                LatLng latLng = new LatLng(lat, lon);
                MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(latLng);
                mBaiduMap.animateMapStatus(update);
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.zoomTo(18.f);
                mBaiduMap.animateMapStatus(mapStatusUpdate);
                break;
            case R.id.action_distance_sign_in:
                inputDialog(editText);
                break;
            case R.id.action_distance_sign_out:
                inputDialog(editText);
                break;
            case R.id.action_time_sign_out:
                Toast.makeText(this, "设置3", Toast.LENGTH_SHORT).show();
                break;
            case R.id.action_time_auto:
                Toast.makeText(this, "设置4", Toast.LENGTH_SHORT).show();
                break;
            case android.R.id.home:
                designDrawerView.openDrawer(GravityCompat.START);
                break;
        }

        return true;
    }

    /*
     *@Author: zhouzhou
     *@Date: 19-11-20
     *@Deecribe：获得弹窗中输入的内容
     *@Params:
     *@Return:
     *@Email：zhou.zhou@sim.com
     */
    private void inputDialog(final EditText editText){
        AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, editText.getText(), Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }).setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();

    }
    /*
     *@Author: zhouzhou
     *@Date: 19-11-20
     *@Deecribe：默认的定位相关设置
     *@Params:[LocationClientOption]定位相关设置
     *@Return:LocationClientOption
     *@Email：zhou.zhou@sim.com
     */
    private void setDefaultOption() {
        if (defaultOption == null) {
            defaultOption = new LocationClientOption();
            defaultOption = new LocationClientOption();
            defaultOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy); // 可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            defaultOption.setCoorType("bd09ll"); // 可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
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
        client.setLocOption(defaultOption);
    }
    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        baidumap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        baidumap.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        client.unRegisterLocationListener(mListener);
        mBaiduMap.setMyLocationEnabled(false);
        client.stop();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        baidumap.onDestroy();
    }

    class LocationEntity {
        BDLocation location;
        long time;
    }

    private class MyNotifyListener extends BDNotifyListener{
        public void onNotify(BDLocation mlocation, float distance){
            mVibrator.vibrate(1000);//振动提醒已到设定位置附近
            Toast.makeText(MainActivity.this, "震动提醒", Toast.LENGTH_SHORT).show();
        }
    }
}
