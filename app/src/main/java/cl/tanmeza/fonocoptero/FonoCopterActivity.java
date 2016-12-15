package cl.tanmeza.fonocoptero;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Calendar;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;



public class FonoCopterActivity extends IOIOActivity implements LocationListener, SensorEventListener{
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
    private static DigitalOutput statLed;
    private static PwmOutput ESC_motor_1;
    private static PwmOutput ESC_motor_2;
    private static PwmOutput ESC_motor_3;
    private static PwmOutput ESC_motor_4;

    //Variables de valores
    static Boolean ECS_calibrado = false;//¿Está calibrado el Acelerador Electrónico?
    public static boolean calibrado = false;
    private static boolean esta_armado = false;
    private static boolean ioio_enable = false;


    private static long periodoPWMmotor_1;
    private static long periodoPWMmotor_2;
    private static long periodoPWMmotor_3;
    private static long periodoPWMmotor_4;


    //boolean mBounded;
    //public IOIOServicio ioioServ;



    public TextView cronometro_paso_2;
    public TextView cronometro_paso_3;
    public Button paso_1_sensores;
    public Button paso_2_calibrar;
    public Button paso_3_comenzar;
    public Button paso_3_detener;
    public Button global_salir;
    public SensorManager sensorManager;
    public Sensor rotationSensor, pressureSensor;
    public TextView paso_2_quedan, paso_2_segundos, paso_3_segundos, paso_3_comenzara;



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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_principal);

        quad_estado = new QuadCopter();
        rotationMatrix = new float[9];
        yawPitchRollVec = new float[3];
        rotationVec = new float[3];
        yawZero = 0.0f;
        pitchZero = 0.0f;
        rollZero = 0.0f;

        //se definen los botones
        paso_1_sensores = (Button) findViewById(R.id.sensores_button);
        paso_2_calibrar = (Button) findViewById(R.id.armado_button);
        paso_3_comenzar = (Button) findViewById(R.id.botonComenzar);
        paso_3_detener = (Button) findViewById(R.id.botonDetener);
        global_salir = (Button) findViewById(R.id.botonSalir);

        cronometro_paso_2 = (TextView) findViewById(R.id.paso_2_chronometer);
        cronometro_paso_3 = (TextView) findViewById(R.id.paso_3_chronometer);
        paso_2_quedan = (TextView) findViewById(R.id.paso_2_quedan);
        paso_2_segundos = (TextView) findViewById(R.id.paso_2_segundos);
        paso_3_segundos = (TextView) findViewById(R.id.paso_3_segundos);
        paso_3_comenzara = (TextView) findViewById(R.id.paso_3_comenzara);

        cronometro_paso_2.setVisibility(View.GONE);
        paso_2_quedan.setVisibility(View.GONE);
        paso_2_segundos.setVisibility(View.GONE);
        cronometro_paso_3.setVisibility(View.GONE);
        paso_3_segundos.setVisibility(View.GONE);
        paso_3_comenzara.setVisibility(View.GONE);

        paso_1_sensores.setOnClickListener(botones_listener);
        paso_2_calibrar.setOnClickListener(botones_listener);
        paso_3_comenzar.setOnClickListener(botones_listener);
        paso_3_detener.setOnClickListener(botones_listener);
        global_salir.setOnClickListener(botones_listener);


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



    }

    View.OnClickListener botones_listener = new View.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.sensores_button:
                    //toast("Comprobar sensores presionado");
                    habilitar_botones(1);
                    break;
                case R.id.armado_button:
                    //toast("Calibrar presionado");
                    habilitar_botones(2);
                    break;
                case R.id.botonComenzar:
                    //toast("Comenzar presionado");
                    habilitar_botones(3);
                    break;
                case R.id.botonDetener:
                    //toast("Detener presionado");
                    habilitar_botones(4);
                    break;
                case R.id.botonSalir:
                    //toast("Salir presionado");
                    alert_dialog_salir();
                    break;
            }
        }
    };

    private void habilitar_botones(final int boton){
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch(boton) {
                    case 1:
                        boolean activador_1 = comprobar_sensores();
                        if(activador_1){
                            paso_1_sensores.setEnabled(false);
                            paso_2_calibrar.setEnabled(true);
                            paso_3_comenzar.setEnabled(false);
                            paso_3_detener.setEnabled(false);
                            alert_dialog("Comprobación de Sensores", "Aceptado. Presiona aceptar para continuar.", true);
                        }
                        else{
                            paso_1_sensores.setEnabled(true);
                            paso_2_calibrar.setEnabled(false);
                            paso_3_comenzar.setEnabled(false);
                            paso_3_detener.setEnabled(false);
                            alert_dialog("Comprobación de Sensores","Rechazado. Tu teléfono no posee los sensores necesarios o tu GPS está desactivado.",false);
                        }
                        break;
                    case 2:
                        paso_1_sensores.setEnabled(false);
                        paso_2_calibrar.setEnabled(false);
                        paso_3_comenzar.setEnabled(false);
                        paso_3_detener.setEnabled(false);
                        global_salir.setEnabled(false);
                        comprobar_calibrado();
                        invisible_counter(2, true);
                        new CountDownTimer(10000,1000) {
                            public void onTick(long millisUntilFinished) {
                                long contador_2 = (millisUntilFinished/1000);
                                String word = ""+contador_2;
                                cronometro_paso_2.setText(word);
                            }
                            public void onFinish() {
                                //toast("Listo!");
                                final boolean activador_2 = calibrado;
                                global_salir.setEnabled(true);
                                cronometro_paso_2.setText("0");
                                invisible_counter(2, false);
                                if (activador_2) {
                                    paso_1_sensores.setEnabled(false);
                                    paso_2_calibrar.setEnabled(false);
                                    paso_3_comenzar.setEnabled(true);
                                    paso_3_detener.setEnabled(false);
                                    alert_dialog("Calibración", "Aceptado. Presiona aceptar para continuar.", true);
                                }
                                else{
                                    paso_1_sensores.setEnabled(false);
                                    paso_2_calibrar.setEnabled(true);
                                    paso_3_comenzar.setEnabled(false);
                                    paso_3_detener.setEnabled(false);
                                    alert_dialog("Calibración","Rechazado. El cuadricóptero no se ha podido calibrar correctamente. Inténtalo nuevamente.",false);
                                }
                            }
                        }.start();
                        break;
                    case 3:
                        paso_1_sensores.setEnabled(false);
                        paso_2_calibrar.setEnabled(false);
                        paso_3_comenzar.setEnabled(false);
                        paso_3_detener.setEnabled(false);
                        global_salir.setEnabled(false);

                        invisible_counter(3, true);
                        new CountDownTimer(10000,1000) {
                            final boolean activador_3 = comprobar_sensores();
                            public void onTick(long millisUntilFinished) {
                                long contador_3 = (millisUntilFinished/1000);
                                String word = ""+contador_3;
                                cronometro_paso_3.setText(word);
                            }
                            public void onFinish() {
                                //toast("Listo!");
                                global_salir.setEnabled(true);
                                cronometro_paso_3.setText("0");
                                invisible_counter(3,false);
                                if (activador_3) {
                                    paso_1_sensores.setEnabled(false);
                                    paso_2_calibrar.setEnabled(false);
                                    paso_3_comenzar.setEnabled(false);
                                    paso_3_detener.setEnabled(true);
                                    //alert_dialog("Comenzar Vuelo", "Aceptado.", true);
                                    toast("Comienza el vuelo");
                                }
                                else{
                                    paso_1_sensores.setEnabled(false);
                                    paso_2_calibrar.setEnabled(false);
                                    paso_3_comenzar.setEnabled(true);
                                    paso_3_detener.setEnabled(false);
                                    //alert_dialog("Comenzar Vuelo","Rechazado.",false);
                                    toast("No ha comenzado el vuelo");
                                }
                            }
                        }.start();

                        break;
                    case 4:
                        boolean activador_4 = comprobar_sensores();
                        if (activador_4) {
                            paso_1_sensores.setEnabled(false);
                            paso_2_calibrar.setEnabled(false);
                            paso_3_comenzar.setEnabled(true);
                            paso_3_detener.setEnabled(false);
                            //alert_dialog("Detener Vuelo", "Aceptado.", true);
                        }
                        else{
                            paso_1_sensores.setEnabled(false);
                            paso_2_calibrar.setEnabled(false);
                            paso_3_comenzar.setEnabled(false);
                            paso_3_detener.setEnabled(true);
                            //alert_dialog("Detener Vuelo","Rechazado.",false);
                        }
                        break;
                    case 5:
                        global_salir.setEnabled(true);
                        break;
                }
            }
        });
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
            quad_estado.yaw = QuadCopter.getMainAngle(-(yawPitchRollVec[0] - yawZero) * RAD_TO_DEG);
            quad_estado.pitch = QuadCopter.getMainAngle(-(yawPitchRollVec[1] - pitchZero) * RAD_TO_DEG);
            quad_estado.roll = QuadCopter.getMainAngle((yawPitchRollVec[2] - rollZero) * RAD_TO_DEG);

            // New sensors data are ready.
            newMeasurementsReady = true;
            //Log.v("Fonotest", "sensor_rotation 2");
        }
        else if(event.sensor == pressureSensor){
            float pressure = event.values[0];
            float rawAltitudeUnsmoothed = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure);
            absoluteElevation = (absoluteElevation*ALTITUDE_SMOOTHING) + (rawAltitudeUnsmoothed*(1.0f-ALTITUDE_SMOOTHING));
            quad_estado.baroElevacion = absoluteElevation - elevationZero;
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


    private void invisible_counter(final int numero,final boolean visible){
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(numero==2){
                    if(visible){
                        cronometro_paso_2.setVisibility(View.VISIBLE);
                        paso_2_quedan.setVisibility(View.VISIBLE);
                        paso_2_segundos.setVisibility(View.VISIBLE);
                    }
                    else{
                        cronometro_paso_2.setVisibility(View.GONE);
                        paso_2_quedan.setVisibility(View.GONE);
                        paso_2_segundos.setVisibility(View.GONE);
                    }
                }
                else if(numero==3){
                    if(visible){
                        cronometro_paso_3.setVisibility(View.VISIBLE);
                        paso_3_segundos.setVisibility(View.VISIBLE);
                        paso_3_comenzara.setVisibility(View.VISIBLE);
                    }
                    else{
                        cronometro_paso_3.setVisibility(View.GONE);
                        paso_3_segundos.setVisibility(View.GONE);
                        paso_3_comenzara.setVisibility(View.GONE);
                    }
                }
            }
        });
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

    private void alert_dialog(final String title,final String message,final boolean estado) {
        final Context context = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (estado) {
                    new AlertDialog.Builder(context)
                            .setTitle(title).setMessage(message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                }
                            })
                            .setIcon(R.drawable.tick)
                            .show();
                } else {
                    new AlertDialog.Builder(context)
                            .setTitle(title).setMessage(message)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // continue with delete
                                }
                            })
                            .setIcon(R.drawable.delete)
                            .show();
                }

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

    //CONEXIÓN CON IOIO

    public void comprobar_calibrado() {
        try {
            calibrado = armar_motores(true);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new Looper();
    }

    class Looper extends BaseIOIOLooper {
        private AnalogInput input_;
        private PwmOutput pwmOutput_;
        private DigitalOutput led_;

        @Override
        public void setup() throws ConnectionLostException, InterruptedException {
            Log.v("Fonocoptero - IOIO", "Comienza configuración del IOIO");
            statLed = ioio_.openDigitalOutput(0, true); //true significa que el led de status comenzará apagado, esto debido a su lógica invertida
            //donde true = OFF y false = ON
            statLed.write(false);
            Thread.sleep(1000);
            statLed.write(true);
            Thread.sleep(1000);

            ESC_motor_1 = ioio_.openPwmOutput(PIN_IOIO_ESC_1, 50);
            ESC_motor_2 = ioio_.openPwmOutput(PIN_IOIO_ESC_2, 50);
            ESC_motor_3 = ioio_.openPwmOutput(PIN_IOIO_ESC_3, 50);
            ESC_motor_4 = ioio_.openPwmOutput(PIN_IOIO_ESC_4, 50);

            Log.v("Fonocoptero - ICS", "Declarados los pines físicos.");
            statLed.write(false);
            Thread.sleep(1000);
            statLed.write(true);
            Thread.sleep(1000);

            //Valores PWM de actuadores son 0 --> UAV desarmado.
            periodoPWMmotor_1 = 0;
            periodoPWMmotor_2 = 0;
            periodoPWMmotor_3 = 0;
            periodoPWMmotor_4 = 0;

            statLed.write(false);
            Thread.sleep(1000);
            statLed.write(true);
            Thread.sleep(1000);

            //ServiceControlActivity.isInterfaceLinked = true;
            Thread.currentThread().setPriority(10);
            Log.v("Fonocoptero ICS", "Interfaz configurada correctamente.");
            toast("IOIO conectado! "+ioio_.getImplVersion(IOIO.VersionType.HARDWARE_VER));
        }

        @Override
        public void loop() throws ConnectionLostException, InterruptedException {
            if(ioio_enable){
                Thread.sleep(20);//20ms, frecuencia de refresco de 50Hz aprox.
                executeActuatorActions();
            }
        }

        @Override
        public void disconnected() {
            super.disconnected();
            //ServiceControlActivity.isInterfaceLinked = false;
            ioio_enable = false;
        }
    }

    //Realiza la rutina de calibrado del acelerador electrónico.
    public static void calibrateECS() throws InterruptedException {
        if(!ECS_calibrado){
            periodoPWMmotor_1 = ESC_PWM_MINIMO_INICIAR;
            periodoPWMmotor_2 = ESC_PWM_MINIMO_INICIAR;
            periodoPWMmotor_3 = ESC_PWM_MINIMO_INICIAR;
            periodoPWMmotor_4 = ESC_PWM_MINIMO_INICIAR;
            Thread.sleep(2000);
            periodoPWMmotor_1 = ESC_PWM_MAXIMO;
            periodoPWMmotor_2 = ESC_PWM_MAXIMO;
            periodoPWMmotor_3 = ESC_PWM_MAXIMO;
            periodoPWMmotor_4 = ESC_PWM_MAXIMO;
            Thread.sleep(2000);
            periodoPWMmotor_1 = ESC_PWM_MINIMO_INICIAR;
            periodoPWMmotor_2 = ESC_PWM_MINIMO_INICIAR;
            periodoPWMmotor_3 = ESC_PWM_MINIMO_INICIAR;
            periodoPWMmotor_4 = ESC_PWM_MINIMO_INICIAR;
            Log.v("Fonocoptero ICS","Aceleradores electrónicos calibrados");
            ECS_calibrado = true;
        } else {
            Log.v("Fonocoptero ICS", "Los aceleradores electrónicos ya han sido calibrados previamente.");
        }
    }

    private void executeActuatorActions() throws ConnectionLostException {
        ESC_motor_1.setPulseWidth(periodoPWMmotor_1);
        ESC_motor_2.setPulseWidth(periodoPWMmotor_2);
        ESC_motor_3.setPulseWidth(periodoPWMmotor_3);
        ESC_motor_4.setPulseWidth(periodoPWMmotor_4);
    }

    public boolean armar_motores(boolean engineArming) throws InterruptedException {
        if(engineArming){
            esta_armado = true;
            calibrateECS();
            periodoPWMmotor_1 = ESC_PWM_MINIMO_RESPOSO;
            periodoPWMmotor_2 = ESC_PWM_MINIMO_RESPOSO;
            periodoPWMmotor_3 = ESC_PWM_MINIMO_RESPOSO;
            periodoPWMmotor_4 = ESC_PWM_MINIMO_RESPOSO;

            //ArmadoActivity.uavState = "Armado";
            Log.w("ICS", "ADVERTENCIA: El motor está armado. ¡RETIRE SUS MANOS DEL PROPULSOR!");
        }else{
            esta_armado = false;
            periodoPWMmotor_1 = 0;
            periodoPWMmotor_2 = 0;
            periodoPWMmotor_3 = 0;
            periodoPWMmotor_4 = 0;
            Log.v("ICS", "Motor desarmado.");
            //ArmadoActivity.uavState = "Desarmado";
        }
        return esta_armado;
        //return false;
    }


    public void comenzar_vuelo() throws InterruptedException {
        if(esta_armado){
            ioio_enable = true;
            obtener_posicion_inicial();
            estabilizar_vuelo();
        }
        else{
            toast("Los motores no se armaron correctamente.");
        }
    }

    public void obtener_posicion_inicial() throws InterruptedException {
        periodoPWMmotor_1 = 0;
        periodoPWMmotor_2 = 0;
        periodoPWMmotor_3 = 0;
        periodoPWMmotor_4 = 0;
    }


    public void estabilizar_vuelo() throws InterruptedException {
            periodoPWMmotor_1 = 0;
            periodoPWMmotor_2 = 0;
            periodoPWMmotor_3 = 0;
            periodoPWMmotor_4 = 0;
    }


    public void setEstadoActualComoZero(){
        yawZero = yawPitchRollVec[0];
        pitchZero = yawPitchRollVec[1];
        rollZero = yawPitchRollVec[2];
        elevationZero = absoluteElevation;
        longitudeZero = absoluteLongitude;
        latitudeZero = absoluteLatitude;



    }

}
