package com.zhouzhou.attendance.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.zhouzhou.attendance.R;
import com.zhouzhou.attendance.service.LocationService;
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

    private LocationService locationService = null;
    private BaiduMap mBaiduMap;
    private LinkedList<LocationEntity> locationList = new LinkedList<LocationEntity>(); // 存放历史定位结果的链表，最大存放当前结果的前5次定位结果
    private double lat;//纬度
    private double lon;//经度
    private Marker marker;
    private boolean isFirst;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        locationService = new LocationService(getApplicationContext());
        locationService.registerListener(mListener);
        locationService.start();
        getPerssions();
        initToolBar();
        mBaiduMap = baidumap.getMap();
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18f));
        mBaiduMap.setMyLocationEnabled(true);


    }


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
                lat = location.getLatitude();
                lon = location.getLongitude();
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
                Toast.makeText(this, "设置2", Toast.LENGTH_SHORT).show();
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
    private void inputDialog(EditText editText){


        AlertDialog dialog = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_LIGHT)
                .setTitle("请输入：" + "")
                .setView(editText)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

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
        locationService.unRegisterListener(mListener);
        mBaiduMap.setMyLocationEnabled(false);
        locationService.stop();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        baidumap.onDestroy();
    }

    class LocationEntity {
        BDLocation location;
        long time;
    }
}
