package com.example.navigate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationEngineListener, PermissionsListener, MapboxMap.OnMapClickListener {

   private MapView mapView;
   private MapboxMap mapbox;
    public LocationEngine locationEngine;
    private Button startButton;
   public Location originLocation;
   private Point originPosition;
   private Point destinationPosition;
   private Marker destinationMarker;
   private NavigationMapRoute navigationMapRoute;
   private  static final String TAG=" MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this,getString(R.string.access_token));
        setContentView(R.layout.activity_main);

        startButton=findViewById(R.id.startNavigationButton);
        mapView=findViewById(R.id.map_View);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Start Navigation
                try {
                    NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                            .origin(originPosition)
                            .destination(destinationPosition)
                            .shouldSimulateRoute(true)
                            .build();
                    NavigationLauncher.startNavigation(MainActivity.this, options);
                }catch (Exception e){
                    Log.e("","Error"+e);
                }
            }
        });
    }
    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        mapbox=mapboxMap;
        mapbox.addOnMapClickListener(this);
        enableLocation();

    }

    private void enableLocation(){

        if(PermissionsManager.areLocationPermissionsGranted(this)){
            initializeLocationEngine();
            initializeLocationLayer();

        }else{
            PermissionsManager permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }

    }

    @SuppressWarnings("MissingPermission")
    private  void initializeLocationEngine(){

       locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation=locationEngine.getLastLocation();
        if(lastLocation!=null){
            originLocation=lastLocation;
            setCameraPosition(lastLocation);
        }else{
            locationEngine.addLocationEngineListener(this);
        }

    }
       @SuppressWarnings("MissingPermission")
    private  void initializeLocationLayer(){

           LocationLayerPlugin locationLayerPlugin = new LocationLayerPlugin(mapView, mapbox, locationEngine);
        locationLayerPlugin.setLocationLayerEnabled(true);
        locationLayerPlugin.setCameraMode(CameraMode.TRACKING);
        locationLayerPlugin.setRenderMode(RenderMode.NORMAL);

    }

    private void setCameraPosition(Location location){
      mapbox.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),location.getLongitude()),21.0));
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {

        if(granted){
            enableLocation();
        }

    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {

        locationEngine.removeLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {

        if(location!=null){
            originLocation=location;
            setCameraPosition(location);
        }
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {

        if(destinationMarker!=null){
            destinationMarker.remove();
        }
        destinationMarker=mapbox.addMarker(new MarkerOptions().position(point));
        destinationPosition=Point.fromLngLat(point.getLongitude(),point.getLatitude());
        originPosition=Point.fromLngLat(originLocation.getLongitude(),originLocation.getLatitude());
        getRoute(originPosition,destinationPosition);
        startButton.setEnabled(true);
        startButton.setBackgroundResource(R.color.colorPrimary);

    }

    public void getRoute(Point initialPoint,Point destinationPoint){

        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(initialPoint)
                .destination(destinationPoint)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        if(response.body()==null){
                            Log.e(TAG,"No routes found");
                        }else if(response.body().routes().size()==0){
                            Log.e(TAG,"No routes found");
                        }

                        DirectionsRoute directionsRoute=response.body().routes().get(0);
                        if(navigationMapRoute!=null){
                            navigationMapRoute.removeRoute();
                        }else{
                            navigationMapRoute=new NavigationMapRoute(null,mapView,mapbox);
                        }
                        navigationMapRoute.addRoute(directionsRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable t) {

                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected  void onStart(){
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
        if(locationEngine!=null){
            locationEngine.removeLocationUpdates();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState, @NonNull PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
        if(locationEngine!=null){
            locationEngine.deactivate();
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        mapView.onDestroy();
    }



}
