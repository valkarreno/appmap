package com.example.pictroviews;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MenuApp extends AppCompatActivity implements OnMapReadyCallback {

    private LocationHandlerThread locationHandlerThread;
    private Marker currentLocationMarker;
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_STORAGE_PERMISSION = 3;
    private GoogleMap googleMap;
    private Marker photoLocationMarker;
    private LocationManager locationManager;
    private Location lastLocation;
    private TextView latitud;
    private LocationListener locationListener;
    private TextView longitud;
    private TextView hora;
    private Button returnButton;
    private ImageView photoImageView;
    private SensorManager sensorManager;
    private Sensor orientationSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_app);
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.d("Storage", "Almacenamiento externo disponible");
        } else {
            Log.e("Storage", "Almacenamiento externo no disponible o no escribible");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        }
        locationHandlerThread = new LocationHandlerThread("LocationThread", locationListener);
        locationHandlerThread.start();
        locationHandlerThread.prepareHandler();
        locationHandlerThread.requestLocationUpdates(locationManager);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
        latitud = findViewById(R.id.latitudText);
        longitud = findViewById(R.id.longitudText);
        hora = findViewById(R.id.hourText);
        returnButton=findViewById(R.id.returnButton);

        photoImageView = findViewById(R.id.imageView);
        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backToMainMenu();
            }
        });

        locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                showCurrentLocationOnMap(location);
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            showCurrentLocationOnMap(lastKnownLocation);
        }
    }
    private void showCurrentLocationOnMap(Location location) {
        if (googleMap != null) {
            LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (currentLocationMarker == null) {
                        currentLocationMarker = googleMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("Ubicación Actual")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    } else {
                        currentLocationMarker.setPosition(currentLatLng);
                    }
                }
            });
        }
    }
    public void capturePhoto(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CAMERA_PERMISSION);
        } else {
            lastLocation = getLastKnownLocation();
            if (lastLocation != null) {
                updateUIWithNewLocation();
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            saveImage(imageBitmap);
            photoImageView.setImageBitmap(imageBitmap);
            updatePhotoLocationMarker();
            updateUIWithNewLocation();
        }
    }
    private void backToMainMenu(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    private void updateUIWithNewLocation() {
        if (lastLocation != null && googleMap != null) {
            double latitude = lastLocation.getLatitude();
            double longitude = lastLocation.getLongitude();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String currentTime = sdf.format(Calendar.getInstance().getTime());
            String time = currentTime;
            latitud.setText("Latitud: " + latitude);
            longitud.setText("Longitud: " + longitude);
            hora.setText("Hora: " + time);
            LatLng locationLatLng = new LatLng(latitude, longitude);
            addMarker(locationLatLng);
            moveCamera(locationLatLng);
        }
    }
    private void updatePhotoLocationMarker() {
        if (lastLocation != null && googleMap != null) {
            LatLng photoLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            if (photoLocationMarker != null) {
                photoLocationMarker.remove();
            }
            photoLocationMarker = googleMap.addMarker(new MarkerOptions()
                    .position(photoLatLng)
                    .title("Ubicación de la foto")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(photoLatLng, 15f));
        }
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    private void saveImage(Bitmap imageBitmap) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getFilesDir(), "Pictures");

        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            File imageFile = new File(storageDir, imageFileName + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();

            Toast.makeText(this, "Imagen guardada en " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            Log.d("SaveImage", "Imagen guardada en " + imageFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("SaveImage", "Error al guardar la imagen: " + e.getMessage());
            Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void addMarker(LatLng latLng) {
        if (photoLocationMarker != null) {
            photoLocationMarker .remove();
        }
        photoLocationMarker  = googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Ubicación de la foto")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void moveCamera(LatLng latLng) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean cameraPermissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            boolean locationPermissionGranted = grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED;
            boolean storagePermissionGranted = grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED;

            if (cameraPermissionGranted && locationPermissionGranted && storagePermissionGranted) {
                capturePhoto(null);
            } else {
                Toast.makeText(this, "Permisos de cámara, ubicación y/o almacenamiento denegados", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "Permiso de almacenamiento denegado", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            startLocationUpdates();
        }
    }
    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        return null;
    }
    public void startLocationUpdates() {
        if (locationHandlerThread != null) {
            locationHandlerThread.requestLocationUpdates(locationManager);
        } else {
            Log.e("PrincipalMenu", "locationHandlerThread is null");
        }
    }

    private void stopLocationUpdates() {

            locationHandlerThread.stopLocationUpdates(locationManager);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                }
            });
        }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
    public class LocationHandlerThread extends HandlerThread {
        private Handler handler;
        private LocationListener locationListener;
        public void setLocationListener(LocationListener listener) {
            locationListener = listener;
        }
        public LocationHandlerThread(String name, LocationListener listener) {
            super(name);
            locationListener = listener;
        }
        public void postTask(Runnable task) {
            handler.post(task);
        }

        public void prepareHandler() {
            handler = new Handler(getLooper());
        }

        public void requestLocationUpdates(LocationManager locationManager) {
            if (handler == null) {
                throw new IllegalStateException("Handler not prepared. Call prepareHandler() first.");
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationManager != null && locationListener != null) {
                        if (ActivityCompat.checkSelfPermission(MenuApp.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MenuApp.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);

                    }
                }
            });
        }
        public void stopLocationUpdates(LocationManager locationManager) {
            if (handler == null) {
                throw new IllegalStateException("Handler not prepared. Call prepareHandler() first.");
            }
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (locationManager != null && locationListener != null) {
                        locationManager.removeUpdates(locationListener);
                    }
                }
            });
        }
    }
}