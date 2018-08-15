package com.example.randomaptesting;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;

/**
 * Created by chengchinlim on 5/29/18.
 */

public class MainActivity extends FragmentActivity {

    EditText keyWordInput;
    Switch metricsSwitch;
    SeekBar radiusBar;
    TextView radiusTxt;
    ToggleButton cheap;
    ToggleButton normal;
    ToggleButton expensive;
    ToggleButton extreme;
    RatingBar ratingBar;
    TextView ratingTxt;
    String userKeyword;
    double userRadius;
    int userPrice;
    double userRating;
    boolean includePrice = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!isNetworkConnected()) {
            showDialog();
        }
        if (!checkLocationAccessPermitted())  {
            requestLocationAccessPermission();
        }
        listenerForRadiusBar();
        cheap = findViewById(R.id.cheap);
        normal = findViewById(R.id.normal);
        expensive = findViewById(R.id.expensive);
        extreme = findViewById(R.id.extreme);
        metricsSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (metricsSwitch.isChecked()) {
                    radiusBar.setMax(160);
                    radiusBar.setProgress(50);
                    radiusTxt.setText("5.0");
                } else {
                    radiusBar.setMax(100);
                    radiusBar.setProgress(30);
                    radiusTxt.setText("3.0");
                }
            }
        });
        listenerForRatingBar();
        keyWordInput = findViewById(R.id.searchKeyTxt);
//        ArrayList<String> keywords = getKeywordsForAutocomplete();
////        for (String keyword: keywords) { // for debug purpose
////            System.out.println("Keyword: " +keyword);
////        }
//        ArrayAdapter adapter = new
//                ArrayAdapter(this,android.R.layout.simple_list_item_1, keywords);
//        keyWordInput.setAdapter(adapter);
//        keyWordInput.setThreshold(1);
    }

    @SuppressWarnings("MissingPermission")
    private void mainFunc() {
        if (!returnInputsForURL()) {
            Toast.makeText(getApplicationContext(), "Please key in a keyword",Toast.LENGTH_SHORT).show();
            return;
        }
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.getLastLocation()
            .addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful() && task.getResult() != null) {
                        Location mLastLocation = task.getResult();
                        final double myLatitude = mLastLocation.getLatitude();
                        final double myLongitude = mLastLocation.getLongitude();
//                        System.out.println("Last known Location Latitude is " +
//                                mLastLocation.getLatitude()); // debugPrint purpose
//                        System.out.println("Last known Location Longitude is " +
//                                mLastLocation.getLongitude()); // debugPrint purpose
                        String completeUrl = constructNearbySearchUrl(myLatitude, myLongitude, "restaurant", userRadius * 1.3, userKeyword);
                        System.out.println(completeUrl); // debugPrint purpose
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, completeUrl,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    ArrayList<Destination> destinationList = new ArrayList<>();
                                    try {
                                        JSONObject obj = new JSONObject(response);
                                        JSONArray results = obj.getJSONArray("results");
                                        for (int i = 0; i < results.length(); i++) {
                                            String latitude = results.getJSONObject(i).getJSONObject("geometry")
                                                    .getJSONObject("location").getString("lat");
                                            String longitude = results.getJSONObject(i).getJSONObject("geometry")
                                                    .getJSONObject("location").getString("lng");
                                            double placeLatitude = Double.parseDouble(latitude);
                                            double placeLongitude = Double.parseDouble(longitude);
//                                            System.out.println("placeLatitude: " + placeLatitude); // for debug purpose
//                                            System.out.println("placeLongitude: " + placeLongitude); // for debug purpose
                                            double distance = calculateDistance(myLatitude, myLongitude, placeLatitude, placeLongitude) * 1000;
//                                            System.out.println("Distance:" + distance); // for debug purpose
                                            String name = results.getJSONObject(i).getString("name");
                                            String placeId = results.getJSONObject(i).getString("place_id");
                                            String address = results.getJSONObject(i).getString("vicinity");
                                            int price = 0;
                                            if (results.getJSONObject(i).has("price_level")) {
                                                includePrice = true;
                                                String price_level = results.getJSONObject(i).getString("price_level");
                                                price = Integer.parseInt(price_level);
                                            }
                                            double rating = 0;
                                            if (results.getJSONObject(i).has("rating")) {
                                                String r = results.getJSONObject(i).getString("rating");
                                                rating = Double.parseDouble(r);
                                            }
                                            Destination d = new Destination(name, address, placeId, distance);
                                            d.setPrice(price);
                                            d.setRating(rating);
                                            destinationList.add(d);
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    System.out.println("Destinations:"); // for debug purpose
                                    for (int i = 0; i < destinationList.size(); i++) {
                                        System.out.println(i+1 + ". " + destinationList.get(i));
                                    }

                                    ArrayList<Destination> matchUserReqList = new ArrayList<>();
                                    ArrayList<Destination> suggestions = new ArrayList<>();

                                    for (Destination d: destinationList) {
                                        if (matchUserReq(d, includePrice)) {
                                            matchUserReqList.add(d);
                                        } else {
                                            suggestions.add(d);
                                        }
                                    }
                                    System.out.println("MatchUserReqList: "); // for debug purpose
                                    for (int i = 0; i < matchUserReqList.size(); i++) {
                                        System.out.println(Integer.toString(i+1) + ". " + matchUserReqList.get(i));
                                    }
                                    System.out.println("Suggestions: "); // for debug purpose
                                    for (int i = 0; i < suggestions.size(); i++) {
                                        System.out.println(Integer.toString(i+1) + ". " + suggestions.get(i));
                                    }
                                    Intent showResultActivity =  new Intent(MainActivity.this, ShowResult.class);
                                    showResultActivity.putExtra("matchUserReqList", matchUserReqList);
                                    showResultActivity.putExtra("suggestions", suggestions);
                                    startActivity(showResultActivity);
                                }
                            }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println("Volley Error");
                            }
                        }
                        );
                        queue.add(stringRequest);

                    } else {
                        System.out.println("No Last known location found. Try current location..!");
                    }
                }
            });
    }

    private boolean matchUserReq(Destination d, boolean includePrice) {
        if (d.getDistance() > userRadius) {
            return false;
        }
        if (d.getRating() != 0) {
            if (d.getRating() < userRating) {
                return false;
            }
        } else {
            return false;
        }
        if (includePrice == true) {
            if (d.getPrice() > userPrice) {
                return false;
            }
        }

        return true;
    }

    private double calculateDistance(double myLatitude, double myLongitude, double placeLatitude, double placeLongitude) {
        double earthRadius = 6371;
        double latDiff = degreeToRadians(placeLatitude - myLatitude);
        double longDiff = degreeToRadians(placeLongitude - myLongitude);
        double a = Math.pow(Math.sin(latDiff/2), 2)
                + Math.cos(degreeToRadians(myLatitude)) * Math.cos(degreeToRadians(placeLatitude))
                * Math.pow(Math.sin(longDiff/2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = earthRadius * c;
        return d;
    }

    private double degreeToRadians(double degree) {
        return degree * (Math.PI/180);
    }

    private boolean returnInputsForURL() {
        userKeyword = keyWordInput.getText().toString();
        if (userKeyword.length() == 0) {
            return false;
        }
        userKeyword = userKeyword.replace(' ', '+');
        if (metricsSwitch.isChecked()) {
            userRadius *= 1000;
        } else {
            userRadius *= 1600;
        }
//        System.out.println("userRadius: " + userRadius); // debug purpose
        if (cheap.isChecked()) {
            userPrice = 1;
        } else if (normal.isChecked()) {
            userPrice = 2;
        } else if (expensive.isChecked()) {
            userPrice = 3;
        } else {
            userPrice = 4;
        }
//        System.out.println("userPrice: " + userPrice); // for debug purpose
        userRating = ratingBar.getRating();
        return true;
    }

    private String constructNearbySearchUrl(double latitude, double longitude,
                                            String type, double radius, String key) {
        String basicUrl ="https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
        String latAndLong = "location=" + Double.toString(latitude) + "," + Double.toString(longitude);
        String radiusFrCurrentLocation = "&radius=" + radius;
        String placeType = "&type=" + type;
        String keyword = "&keyword=" + key + "&opennow=1";
        String apiKey = "&key=AIzaSyDpKpQ2S8lvUK7xfHGgSoJXy0HG9tFU-7s";
        String completeUrl = basicUrl + latAndLong + radiusFrCurrentLocation
                + placeType + keyword + apiKey;
        return completeUrl;
    }

    /*
    * The two functions below are used to check if there is Wifi or data connection
    * If not, prompt user to open Wifi
    * */

    private boolean isNetworkConnected() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(getApplicationContext().CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    private void showDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Connect to wifi or quit")
                .setCancelable(false)
                .setPositiveButton("Connect to WIFI", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    }
                })
                .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishActivity(0);
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /*
    * The below two functions are used to check if the user has enabled location services
    * and prompt them to open it if it is not enabled
    * */


    private boolean checkLocationAccessPermitted() {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void requestLocationAccessPermission() {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        System.out.println("All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        System.out.println("Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result
                            // in onActivityResult().
                            status.startResolutionForResult(MainActivity.this, 1);
                        } catch (IntentSender.SendIntentException e) {
                            System.out.println("PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        System.out.println("Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    public void onSubmitClicked(View v) {
        mainFunc();
    }

    public void onCheapClicked(View v) {
        cheap.setChecked(true);
        normal.setChecked(false);
        expensive.setChecked(false);
        extreme.setChecked(false);
    }

    public void onNormalClicked(View v) {
        cheap.setChecked(false);
        normal.setChecked(true);
        expensive.setChecked(false);
        extreme.setChecked(false);
    }

    public void onExpensiveClicked(View v) {
        cheap.setChecked(false);
        normal.setChecked(false);
        expensive.setChecked(true);
        extreme.setChecked(false);
    }

    public void onExtremeClicked(View v) {
        cheap.setChecked(false);
        normal.setChecked(false);
        expensive.setChecked(false);
        extreme.setChecked(true);
    }

    /*
     * The code below are UI elements, it would be changed to the newest design accordingly
     * So it is not very important, but the way those bars work would be applied later
     *
     * */

    public void listenerForRatingBar() {
        ratingBar = findViewById(R.id.ratingBar);
        ratingTxt = findViewById(R.id.ratingTxt);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                ratingTxt.setText(Float.toString(rating));
                if (rating < 1.0f) {
                    ratingBar.setRating(1.0f);
                }
            }
        });
    }

    public void listenerForRadiusBar() {
        metricsSwitch = findViewById(R.id.metricsSwitch);
        radiusBar = findViewById(R.id.radiusBar);
        radiusTxt = findViewById(R.id.radiusTxt);
        radiusBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                userRadius = ((double)progress/10);
                radiusTxt.setText(Double.toString(userRadius));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (metricsSwitch.isChecked()) {
                    radiusBar.setMax(160);
                    radiusBar.setProgress(50);
                } else {
                    radiusBar.setMax(100);
                    radiusBar.setProgress(30);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (userRadius < 1) {
                    Toast.makeText(getApplicationContext(), "Minimum is 1, radius will be set to 1", Toast.LENGTH_SHORT).show();
                    userRadius = 1;
                }
            }
        });
    }

    /*
     * The function below is used for auto complete loading from text file
     * But we changed plans so it is not used, but it is useful for future
     * */

//    private ArrayList<String> getKeywordsForAutocomplete() {
//        System.out.println("Inside getKeywordsForAutoComplete function"); // for debug purpose
//        ArrayList<String> keywords = new ArrayList<>();
//        InputStream is = getResources().openRawResource(R.raw.keywords);
//        try {
//            BufferedReader br = new BufferedReader(new InputStreamReader(is));
//            String line;
//            while ((line = br.readLine()) != null) {
//                keywords.add(line);
//            }
//            br.close();
//        }
//        catch (IOException e) {
//            System.out.println("Failed to load keywords from text file");
//        }
//        return keywords;
//    }
}
