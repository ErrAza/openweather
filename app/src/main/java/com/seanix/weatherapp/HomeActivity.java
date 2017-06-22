package com.seanix.weatherapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private String key = "&appid=1414de1d5eb1b1e64ede62c53e295524";
    private String baseUrl = "http://api.openweathermap.org/data/2.5/forecast";

    private City currentCity;
    private Boolean fabMenuExpanded;

    ListView listView;
    FloatingActionButton fabMain;
    FloatingActionButton fabCity;
    FloatingActionButton fabLocation;

    AlertDialog cityInputDialog;

    SimpleAdapter adapter;
    List<Map<String, String>> mWeatherDataList;
    List<Weather> mWeatherObjectList;
    HomeActivity.DownloadTask task;

    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fabMain = (FloatingActionButton) findViewById(R.id.floatingActionButtonMain);
        fabCity = (FloatingActionButton) findViewById(R.id.floatingActionButtonCity);
        fabLocation = (FloatingActionButton) findViewById(R.id.floatingActionButtonLocation);

        fabMenuExpanded = false;
        fabCity.setVisibility(View.INVISIBLE);
        fabLocation.setVisibility(View.INVISIBLE);

        mWeatherObjectList = new ArrayList<>();
        mWeatherDataList = new ArrayList<>();

        listView = (ListView) findViewById(R.id.listView);

        adapter = new SimpleAdapter(HomeActivity.this, mWeatherDataList, R.layout.list_item_override, new String[]{"Day", "Description", "Temperature", "Humidity", "Windspeed", "Icon"}, new int[]{R.id.day, R.id.description, R.id.temperature, R.id.humidity, R.id.windspeed, R.id.imageView});
        listView.setAdapter(adapter);

        GetCurrentLocation();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.nav_visit)
        {
            Uri uri = Uri.parse("http://www.openweathermap.org");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
        else if (id == R.id.nav_about)
        {
            Toast.makeText(this, "A Simple Creation of Sean McCann", Toast.LENGTH_SHORT).show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void OnFabMainClicked(View view) {
        if (fabMenuExpanded) {
            CollapseFabMenu();
        } else {
            ExpandFabMenu();
        }
    }

    public void ExpandFabMenu() {
        fabMain.setClickable(false);
        fabCity.setVisibility(View.VISIBLE);
        fabLocation.setVisibility(View.VISIBLE);

        fabMain.animate().rotationBy(180f).setDuration(500).start();

        fabCity.animate().translationYBy(-150f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                fabMain.setClickable(true);
                fabMenuExpanded = true;
            }
        }).start();

        fabLocation.animate().translationYBy(-300f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                fabMain.setClickable(true);
                fabMenuExpanded = true;
            }
        }).start();
    }

    public void CollapseFabMenu() {
        fabMain.setClickable(false);
        fabMain.animate().rotationBy(180f).setDuration(500).start();

        fabCity.animate().translationYBy(150f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                fabCity.setVisibility(View.INVISIBLE);
                fabMain.setClickable(true);
                fabMenuExpanded = false;
            }
        }).start();

        fabLocation.animate().translationYBy(300f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                fabLocation.setVisibility(View.INVISIBLE);
                fabMain.setClickable(true);
                fabMenuExpanded = false;
            }
        }).start();
    }

    public void OnFabCityClicked(View view) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        cityInputDialog = new AlertDialog.Builder(HomeActivity.this)
                .setTitle("View Weather for City")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!input.getText().toString().isEmpty()) {
                            City city = new City();
                            city.mName = input.getText().toString();

                            GetWeather(city);

                            CollapseFabMenu();
                        } else {
                            dialog.cancel();

                            CollapseFabMenu();

                            Toast.makeText(HomeActivity.this, "No City Provided. Please Enter a City.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        CollapseFabMenu();
                    }
                })
                .show();
    }


    public void OnFabLocationClicked(View view) {
        GetCurrentLocation();

        CollapseFabMenu();
    }

    public void DiscardListView()
    {
        listView.animate().setDuration(250).translationYBy(-1600f).withEndAction(new Runnable() {
            @Override
            public void run() {
                listView.setVisibility(View.INVISIBLE);
            }
        }).start();
    }

    public void FocusListView()
    {
        listView.setVisibility(View.VISIBLE);
        listView.animate().setDuration(250).translationYBy(1600f).start();
    }

    public void GetCurrentLocation() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        if (Build.VERSION.SDK_INT < 23) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                if (lastKnownLocation != null) {
                    GetWeather(lastKnownLocation);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    GetWeather(lastKnownLocation);
                }
            }
        }
    }

    public void PopulateListWithWeatherData() {

        for (int i = 0; i < mWeatherObjectList.size(); i++) {
            Map<String, String> weatherInfo = new HashMap<>();

            weatherInfo.put("Day", mWeatherObjectList.get(i).mDay);
            weatherInfo.put("Description", mWeatherObjectList.get(i).mDescription);
            weatherInfo.put("Temperature", "Temperature: " + String.valueOf(mWeatherObjectList.get(i).mTemperature) + "°");
            weatherInfo.put("Humidity", "Humidity: " + String.valueOf(mWeatherObjectList.get(i).mHumidity) + "%");
            weatherInfo.put("Windspeed", "Wind Speed: " + String.valueOf(mWeatherObjectList.get(i).mWindSpeed) + " km/h");
            weatherInfo.put("Icon", mWeatherObjectList.get(i).mIcon);

            mWeatherDataList.add(weatherInfo);
        }

        adapter.notifyDataSetChanged();

        FocusListView();
    }

    public void UpdateWeatherIcons() {

        //TODO make a non duct tape method to figure out icons

        for (Weather weatherItem : mWeatherObjectList) {
            if (weatherItem.mId <= 211)
                weatherItem.mIcon = Integer.toString(R.drawable.a211);

            if (weatherItem.mId >= 500 && weatherItem.mId < 510)
                weatherItem.mIcon = Integer.toString(R.drawable.a500);

            if (weatherItem.mId >= 511 && weatherItem.mId <= 531)
                weatherItem.mIcon = Integer.toString(R.drawable.a521);

            if (weatherItem.mId >= 600 && weatherItem.mId <= 699)
                weatherItem.mIcon = Integer.toString(R.drawable.a600);

            if (weatherItem.mId >= 700 && weatherItem.mId <= 799)
                weatherItem.mIcon = Integer.toString(R.drawable.a700);

            if (weatherItem.mId == 800)
                weatherItem.mIcon = Integer.toString(R.drawable.a800);

            if (weatherItem.mId == 801)
                weatherItem.mIcon = Integer.toString(R.drawable.a801);

            if (weatherItem.mId == 802)
                weatherItem.mIcon = Integer.toString(R.drawable.a802);

            if (weatherItem.mId == 803)
                weatherItem.mIcon = Integer.toString(R.drawable.a802);

            if (weatherItem.mId == 804)
                weatherItem.mIcon = Integer.toString(R.drawable.a802);
        }
    }

    public Date GetDateFromEpoch(long epoch) {
        // Epoch arriving in SECONDS
        Date expiry = new Date(epoch * 1000);
        return expiry;
    }

    public void GetWeather(Location location) {
        if (location == null)
            return;

        setTitle("Busy...");

        DiscardListView();

        mWeatherObjectList.clear();
        mWeatherDataList.clear();

        String fetchUrl = String.format(baseUrl + "?lat=%s" + "&lon=%s" + key, location.getLatitude(), location.getLongitude());

        Log.i("LOCATION", fetchUrl);

        task = new HomeActivity.DownloadTask();
        task.execute(fetchUrl);
    }

    public void GetWeather(City city) {
        if (city == null)
            return;

        setTitle("Busy...");

        DiscardListView();

        mWeatherObjectList.clear();
        mWeatherDataList.clear();

        String fetchUrl = String.format(baseUrl + "?q=%s" + key, city.mName);

        Log.i("CITY", fetchUrl);

        task = new HomeActivity.DownloadTask();
        task.execute(fetchUrl);
    }

    public void DisplayWeather(String data) {

        try {
            JSONObject jsonObject = new JSONObject(data);

            if (jsonObject.getInt("cod") != 200) {
                Log.e("COD", String.valueOf(jsonObject.getInt("cod")));
                return;
            }

            Log.i("DATA", jsonObject.toString());

            ///////////////////////////////////////////////

            String listInfo = jsonObject.getString("list");
            JSONArray listArray = new JSONArray(listInfo);

            long epoch;
            String temperature;
            int humidity;
            int id;
            String main;
            String description;
            String windspeed;

            for (int i = 0; i < 36; i += 8) {
                // We do this in intervals of 8 since 8 x 3 hours = 24hours.
                JSONObject listPart = listArray.getJSONObject(i);
                JSONObject mainPartInList = listPart.getJSONObject("main");
                JSONArray weatherPartInList = listPart.getJSONArray("weather");
                JSONObject windPartInList = listPart.getJSONObject("wind");

                epoch = listPart.getLong("dt");
                temperature = mainPartInList.getString("temp");
                humidity = mainPartInList.getInt("humidity");

                id = weatherPartInList.getJSONObject(0).getInt("id");
                main = weatherPartInList.getJSONObject(0).getString("main");
                description = weatherPartInList.getJSONObject(0).getString("description");

                windspeed = windPartInList.getString("speed");

                //Convert Kelvin to C
                double tempKelvin = Float.parseFloat(temperature);
                double tempCelsius = tempKelvin - 273.15;
                tempCelsius = Math.round(tempCelsius);

                //Get Day of the week shorthand
                Date date = GetDateFromEpoch(epoch);
                String day = new SimpleDateFormat("EE").format(date);

                Weather weather = new Weather();

                weather.mDescription = WordUtils.capitalize(description);
                weather.mEpoch = epoch;
                weather.mId = id;
                weather.mMain = main;
                weather.mHumidity = humidity;
                weather.mTemperature = (int) tempCelsius;
                weather.mWindSpeed = Math.round(Float.parseFloat(windspeed));

                if (i == 0)
                    day = "Today";

                if (i == 8)
                    day = "Tomorrow";

                weather.mDay = day;

                mWeatherObjectList.add(weather);
            }

            City city = new City();

            JSONObject cityInfo = jsonObject.getJSONObject("city");
            JSONObject coordInfo = cityInfo.getJSONObject("coord");

            Location location = new Location("");

            location.setLatitude(coordInfo.getDouble("lat"));
            location.setLongitude(coordInfo.getDouble("lon"));

            city.mName = cityInfo.getString("name");
            city.mCode = cityInfo.getString("country");
            //city.mPopulation = cityInfo.getInt("population");
            city.mLocation = location;

            currentCity = city;

            setTitle(currentCity.mName + ", " + currentCity.mCode);

            UpdateWeatherIcons();

        } catch (JSONException e) {
            e.printStackTrace();
        }

        PopulateListWithWeatherData();
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection;

            try {
                url = new URL(urls[0]);

                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();

                while (data != -1) {
                    char current = (char) data;
                    result += current;

                    data = reader.read();
                }

                return result;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                DisplayWeather(result);
            }
            else {
                Log.e("ERROR", "ONPOSTEXECUTE ISSUE");
            }
        }
    }
}
