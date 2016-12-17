package cl.tanmeza.fonocoptero;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;

/**
 * Created by Sebastian on 14-12-2016.
 */
public class PruebasActivity extends Activity implements LocationListener, SensorEventListener {
    String text_yawZero = "yawZero: ";
    String text_pitchZero = "pitchZero: ";
    String text_rollZero = "rollZero: ";
    String text_elevationZero = "elevationZero: ";
    String text_longitudeZero = "longitudeZero: ";
    String text_latitudeZero = "latitudeZero: ";
    public TextView textView_yawZero;
    public TextView textView_pitchZero;
    public TextView textView_rollZero;
    public TextView textView_elevationZero;
    public TextView textView_longitudeZero;
    public TextView textView_latitudeZero;
    String text_yaw = "yaw: ";
    String text_pitch = "pitch: ";
    String text_roll = "roll: ";
    String text_elevation = "elevation: ";
    String text_longitude = "longitude: ";
    String text_latitude = "latitude: ";
    public TextView textView_yaw;
    public TextView textView_pitch;
    public TextView textView_roll;
    public TextView textView_elevation;
    public TextView textView_longitude;
    public TextView textView_latitude;
    String text_motor1 = "Motor 1: ";
    String text_motor2 = "Motor 2: ";
    String text_motor3 = "Motor 3: ";
    String text_motor4 = "Motor 4: ";
    public TextView textView_motor1;
    public TextView textView_motor2;
    public TextView textView_motor3;
    public TextView textView_motor4;

    public TextView textView_altura_target;
    String text_altura_target = "Altura target: ";


    Handler handler,handler_2;
    Runnable run,run_2;
    int contador = 0;
    boolean loop_vuelo = true,loop_Zero = true;

    //constantes iniciales
    private static final int ESC_PWM_MINIMO_INICIAR = 750; //Este valor sólo se utiliza para incializar el acelerador electrónico
    private static final int ESC_PWM_MINIMO_RESPOSO = 800; //Período donde el motor se encuentra en reposo
    private static final int ESC_PWM_MINIMO_ACTIVO = 900;//Valor mínimo del período para el motor con limitador activo, aprox. un 10% de su capacidad.
    private static final int ESC_PWM_MAXIMO = 1700; //Valor máximo de inicialización del acelerador electrónico, y período donde el motor desarrolla su máxima potencia

    //Pines del IOIO
    private static final int PIN_IOIO_ESC_1 = 1; // MOTOR DELANTERO DERECHO
    private static final int PIN_IOIO_ESC_2 = 2; // MOTOR DELANTERO IZQUIERDO
    private static final int PIN_IOIO_ESC_3 = 3; // MOTOR TRASERO IZQUIERDO
    private static final int PIN_IOIO_ESC_4 = 4; // MOTOR TRASERO DERECHO

    //Variables de salidas y entradas del IOIO

    //Variables de valores
    private static boolean esta_armado = true;
    private static boolean ioio_enable = false;
    private static boolean loop_estabilizar = true;

    private static long periodoPWMmotor_1;
    private static long periodoPWMmotor_2;
    private static long periodoPWMmotor_3;
    private static long periodoPWMmotor_4;

    public SensorManager sensorManager;
    public Sensor rotationSensor, pressureSensor;


    public static boolean gps_enable=true;
    public static QuadCopter quad_estado;

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

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pruebas);

        quad_estado = new QuadCopter();
        rotationMatrix = new float[9];
        yawPitchRollVec = new float[3];
        rotationVec = new float[3];
        yawZero = 0.0f;
        pitchZero = 0.0f;
        rollZero = 0.0f;

        textView_yawZero = (TextView) findViewById(R.id.textView_yawZero);
        textView_pitchZero = (TextView) findViewById(R.id.textView_pitchZero);
        textView_rollZero = (TextView) findViewById(R.id.textView_rollZero);
        textView_elevationZero = (TextView) findViewById(R.id.textView_elevationZero);
        textView_longitudeZero = (TextView) findViewById(R.id.textView_longitudeZero);
        textView_latitudeZero = (TextView) findViewById(R.id.textView_latitudeZero);
        textView_yaw = (TextView) findViewById(R.id.textView_yaw);
        textView_pitch = (TextView) findViewById(R.id.textView_pitch);
        textView_roll = (TextView) findViewById(R.id.textView_roll);
        textView_elevation = (TextView) findViewById(R.id.textView_elevation);
        textView_longitude = (TextView) findViewById(R.id.textView_longitude);
        textView_latitude = (TextView) findViewById(R.id.textView_latitude);
        textView_motor1 = (TextView) findViewById(R.id.textView_motor1);
        textView_motor2 = (TextView) findViewById(R.id.textView_motor2);
        textView_motor3 = (TextView) findViewById(R.id.textView_motor3);
        textView_motor4 = (TextView) findViewById(R.id.textView_motor4);

        textView_altura_target = (TextView) findViewById(R.id.textView_altura_target);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size()!=0){
            rotationSensor = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this,rotationSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
        if(sensorManager.getSensorList(Sensor.TYPE_PRESSURE).size()!=0){
            pressureSensor = sensorManager.getSensorList(Sensor.TYPE_PRESSURE).get(0);
            sensorManager.registerListener(this,pressureSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                //toast("GPS configurado");
            }
        }

        comenzar_vuelo();

    }


    @Override
    public void onSensorChanged(SensorEvent event){
        Calendar calendar = Calendar.getInstance();
        long seconds = calendar.getTimeInMillis();
        DELAY_toast_new = seconds;
        if(DELAY_toast_new-DELAY_toast_old>500) {
            //String str = "SENSORES LEIDOS";
            //toast(str);
            DELAY_toast_old = seconds;
        }

        if (event.sensor == rotationSensor){
            // Get the time and the rotation vector.
            quad_estado.time = event.timestamp;
            System.arraycopy(event.values, 0, rotationVec, 0, 3);
            // Convert the to "yaw, pitch, roll".
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVec);
            SensorManager.getOrientation(rotationMatrix, yawPitchRollVec);

            // Make the measurements relative to the user-defined zero orientation.
            //arreglar
            quad_estado.yaw = QuadCopter.getMainAngle(-(yawPitchRollVec[0]) * RAD_TO_DEG);
            quad_estado.pitch = QuadCopter.getMainAngle(-(yawPitchRollVec[1]) * RAD_TO_DEG);
            quad_estado.roll = QuadCopter.getMainAngle((yawPitchRollVec[2]) * RAD_TO_DEG);

            // New sensors data are ready.
            newMeasurementsReady = true;
            //Log.v("Fonotest", "sensor_rotation 2");
        }
        else if(event.sensor == pressureSensor){
            float pressure = event.values[0];
            float rawAltitudeUnsmoothed = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
            absoluteElevation = (absoluteElevation*ALTITUDE_SMOOTHING) + (rawAltitudeUnsmoothed*(1.0f-ALTITUDE_SMOOTHING));
            //quad_estado.baroElevacion = absoluteElevation - elevationZero;
            quad_estado.baroElevacion = absoluteElevation;

        }
    }

    private boolean comprobar_sensores() {
        final Context context = this;
        boolean enable;
        if((rotationSensor == null) || (pressureSensor == null) || !gps_enable) {
            enable = false;
        }
        else{
            enable = true;
        }
        return enable;
    }



    @Override
    public void onLocationChanged(Location location){
        quad_estado.gpsElevacion = (float) location.getAltitude();
        quad_estado.gpsExactitud = location.getAccuracy();
        quad_estado.longitude = location.getLongitude();
        quad_estado.latitude = location.getLatitude();

        quad_estado.nSatelites = location.getExtras().getInt("satellites");
        String str = "GPS: " + "altitud: " + quad_estado.gpsElevacion + ", " + "exactitud: " + quad_estado.gpsExactitud + ", "
                + "longitud: " + quad_estado.longitude + ", " + "latitud: " + quad_estado.latitude + ", "
                + "nºSatelites: " + quad_estado.nSatelites;// + ", "
                /*+ "navegacion: " + location.getBearing() + ", " + "velocidad: " + location.getSpeed() + ", "
                + "proveedor: " + location.getProvider();*/

        // Convert longitude+latitude to x+y (using the small angles approximation: sin(x)~=x).
        quad_estado.xPos = (float) (EARTH_RADIUS * (quad_estado.longitude-longitudeZero) * DEG_TO_RAD);
        quad_estado.yPos = (float) (EARTH_RADIUS * (quad_estado.latitude-latitudeZero) * DEG_TO_RAD);
        // Convert heading+speed to speedx+speedy.
        quad_estado.xVelocidad = location.getSpeed() * (float) Math.cos(location.getBearing());
        quad_estado.yVelocidad = location.getSpeed() * (float) Math.sin(location.getBearing());
        //str = "Latitude: "+location.getLatitude()+", Longitude: "+location.getLongitude();

        Calendar calendar = Calendar.getInstance();
        long seconds = calendar.getTimeInMillis();
        DELAY_toast_GPS_new = seconds;
        if(DELAY_toast_GPS_new-DELAY_toast_GPS_old>10000) {
            //Log.v("Fonotest", "GPS change");
            //toast(str);
            DELAY_toast_GPS_old = seconds;
        }
    }


    private void toast(final String message) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        alert_dialog_salir();
    }

    private void alert_dialog_salir() {
        final Context context = this;
        final String title = "Salir";
        final String message= "¿Está seguro que desea salir?";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(title).setMessage(message)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                detener_gps();
                                loop_vuelo = false;
                                finish();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .setIcon(R.drawable.ic_action_warning).show();
            }
        });
    }

    @Override
    public void onProviderDisabled(String provider) {
        //se llama cuando el Gps está apagado
        gps_enable = false;
        //toast("Su GPS está apagado. Actívelo y vuelva a intentarlo.");
    }

    @Override
    public void onProviderEnabled(String provider) {
        //se llama cuando el Gps está activado
        gps_enable = true;
        //toast("GPS activado.");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras){
        //se llama cuando hay una cambio en el estado del gps
        quad_estado.nSatelites = extras.getInt("satellites");
        quad_estado.gpsEstado = status;
    }

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

    protected void comenzar_gps(){
        if(sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).size()!=0){
            rotationSensor = sensorManager.getSensorList(Sensor.TYPE_ROTATION_VECTOR).get(0);
            sensorManager.registerListener(this,rotationSensor, SensorManager.SENSOR_DELAY_NORMAL); //SENSOR_DELAY_NORMAL o SENSOR_DELAY_FASTEST
        }
        if(sensorManager.getSensorList(Sensor.TYPE_PRESSURE).size()!=0){
            pressureSensor = sensorManager.getSensorList(Sensor.TYPE_PRESSURE).get(0);
            sensorManager.registerListener(this,pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void detener_gps(){
        //detener listener de los sensores
        sensorManager.unregisterListener(this);
        //detener listener del GPS
        if (locationManager != null){
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.removeUpdates(this);
            }
        }
    }

    private void comenzar_vuelo() {
        if(esta_armado){
            ioio_enable = true;
            periodoPWMmotor_1 = 0;
            periodoPWMmotor_2 = 0;
            periodoPWMmotor_3 = 0;
            periodoPWMmotor_4 = 0;

            yawRegulator = new PIDReguladorAngulo(0.002f, 0.0f, 0.0f, PID_DERIV_SMOOTHING);
            pitchRegulator = new PIDReguladorAngulo(0.01f, 0.0f, 0.0f, PID_DERIV_SMOOTHING);
            rollRegulator = new PIDReguladorAngulo(0.01f, 0.0f, 0.0f, PID_DERIV_SMOOTHING);
            altitudeRegulator = new PIDReguladorAltura(0.2f,  0.0f,  0.0f, PID_DERIV_SMOOTHING, 5.0f);
            new CountDownTimer(2000,1000) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    getEstadoActualComoZero();
                    estabilizar_vuelo();
                }
            }.start();
        }
        else{
            toast("Los motores no se armaron correctamente.");
        }
    }

    private void estabilizar_vuelo() {
        handler = new Handler();
        run = new Runnable() {
            @Override
            public void run() {
                //Con dt=20 (loop cada 0,02 segundos) 1000 se alcanza a los 20 segundos,
                // lo que permite obtener valores más exactos de los sensores
                float altura_target=0.0f;
                if(barometro_estabilizado){
                    float yawForce, pitchForce, rollForce, altitudeForce;
                    float currentYaw, currentPitch, currentRoll, currentAltitude;
                    double tempPowerNW, tempPowerNE, tempPowerSE, tempPowerSW;

                    loop_estabilize_contador++;
                    if(loop_estabilize_contador==500){
                        toast("comienza ascenso");
                        altitudeLock_bajo = false;
                    }
                    else if(loop_estabilize_contador==2000){
                        toast("comienza descenso");
                        altitudeLock_alto = false;
                    }
                    currentYaw = quad_estado.yaw;
                    currentPitch = quad_estado.pitch;
                    currentRoll = quad_estado.roll;
                    currentAltitude = quad_estado.baroElevacion;
                    yawForce = yawRegulator.getInput(yawAngleTarget, currentYaw, dt);
                    pitchForce = pitchRegulator.getInput(pitchAngleTarget, currentPitch, dt);
                    rollForce = rollRegulator.getInput(rollAngleTarget, currentRoll, dt);
                    altura_target=calcular_altura_target(1.0f);
                    altitudeForce = altitudeRegulator.getInput(altura_target, currentAltitude, dt);
                    //altitudeForce = meanThrust;

                    tempPowerNW = altitudeForce; // Vertical "force".
                    tempPowerNE = altitudeForce; //
                    tempPowerSE = altitudeForce; //
                    tempPowerSW = altitudeForce; //

                    tempPowerNW += pitchForce; // Pitch "force".
                    tempPowerNE += pitchForce; //
                    tempPowerSE -= pitchForce; //
                    tempPowerSW -= pitchForce; //

                    tempPowerNW += rollForce; // Roll "force".
                    tempPowerNE -= rollForce; //
                    tempPowerSE -= rollForce; //
                    tempPowerSW += rollForce; //

                    tempPowerNW += yawForce; // Yaw "force".
                    tempPowerNE -= yawForce; //
                    tempPowerSE += yawForce; //
                    tempPowerSW -= yawForce; //

                    //periodoPWMmotor_1 = ne;
                    //periodoPWMmotor_2 = nw;
                    //periodoPWMmotor_3 = sw;
                    //periodoPWMmotor_4 = se;

                    periodoPWMmotor_1 = motorSaturacion(periodoPWMmotor_1+tempPowerNE);
                    periodoPWMmotor_2 = motorSaturacion(periodoPWMmotor_2+tempPowerNW);
                    periodoPWMmotor_3 = motorSaturacion(periodoPWMmotor_3+tempPowerSW);
                    periodoPWMmotor_4 = motorSaturacion(periodoPWMmotor_4+tempPowerSE);
                    if(loop_estabilize_contador==3000){
                        loop_vuelo=false;
                        periodoPWMmotor_1 = 0;
                        periodoPWMmotor_2 = 0;
                        periodoPWMmotor_3 = 0;
                        periodoPWMmotor_4 = 0;
                        toast("Se Detiene");
                    }
                }
                getEstadoActual(altura_target);
                executeActuatorActions();
                //Log.v("Fonocoptero", "antes sleep - estabilizar_vuelo " + contador);
                if(loop_vuelo) { //checks if it is not already 3 second
                    handler.postDelayed(run, (long) dt*1000); //run the method again
                }
            }
        };
        handler.postDelayed(run, (long) dt*1000); //will call the runnable every 1 second
    }

    public float calcular_altura_target(float altura_esperada){
        float altura_target = 0.0f,altura_maxima=elevationZero+altura_esperada;

        if(altitudeLock_bajo){
            altura_target = elevationZero;
            return altura_target;
        }
        if(altitudeLock_alto){
            altura_target = altura_maxima;
            return altura_target;
        }
        else if(altura_subiendo){
            altura_deseada = altura_deseada+0.002f;
            if(altura_deseada>=altura_maxima){
                altitudeLock_alto=true;
                altura_bajando=true;
                altura_subiendo=false;
                return altura_maxima;
            }
            else{
                return altura_deseada;
            }
        }
        else if(altura_bajando){
            altura_deseada = altura_deseada-0.002f;
            if(altura_deseada<=elevationZero){
                altitudeLock_bajo=true;
                altura_subiendo=true;
                altura_bajando=false;
                return elevationZero;
            }
            else{
                return altura_deseada;
            }
        }
        return elevationZero;
    }


    public void getEstadoActual(float altura_target){
        textView_yaw.setText(text_yaw+quad_estado.yaw);
        textView_pitch.setText(text_pitch+quad_estado.pitch);
        textView_roll.setText(text_roll+quad_estado.roll);
        textView_elevation.setText(text_elevation+quad_estado.baroElevacion);
        textView_longitude.setText(text_longitude+quad_estado.longitude);
        textView_latitude.setText(text_latitude + quad_estado.latitude);
        textView_altura_target.setText(text_altura_target+altura_target);

    }

    private void executeActuatorActions(){
        textView_motor1.setText(text_motor1+periodoPWMmotor_1);
        textView_motor2.setText(text_motor2+periodoPWMmotor_2);
        textView_motor3.setText(text_motor3 + periodoPWMmotor_3);
        textView_motor4.setText(text_motor4 + periodoPWMmotor_4);
    }

    public void getEstadoActualComoZero() {
        handler_2 = new Handler();
        run_2 = new Runnable() {
            @Override
            public void run() {

                barometro_new = quad_estado.baroElevacion;
                if(barometro_old>barometro_new){
                    yawZero = quad_estado.yaw;
                    pitchZero = quad_estado.pitch;
                    rollZero = quad_estado.roll;
                    elevationZero = quad_estado.baroElevacion;
                    altura_deseada = elevationZero;
                    longitudeZero = (float) quad_estado.longitude;
                    latitudeZero = (float) quad_estado.latitude;
                    textView_yawZero.setText(text_yawZero+yawZero);
                    textView_pitchZero.setText(text_pitchZero+pitchZero);
                    textView_rollZero.setText(text_rollZero+rollZero);
                    textView_elevationZero.setText(text_elevationZero+elevationZero);
                    textView_longitudeZero.setText(text_longitudeZero+longitudeZero);
                    textView_latitudeZero.setText(text_latitudeZero+latitudeZero);

                    yawAngleTarget = yawZero;
                    pitchAngleTarget = pitchZero;
                    rollAngleTarget = rollZero;
                    altitudeTarget = elevationZero;

                    periodoPWMmotor_1 = ESC_PWM_MINIMO_ACTIVO;
                    periodoPWMmotor_2 = ESC_PWM_MINIMO_ACTIVO;
                    periodoPWMmotor_3 = ESC_PWM_MINIMO_ACTIVO;
                    periodoPWMmotor_4 = ESC_PWM_MINIMO_ACTIVO;

                    toast("Barómetro Calibrado. Zero definido.");
                    loop_Zero=false;
                    barometro_estabilizado = true;

                }
                barometro_old = barometro_new;

                if (loop_Zero) { //checks if it is not already 3 second
                    handler_2.postDelayed(run_2, (long) 1000); //run the method again
                }
            }
        };
        handler_2.postDelayed(run_2, (long) 1000); //will call the runnable every 1 second
    }

    private int motorSaturacion(double val){
        if(val > (double) ESC_PWM_MAXIMO)
            return ESC_PWM_MAXIMO;
        else if(val < (double) ESC_PWM_MINIMO_ACTIVO)
            return ESC_PWM_MINIMO_ACTIVO;
        else
            return (int) val;
    }

    public static final int STATE_SEND_DIVIDER = 20;
    public static final double MAX_MOTOR_POWER = 255.0; // 255.0 normally, less for testing.
    public static final float MAX_TIME_WITHOUT_PC_RX = 1.0f; // Maximum time [s] without any message from the PC, before emergency stop.
    public static final float MAX_TIME_WITHOUT_ADK_RX = 1.0f; // Maximum time [s] without any message from the ADK, setting the temperature to 0 (error).
    public static final long INT_MAX = 2147483648L;
    public static final float MAX_SAFE_PITCH_ROLL = 60; // [deg].
    public static final float PID_DERIV_SMOOTHING = 0.5f;


    float dt = 0.02f,barometro_new = -999.0f, barometro_old = -1000.0f, altura_deseada=0.0f;
    boolean barometro_estabilizado = false;
    private float meanThrust, yawAngleTarget, pitchAngleTarget, rollAngleTarget,
            altitudeTarget, batteryVoltage, timeWithoutPcRx,
            timeWithoutAdkRx;
    public static boolean altitudeLock_bajo=true,altitudeLock_alto=false,altura_subiendo=true,altura_bajando=false;
    private PIDReguladorAngulo yawRegulator, pitchRegulator, rollRegulator;
    private PIDReguladorAltura altitudeRegulator;

    private int stateSendDividerCounter;
    public static long loop_estabilize_contador=0;

}
