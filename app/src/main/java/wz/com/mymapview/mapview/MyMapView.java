package wz.com.mymapview.mapview;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;

import java.util.ArrayList;
import java.util.List;

import wz.com.mymapview.R;
import wz.com.mymapview.utils.SubscribeReturnUtils;

/**
 * Created by Administrator on 2017/4/23.
 */

public class MyMapView extends RelativeLayout implements LocationSource, AMapLocationListener, AMap.OnInfoWindowClickListener, AMap.InfoWindowAdapter, AMap.OnMapClickListener, RouteSearch.OnRouteSearchListener {

    private MapView bikeMap;
    private AMap aMap;
    private UiSettings mUiSettings;
    OnLocationChangedListener mListener;
    AMapLocationClient mlocationClient;
    AMapLocationClientOption mLocationOption;
    Marker marketLocat;
    int mapX , mapY;
    List<LatLng> list;
    private List<Marker> markerList = new ArrayList<>(); //当前所有单车的点
    Marker marketClick;
    private LatLonPoint start;
    private LatLonPoint end;

    private RouteSearch mRouteSearch;
    private WalkRouteResult mWalkRouteResult;
    private GeocodeSearch geocoderSearch;
    private final int ROUTE_TYPE_WALK = 3;
    WalkRouteOverlay walkRouteOverlay; // 画线


    public MyMapView(Context context) {
        super(context);
        initView();
    }

    public MyMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MyMapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public void initView(){
        LayoutInflater.from(getContext()).inflate(R.layout.map_view,this,true);
        bikeMap = (MapView) findViewById(R.id.my_map_view);
    }

    public void initMap(Bundle bundle){
        bikeMap.onCreate(bundle);
        init();
    }

    private void init() {

        initLbsList();

        bikeMap.post(new Runnable() {
            @Override
            public void run() {
                mapX =  bikeMap.getMeasuredWidth();
                mapY =  bikeMap.getMeasuredHeight();
            }
        });

        if (aMap == null) {
            aMap = bikeMap.getMap();
            aMap.moveCamera(CameraUpdateFactory.zoomTo(15));
            mUiSettings = aMap.getUiSettings();
            aMap.setOnMarkerClickListener(markerClickListener);
            mRouteSearch = new RouteSearch(getContext());
            mRouteSearch.setRouteSearchListener(this);
            setUpMap();
        }
    }

    /**
     * 初始化点的集合
     */
    private void initLbsList() {
        list = new ArrayList<>();
        list.add(new LatLng(27.110141,115.02815));
        list.add(new LatLng(27.113369,115.023622));
        list.add(new LatLng(27.111631,115.020028));
    }

    /**
     * 设置map属性
     */
    private void setUpMap() {

        aMap.setOnInfoWindowClickListener(this);
        aMap.setInfoWindowAdapter(this);
        aMap.setOnMapClickListener(this);
        aMap.setLocationSource(this);// 设置定位监听
        aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
        aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
        aMap.setMyLocationType(AMap.LOCATION_TYPE_MAP_FOLLOW);
        setupLocationStyle();
    }

    private void setupLocationStyle() {
// 自定义系统定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        // 自定义定位蓝点图标
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.
                fromResource(R.mipmap.start_location));
        // 自定义精度范围的圆形边框颜色
        myLocationStyle.strokeColor(Color.TRANSPARENT);
        //自定义精度范围的圆形边框宽度
        myLocationStyle.strokeWidth(0);
        // 设置圆形的填充颜色
//        myLocationStyle.radiusFillColor(FILL_COLOR);
        // 将自定义的 myLocationStyle 对象添加到地图上
        aMap.setMyLocationStyle(myLocationStyle);
        mUiSettings.setMyLocationButtonEnabled(true); // 是否显示默认的定位按钮
        mUiSettings.setTiltGesturesEnabled(false);// 设置地图是否可以倾斜
        mUiSettings.setScaleControlsEnabled(false);// 设置地图默认的比例尺是否显示
        mUiSettings.setZoomControlsEnabled(false);
    }

    @Override
    public void activate(OnLocationChangedListener listener) {
        mListener = listener;
        if (mlocationClient == null) {
            //初始化定位
            mlocationClient = new AMapLocationClient(getContext());
            //初始化定位参数
            mLocationOption = new AMapLocationClientOption();
            //设置定位回调监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();//启动定位
        }
    }

    @Override
    public void deactivate() {
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }

    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (mListener != null&&amapLocation != null) {
            if (amapLocation != null
                    &&amapLocation.getErrorCode() == 0)   {;

                start = new LatLonPoint(amapLocation.getLatitude(),amapLocation.getLongitude());
                mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                addMylocationMarker(amapLocation.getLatitude(),amapLocation.getLongitude());
                drawNearMarker();
                Toast.makeText(getContext(),"定位成功",Toast.LENGTH_SHORT).show();
            } else {
                String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
                Log.e("AmapErr",errText);
            }
        }
    }

    /**
     * 定位成功画出附近点
     * 实际中应该是根据经纬度切换加载附近的点
     * 所以添加之前先清理一遍已有的点
     */
    private void drawNearMarker() {
        if(markerList.size()>0){
            for(int i = 0;i < markerList.size();i ++){
                Marker marker = markerList.get(i);
                marker.destroy();
            }
            markerList.clear();
        }

        for (int i = 0; i < list.size(); i++) {
            LatLng latLng = new LatLng(list.get(i).latitude,
                    list.get(i).longitude);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                    .decodeResource(getResources(), R.mipmap.bike)));
            final Marker marker = aMap.addMarker(markerOptions);
            marker.setPosition(latLng);
            marker.setDraggable(false);
            marker.setObject("bike");
            marker.setSnippet("sssss");
            marker.setInfoWindowEnable(true);
            markerList.add(marker);
        }

    }

    /**
     * 根据定位画出当前位置的图标
     * @param latitude
     * @param longitude
     */
    private void addMylocationMarker(double latitude, double longitude) {
        LatLng latlng = new LatLng(latitude,longitude);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(getResources(), R.mipmap.gps)));
        marketLocat = aMap.addMarker(markerOptions);
        marketLocat.setDraggable(false);
        marketLocat.setPositionByPixels(mapX/2,mapY/2);
    }

    public void onDestroyMap(){
        bikeMap.onDestroy();
    }

    public void onResume(){
        bikeMap.onResume();
    }

    public void onPause(){
        bikeMap.onPause();
    }

    public void onSave(Bundle outState){
        bikeMap.onSaveInstanceState(outState);
    }

    // 定义 Marker 点击事件监听
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        // marker 对象被点击时回调的接口
        // 返回 true 则表示接口已响应事件，否则返回false
        @Override
        public boolean onMarkerClick(Marker marker) {
            Toast.makeText(getContext(),"点击了"+marker.getPosition().latitude+marker.getPosition().longitude,Toast.LENGTH_SHORT).show();
            marker.showInfoWindow();
            marketClick = marker;
            end = new LatLonPoint(marker.getPosition().latitude,marker.getPosition().longitude);
            searchRouteResult(ROUTE_TYPE_WALK, RouteSearch.WALK_DEFAULT, start, end);
            return true;
        }
    };

    /**
     * 开始路径规划
     * @param route_type_walk
     * @param walkDefault
     * @param start
     * @param end
     */
    private void searchRouteResult(int route_type_walk, int walkDefault, LatLonPoint start, LatLonPoint end) {
        RouteSearch.FromAndTo fromTo = new RouteSearch.FromAndTo(start,end);
        if (route_type_walk == ROUTE_TYPE_WALK) {// 步行路径规划
            RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromTo, walkDefault);
            mRouteSearch.calculateWalkRouteAsyn(query);// 异步路径规划步行模式查询
        }
    }

    View infoView = null;

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public View getInfoWindow(Marker marker) {
        if (infoView == null) {
            infoView = LayoutInflater.from(getContext()).inflate(R.layout.my_info_window, null);
            TextView title = (TextView) infoView.findViewById(R.id.info_tv_tips);
            title.setText(SubscribeReturnUtils.ClickReturnTips());
        }
        return infoView;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        infoView = null;
        marketClick.hideInfoWindow();
        walkRouteOverlay.removeFromMap();
    }

    @Override
    public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

    }

    @Override
    public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

    }

    @Override
    public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int code) {
        if (code == AMapException.CODE_AMAP_SUCCESS) {
            if (walkRouteResult != null && walkRouteResult.getPaths() != null) {
                if (walkRouteResult.getPaths().size() > 0) {
                    mWalkRouteResult = walkRouteResult;
                    final WalkPath walkPath = mWalkRouteResult.getPaths()
                            .get(0);
                    walkRouteOverlay = new WalkRouteOverlay(getContext(), aMap, walkPath,
                            mWalkRouteResult.getStartPos(),
                            mWalkRouteResult.getTargetPos());

                    walkRouteOverlay.removeFromMap();
                    walkRouteOverlay.addToMap();
                    walkRouteOverlay.zoomToSpan();
                    walkRouteOverlay.setNodeIconVisibility(false);
                } else if (walkRouteResult != null && walkRouteResult.getPaths() == null) {

                }
            } else {

            }
        }
    }

    @Override
    public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

    }
}
