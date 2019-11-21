package com.zhouzhou.attendance.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
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
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.DotOptions;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.zhouzhou.attendance.R;
//import com.zhouzhou.attendance.service.LocationService;
import com.zhouzhou.attendance.service.utils;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.internal.Utils;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.baidumap)
    MapView baidumap;
    @BindView(R.id.design_navigation_view)
    NavigationView designNavigationView;
    @BindView(R.id.design_drawer_view)
    DrawerLayout designDrawerView;

    private static LocationClient mLocationClient = null;
    private static LocationClientOption defaultOption;
    private LinkedList<LocationEntity> locationList = new LinkedList<LocationEntity>(); // 存放历史定位结果的链表，最大存放当前结果的前5次定位结果
    private BaiduMap mBaiduMap;
    private double lat;//纬度
    private double lon;//经度
    private Marker marker;
    private boolean isFirst;
    private BDNotifyListener myListener = new MyNotifyLister();
    private  BDAbstractLocationListener locationListener = new MyLocationListener();
    private Vibrator mVibrator;
    private boolean isSelected = false;
    private OverlayOptions ooCircle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getPerssions();
        initToolBar();
        mVibrator =(Vibrator)getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);

        //获取地图控件引用
        mBaiduMap = baidumap.getMap();
        //声明LocationClient类
        mLocationClient = new LocationClient(getApplicationContext());
        //注册监听函数
        mLocationClient.registerNotify(myListener);
        mLocationClient.registerLocationListener(locationListener);
        //调用BDNotifyListener的setNotifyLocation方法，实现设置位置消息提醒。


        mBaiduMap.setMyLocationEnabled(true);
        //启动定位，SDK便会自动开启位置消息提醒的监听
        setDefaultOption();
        mLocationClient.start();
        //调用BDNotifyListener的removeNotifyEvent方法，实现取消位置监听
        //mLocationClient.removeNotifyEvent(myListener);
        mBaiduMap.setOnMapClickListener(listener);




    }

    public class MyLocationListener extends BDAbstractLocationListener{

        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            lat = bdLocation.getLatitude();
            lon = bdLocation.getLongitude();
            if (bdLocation == null || baidumap == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder().accuracy(bdLocation.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(bdLocation.getDirection()).latitude(bdLocation.getLatitude())
                    .longitude(bdLocation.getLongitude()).build();
            // 设置定位数据
            mBaiduMap.setMyLocationData(locData);
            //地图SDK处理
            if (!isFirst) {
                isFirst = true;
                LatLng ll = new LatLng(bdLocation.getLatitude(),
                        bdLocation.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(15.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
            if (bdLocation != null && (bdLocation.getLocType() == 161 || bdLocation.getLocType() == 66)) {
                Message locMsg = locHander.obtainMessage();
                Bundle data;
                data = Algorithm(bdLocation);
                if (data != null) {
                    data.putParcelable("loc", bdLocation);
                    locMsg.setData(data);
                    locHander.sendMessage(locMsg);
                }
            }
          notifyHandler.sendEmptyMessage(0);
            //myListener.SetNotifyLocation(lat,lon, 3000,mLocationClient.getLocOption().getCoorType());//4个参数代表要位置提醒的点的坐标，具体含义依次为：纬度，经度，距离范围，坐标系类型(gcj02,gps,bd09,bd09ll)

        }
    }
    /***
     * 平滑策略代码实现方法，主要通过对新定位和历史定位结果进行速度评分，
     * 来判断新定位结果的抖动幅度，如果超过经验值，则判定为过大抖动，进行平滑处理,若速度过快，
     * 则推测有可能是由于运动速度本身造成的，则不进行低速平滑处理 ╭(●｀∀´●)╯
     *
     * @param location
     * @return Bundle
     */
    private Bundle Algorithm(BDLocation location) {
        Bundle locData = new Bundle();
        double curSpeed = 0;
        if (locationList.isEmpty() || locationList.size() < 2) {
            LocationEntity temp = new LocationEntity();
            temp.location = location;
            temp.time = System.currentTimeMillis();
            locData.putInt("iscalculate", 0);
            locationList.add(temp);
        } else {
            if (locationList.size() > 5)
                locationList.removeFirst();
            double score = 0;
            for (int i = 0; i < locationList.size(); ++i) {
                LatLng lastPoint = new LatLng(locationList.get(i).location.getLatitude(),
                        locationList.get(i).location.getLongitude());
                LatLng curPoint = new LatLng(location.getLatitude(), location.getLongitude());
                double distance = DistanceUtil.getDistance(lastPoint, curPoint);
                curSpeed = distance / (System.currentTimeMillis() - locationList.get(i).time) /1000;
                score += curSpeed * utils.EARTH_WEIGHT[i];
            }
            if (score > 0.00000999 && score < 0.00005) { // 经验值,开发者可根据业务自行调整，也可以不使用这种算法
                location.setLongitude(
                        (locationList.get(locationList.size() - 1).location.getLongitude() + location.getLongitude())
                                / 2);
                location.setLatitude(
                        (locationList.get(locationList.size() - 1).location.getLatitude() + location.getLatitude())
                                / 2);
                locData.putInt("iscalculate", 1);
            } else {
                locData.putInt("iscalculate", 0);
            }
            LocationEntity newLocation = new LocationEntity();
            newLocation.location = location;
            newLocation.time = System.currentTimeMillis();
            locationList.add(newLocation);
        }
        return locData;
    }

    /***
     * 接收定位结果消息，并显示在地图上
     */
    private Handler locHander = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            try {
                BDLocation location = msg.getData().getParcelable("loc");
                int iscal = msg.getData().getInt("iscalculate");
                if (location != null) {
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    OverlayOptions dotOption = new DotOptions().center(point).color(0xAAA9A9A9).radius(50);
                    mBaiduMap.addOverlay(dotOption);
                    mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(point));
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }
    };
    private Handler notifyHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            //设置位置提醒，四个参数分别是：纬度、精度、半径、坐标类型
            myListener.SetNotifyLocation(lat,lon, 3000,mLocationClient.getLocOption().getCoorType());//4个参数代表要位置提醒的点的坐标，具体含义依次为：纬度，经度，距离范围，坐标系类型(gcj02,gps,bd09,bd09ll)

        }
    };
    /**
     *定义MyNotifyLister类，继承BDNotifyListener，实现位置监听的回调。
     */
    public class MyNotifyLister extends BDNotifyListener {
        public void onNotify(BDLocation mlocation, float distance){
            mVibrator.vibrate(1000);//振动提醒已到设定位置附近
            Toast.makeText(MainActivity.this, "已到签到范围", Toast.LENGTH_SHORT).show();
        }
    }
    BaiduMap.OnMapClickListener listener = new BaiduMap.OnMapClickListener() {
        /**
         * 地图单击事件回调函数
         *
         * @param point 点击的地理坐标
         */
        @Override
        public void onMapClick(LatLng point) {
            if (isSelected) {
                mBaiduMap.clear();
                if (marker != null) {
                    marker.remove();
                }
                //构建Marker图标
                BitmapDescriptor bitmap = BitmapDescriptorFactory.fromResource(R.drawable.icon_gcoding);
                //构建MarkerOption，用于在地图上添加Marker
                OverlayOptions option = new MarkerOptions().position(point).icon(bitmap);
                //在地图上添加Marker，并显示
                marker = (Marker) mBaiduMap.addOverlay(option);
                ooCircle = new CircleOptions().fillColor(0x000000FF)
                        .center(point).stroke(new Stroke(5, 0xAA000000))
                        .radius(300);//制定半径
                mBaiduMap.addOverlay(ooCircle);
            }
        }

        /**
         * 地图内 Poi 单击事件回调函数
         *
         * @param mapPoi 点击的 poi 信息
         */
        @Override
        public void onMapPoiClick(MapPoi mapPoi) {

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
                if (!isSelected){
                    isSelected = true;
                }else{
                    isSelected = false;
                }

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
            //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            defaultOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            //可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            defaultOption.setCoorType("gcj02");
            //可选，默认0，即仅定位一次，设置发起连续定位请求的间隔需要大于等于1000ms才是有效的
            defaultOption.setScanSpan(1000);
            //可选，设置是否需要地址信息，默认不需要
            defaultOption.setIsNeedAddress(true);
            //可选，设置是否需要地址描述
            defaultOption.setIsNeedLocationDescribe(true);
            //可选，设置是否需要设备方向结果
            defaultOption.setNeedDeviceDirect(false);
            //可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            defaultOption.setLocationNotify(true);
            //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            defaultOption.setIgnoreKillProcess(true);
            //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            defaultOption.setIsNeedLocationDescribe(true);
            //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            defaultOption.setIsNeedLocationPoiList(true);
            //可选，默认false，设置是否收集CRASH信息，默认收集
            defaultOption.SetIgnoreCacheException(false);
            //可选，默认false，设置是否开启Gps定位
            defaultOption.setOpenGps(true);
            //可选，默认false，设置定位时是否需要海拔信息，默认不需要，除基础定位版本都可用
            defaultOption.setIsNeedAltitude(false);
            //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者，该模式下开发者无需再关心定位间隔是多少，定位SDK本身发现位置变化就会及时回调给开发者
            defaultOption.setOpenAutoNotifyMode();
            //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
            defaultOption.setOpenAutoNotifyMode(3000,1, LocationClientOption.LOC_SENSITIVITY_HIGHT);
            //设置打开自动回调位置模式，该开关打开后，期间只要定位SDK检测到位置变化就会主动回调给开发者
        }
        mLocationClient.setLocOption(defaultOption);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tool_bar, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时必须调用mMapView. onResume ()
        baidumap.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时必须调用mMapView. onPause ()
        baidumap.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时必须调用mMapView.onDestroy()
        mLocationClient.unRegisterLocationListener(locationListener);
        baidumap.onDestroy();
        baidumap = null;
        // 关闭前台定位服务
        mLocationClient.disableLocInForeground(true);
        mLocationClient.stop();
        baidumap.onDestroy();
    }
    /**
     * 封装定位结果和时间的实体类
     *
     * @author baidu
     *
     */
    class LocationEntity {
        BDLocation location;
        long time;
    }

}
