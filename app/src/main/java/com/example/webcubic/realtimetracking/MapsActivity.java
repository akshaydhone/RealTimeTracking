package com.example.webcubic.realtimetracking;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.webcubic.realtimetracking.Remote.Common;
import com.example.webcubic.realtimetracking.Remote.IGoogleApi;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Url;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    SupportMapFragment  mapFragment;
    private List<LatLng> polyLineList;
    private Marker marker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition, currentPosition;
    private int index, next;
    private Button btnGo;
  //  private ValueAnimator valueAnimator;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions polylineOptions, blackPolylineoptions;
    private Polyline blackPolyline, greyPolyline;
    private IGoogleApi mService;
    private  LatLng myLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
   mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);




        polyLineList = new ArrayList<>();
        btnGo = (Button)findViewById(R.id.btnSearch);
        edtPlace = (EditText)findViewById(R.id.edtplace);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destination = edtPlace.getText().toString();
                destination = destination.replace("","+"); //replace space whith + for fetch data
               mapFragment.getMapAsync(MapsActivity.this);
            }
        });

        mService = Common.getGoogleAPI();


    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);


        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(19.012791, 72.824499);
        mMap.addMarker(new MarkerOptions().position(sydney).title("My Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                new CameraPosition.Builder()
                        .target(googleMap.getCameraPosition().target)
                        .zoom(17)
                        .bearing(30)
                        .tilt(45)
                        .build()

        ));
        String requestUrl =null;
        try {
            requestUrl = "https://maps.googleapis.com/maps/api/directions/json?"+
                    "mode=driving&"+
                    "transit_routing_preference=less_driving&"+
                    "origin="+sydney.latitude+","+sydney.longitude+"&"+
                    "destination="+destination+"&"+
                    "key="+getResources().getString(R.string.google_directions_key);
            Log.d("URL",requestUrl);
            mService.getDataFromGoogleApi(requestUrl)
            .enqueue(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {


                    try{


                        JSONObject jsonObject = new JSONObject(response.body().toString());
                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                        for(int i =0; i<jsonArray.length();i++)
                        {
                            JSONObject route = jsonArray.getJSONObject(i);
                            JSONObject poly = route.getJSONObject("overview_polyline");
                            String polyline = poly.getString("points");
                            polyLineList = decodePoly(polyline);



                        }

                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        for(LatLng latLng:polyLineList)
                            builder.include(latLng);
                        LatLngBounds bounds = builder.build();
                        CameraUpdate mCameraUpdate  = CameraUpdateFactory.newLatLngBounds(bounds, 2);
                        mMap.animateCamera(mCameraUpdate);

                        polylineOptions = new PolylineOptions();
                        polylineOptions.color(Color.GRAY);
                        polylineOptions.width(5);
                        polylineOptions.startCap(new SquareCap());
                        polylineOptions.endCap(new SquareCap());
                        polylineOptions.jointType(JointType.ROUND);
                        polylineOptions.addAll(polyLineList);
                        greyPolyline = mMap.addPolyline(polylineOptions);




                        blackPolylineoptions = new PolylineOptions();
                        blackPolylineoptions.color(Color.BLACK);
                        blackPolylineoptions.width(5);
                        blackPolylineoptions.startCap(new SquareCap());
                        blackPolylineoptions.endCap(new SquareCap());
                        blackPolylineoptions.jointType(JointType.ROUND);
                        blackPolylineoptions.addAll(polyLineList);
                        blackPolyline = mMap.addPolyline(blackPolylineoptions);

                        mMap.addMarker(new MarkerOptions()
                                .position(polyLineList.get(polyLineList.size()-1)));

                    final ValueAnimator polyLineAnimator = ValueAnimator.ofInt(0,100);
                        polyLineAnimator.setDuration(2000);
                        polyLineAnimator.setInterpolator(new LinearInterpolator());
                        polyLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                List<LatLng> points = greyPolyline.getPoints();
                                int percentValue = (int) polyLineAnimator.getAnimatedValue();
                                int size = points.size();
                                int newPoints = (int) (size*(percentValue/100.0f));
                                List<LatLng> p = points.subList(0, newPoints);
                                blackPolyline.setPoints(p);

                            }
                        });
                        polyLineAnimator.start();
                      marker = mMap.addMarker(new MarkerOptions().position(currentPosition)
                                .flat(true)
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.bike)));

                        handler = new Handler();
                        index =-1;
                        next=1;
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(index<polyLineList.size()-1)
                                {
                                    index++;
                                    next = index  + 1;
                                }
                                if(index < polyLineList.size()-1)
                                {
                                    startPosition = polyLineList.get(index);
                                    endPosition = polyLineList.get(next);

                                }




                                final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0,1);
                                valueAnimator.setDuration(3000);
                                valueAnimator.setInterpolator(new LinearInterpolator());
                                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                    @Override
                                    public void onAnimationUpdate(ValueAnimator animation) {
                                        v = valueAnimator.getAnimatedFraction();
                                        lng = v*endPosition.longitude+(1-v)*startPosition.longitude;
                                        lat = v*endPosition.latitude+(1-v)*startPosition.latitude;
                                        LatLng newPos = new LatLng(lat,lng);
                                        marker.setPosition(newPos);
                                        marker.setAnchor(0.5f,0.5f);
                                        marker.setRotation(getBearing(startPosition, newPos));
                                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                                        .target(newPos)
                                        .zoom(15.5f)
                                        .build()));

                                    }
                                });
                              valueAnimator.start();
                                handler.postDelayed(this, 3000);

                            }
                        },3000);

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Toast.makeText(MapsActivity.this, ""+t.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });//print url for debug
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private float getBearing(LatLng startPosition, LatLng newPos) {

        double lat = Math.abs(startPosition.latitude-newPos.latitude);
        double lng = Math.abs(startPosition.longitude-newPos.longitude);
        if(startPosition.latitude<newPos.latitude&&startPosition.longitude<newPos.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat)));
        else if(startPosition.latitude >= newPos.latitude&&startPosition.longitude<newPos.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+90);
        else if(startPosition.latitude >= newPos.latitude&&startPosition.longitude >= newPos.longitude)
            return (float)(Math.toDegrees(Math.atan(lng/lat))+180);
        else if(startPosition.latitude<newPos.latitude&&startPosition.longitude >= newPos.longitude)
            return (float)((90-Math.toDegrees(Math.atan(lng/lat)))+270);
        return -1;

    }


    private List<LatLng> decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
    }

