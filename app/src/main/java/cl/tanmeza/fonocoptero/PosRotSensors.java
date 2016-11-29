package cl.tanmeza.fonocoptero;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Calendar;

import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class PosRotSensors extends IOIOService implements LocationListener, SensorEventListener{

    private SensorManager sensorManager;
    private PowerManager mPowerManager;
    private WindowManager mWindowManager;
    private Display mDisplay;

    private Sensor rotationSensor, pressureSensor;
    private HeliState heliState;
    private float[] rotationVec, rotationMatrix, yawPitchRollVec;
    private float yawZero, pitchZero, rollZero, elevationZero, latitudeZero,
            longitudeZero, absoluteLongitude, absoluteLatitude, absoluteElevation;
    private boolean newMeasurementsReady;
    private LocationManager locationManager;
    private LocationListener locationListener;

    public static long DELAY_toast_old = 0;
    public static long DELAY_toast_new = 0;
    public static long DELAY_toast_GPS_old = 0;
    public static long DELAY_toast_GPS_new = 0;

    public static final float PI = 3.14159265359f;
    public static final float RAD_TO_DEG = 180.0f / PI;
    public static final double DEG_TO_RAD = PI / 180.0;
    public static final float ALTITUDE_SMOOTHING = 0.95f;
    public static final double EARTH_RADIUS = 6371000; // [m].
    public static final boolean USE_GPS = false;
    Context mContext;

    public PosRotSensors(Context mContext){
        this.mContext = mContext;
        heliState = new HeliState();
        rotationMatrix = new float[9];
        yawPitchRollVec = new float[3];
        rotationVec = new float[3];

        yawZero = 0.0f;
        pitchZero = 0.0f;
        rollZero = 0.0f;

        // Get the sensors manager.
        sensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);

        if(sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size()!=0){
            rotationSensor = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this,rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if(sensorManager.getSensorList(Sensor.TYPE_PRESSURE).size()!=0){
            pressureSensor = sensorManager.getSensorList(Sensor.TYPE_PRESSURE).get(0);
            sensorManager.registerListener(this,pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
        /*

        // Get the sensors.
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        */

        Log.v("Fonotest", "paso 3");
        if((rotationSensor == null) || (pressureSensor == null)) {
            Log.v("Fonotest", "Sensores faltantes!");
        }
        else{
            Log.v("Fonotest", "Sensor rota: "+rotationSensor.getName()+ ", Sensor press: "+pressureSensor.getName());
        }

        //insert HERE

        //Log.v("Fonotest", "GPS:"+USE_GPS);

        /*
        if(USE_GPS){
            locationManager = (LocationManager) getSystemService(mContext.LOCATION_SERVICE);
            Log.v("Fonotest", "paso 4 gps");
        }
        */
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 3000, 10, this);
        if (locationManager != null){
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(mContext,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                Log.v("Fonotest", "GPS configurado");
            }
        }


    }




    /*
    @Override
    protected void onResume(){
        super.onResume();
        if(sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size()!=0){
            rotationSensor = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this,rotationSensor, SensorManager.SENSOR_DELAY_NORMAL); //SENSOR_DELAY_NORMAL o SENSOR_DELAY_FASTEST
        }
        if(sensorManager.getSensorList(Sensor.TYPE_PRESSURE).size()!=0){
            pressureSensor = sensorManager.getSensorList(Sensor.TYPE_PRESSURE).get(0);
            sensorManager.registerListener(this,pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    @Override
    protected void onPause(){
        super.onPause();
        //sensorManager.unregisterListener(this);
    }
    */

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {

                Thread.currentThread().setPriority(10);
            }

            @Override
            public void loop() throws ConnectionLostException, InterruptedException {

            }

            @Override
            public void disconnected() {
                super.disconnected();
                stopSelf();
            }


        };
    }



    protected void detener(){
        //detener listener de los sensores
        sensorManager.unregisterListener(this);
        //detener listener del GPS
        if (locationManager != null){
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.removeUpdates(this);
            }
        }
    }

    protected void comenzar(){
        if(sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size()!=0){
            rotationSensor = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this,rotationSensor, SensorManager.SENSOR_DELAY_NORMAL); //SENSOR_DELAY_NORMAL o SENSOR_DELAY_FASTEST
        }
        if(sensorManager.getSensorList(Sensor.TYPE_PRESSURE).size()!=0){
            pressureSensor = sensorManager.getSensorList(Sensor.TYPE_PRESSURE).get(0);
            sensorManager.registerListener(this,pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }

/*
    @Override
    protected void onPause(){
        super.onPause();
        Log.v("Fonotest", "pausa sensor");
        // Disable the GPS.
        if(USE_GPS)

            if (locationManager != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locationManager.removeUpdates(locationListener);
                }
            }
            //locationManager.removeUpdates(locationListener);
        // Disable the inertial sensors.
        sensorManager.unregisterListener(this);
        }
*/

    @Override
    public void onLocationChanged(Location location){
        heliState.gpsElevation = (float) location.getAltitude();
        heliState.gpsAccuracy = location.getAccuracy();
        heliState.longitude = location.getLongitude();
        heliState.latitude = location.getLatitude();

        heliState.nSatellites = location.getExtras().getInt("satellites");
        String str = "GPS: " + "altitud: " + heliState.gpsElevation + ", " + "exactitud: " + heliState.gpsAccuracy + ", "
                + "longitud: " + heliState.longitude + ", " + "latitud: " + heliState.latitude + ", "
                + "nºSatelites: " + heliState.nSatellites;// + ", "
                /*+ "navegacion: " + location.getBearing() + ", " + "velocidad: " + location.getSpeed() + ", "
                + "proveedor: " + location.getProvider();*/

        // Convert longitude+latitude to x+y (using the small angles approximation: sin(x)~=x).
        heliState.xPos = (float) (EARTH_RADIUS * (heliState.longitude-longitudeZero) * DEG_TO_RAD);
        heliState.yPos = (float) (EARTH_RADIUS * (heliState.latitude-latitudeZero) * DEG_TO_RAD);

        // Convert heading+speed to speedx+speedy.
        heliState.xSpeed = location.getSpeed() * (float) Math.cos(location.getBearing());
        heliState.ySpeed = location.getSpeed() * (float) Math.sin(location.getBearing());

        //str = "Latitude: "+location.getLatitude()+", Longitude: "+location.getLongitude();

        Calendar calendar = Calendar.getInstance();
        long seconds = calendar.getTimeInMillis();
        DELAY_toast_GPS_new = seconds;
        if(DELAY_toast_GPS_new-DELAY_toast_GPS_old>5000) {
            //Log.v("Fonotest", "GPS change");
            Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
            DELAY_toast_GPS_old = seconds;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "[iv]: Servicio IOIO iniciado", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onProviderDisabled(String provider) {
        /******** se llama cuando el Gps esta apagado *********/
        Toast.makeText(mContext, "GPS apagado!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        /******** se llama cuando el Gps esta prendido *********/
        Toast.makeText(mContext, "GPS prendido", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){
        //Log.v("Fonotest", "GPS sensor change status");
        heliState.nSatellites = extras.getInt("satellites");
        heliState.gpsStatus = status;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event){
        Calendar calendar = Calendar.getInstance();
        long seconds = calendar.getTimeInMillis();
        DELAY_toast_new = seconds;
        if(DELAY_toast_new-DELAY_toast_old>20000) {
            String str = "SENSORES LEIDOS";
            //Log.v("Fonotest", str);
            Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
            DELAY_toast_old = seconds;
        }

        if (event.sensor == rotationSensor){
            // Get the time and the rotation vector.
            heliState.time = event.timestamp;
            System.arraycopy(event.values, 0, rotationVec, 0, 3);
            // Convert the to "yaw, pitch, roll".
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVec);
            SensorManager.getOrientation(rotationMatrix, yawPitchRollVec);

            // Make the measurements relative to the user-defined zero orientation.
            heliState.yaw = getMainAngle(-(yawPitchRollVec[0]-yawZero) * RAD_TO_DEG);
            heliState.pitch = getMainAngle(-(yawPitchRollVec[1]-pitchZero) * RAD_TO_DEG);
            heliState.roll = getMainAngle((yawPitchRollVec[2]-rollZero) * RAD_TO_DEG);

            // New sensors data are ready.
            newMeasurementsReady = true;
            //Log.v("Fonotest", "sensor_rotation 2");
        }
        else if(event.sensor == pressureSensor){
            float pressure = event.values[0];
            float rawAltitudeUnsmoothed = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
            absoluteElevation = (absoluteElevation*ALTITUDE_SMOOTHING) + (rawAltitudeUnsmoothed*(1.0f-ALTITUDE_SMOOTHING));
            heliState.baroElevation = absoluteElevation - elevationZero;
        }
    }

    public class HeliState{
        public float yaw, pitch, roll, // [degrees].
                baroElevation, gpsElevation; // [m].
        public long time; // [nanoseconds].
        public double longitude, latitude; // [degrees].
        public float gpsAccuracy, xPos, yPos; // [m].
        public float xSpeed, ySpeed; // [m/s].
        public int nSatellites, gpsStatus; // [].
        }

    public HeliState getState(){
        newMeasurementsReady = false;
        return heliState;
        }

    public void setCurrentStateAsZero(){
        yawZero = yawPitchRollVec[0];
        pitchZero = yawPitchRollVec[1];
        rollZero = yawPitchRollVec[2];
        elevationZero = absoluteElevation;
        longitudeZero = absoluteLongitude;
        latitudeZero = absoluteLatitude;
        }

    public boolean newMeasurementsReady()
    {
        return newMeasurementsReady;
    }
    // Return the smallest angle between two segments.
    public static float getMainAngle(float angle){
        while(angle < -180.0f){
            angle += 360.0f;
        }
        while(angle > 180.0f){
            angle -= 360.0f;
        }
        return angle;
    }

}
