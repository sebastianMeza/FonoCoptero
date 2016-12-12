package cl.tanmeza.fonocoptero;

/**
 * Created by Sebastian on 10-12-2016.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;

import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

public class InterfaceIOIOService extends IOIOService {
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
    private static final int PIN_IOIO_LED_NO_ARMADO = 5; //IOIO no armado
    private static final int PIN_IOIO_LED_ARMADO = 6; //IOIO armado
    private static final int PIN_IOIO_LED_LISTO = 7; //IOIO en modo de vuelo, inminente o en ejecución

    //Variables de salidas y entradas del IOIO
    private static DigitalOutput statLed;
    private static DigitalOutput Led_no_armado;//Rojo, indica que el avión entró a conteo regresivo de lanzamiento
    private static DigitalOutput Led_armado;//Amarillo, indica el estado de armado del motor
    private static DigitalOutput Led_listo;//Verde, indica si el avión está listo para volar
    private static PwmOutput ESC_motor_1;
    private static PwmOutput ESC_motor_2;
    private static PwmOutput ESC_motor_3;
    private static PwmOutput ESC_motor_4;

    //Variables de valores
    static Boolean led_encendido = false;//Esta variable NO sigue la lógica invertida del led STATUS del IOIO. La lógica se invierte en el método que actualiza el LED.
    static Boolean led_encendido_antes = false;
    static Boolean esta_listo = true; //¿Está listo para volar el UAV? (Todas las pruebas realizadas exitosamente)
    static Boolean esta_armado = false; //¿Está armado el motor?
    static Boolean servicio_enable = false; //Está funcionando el servicio con el IOIO?
    static Boolean seguro_motor = true;//Seguro de motor, si está activo, el PWM siempre será 0
    static Boolean seguro_motor_minimo = false;//Si está activo, sólo acelerará a un 10% de su capacidad total.
    static Boolean ECS_calibrado = false;//¿Está calibrado el Acelerador Electrónico?

    private static long periodoPWMmotor_1;
    private static long periodoPWMmotor_2;
    private static long periodoPWMmotor_3;
    private static long periodoPWMmotor_4;

    //Si el IOIO llegara a desconectarse, los valores de altitud y velocidad pueden obtenerse desde el GPS mientras se
    private static boolean IOIO_emergencia = false;

/*
    //variables por borrar
    private static final int AILERON_CENTER_PWM_PERIOD = 1500; //Período en el que los alerones se encuentran a 0°
    private static final int AILERON_MAX_PWM_PERIOD = 2860; //Alerón izquierdo deflectado al máximo hacia abajo, el derecho hacia arriba
    private static final int AILERON_MIN_PWM_PERIOD = 1130; //Alerón izquierdo deflectado al máximo hacia arriba, el derecho hacia abajo
    private static final int ENGINE_SETUP_MINIMUM_PWM_PERIOD = 750; //Este valor sólo se utiliza para incializar el acelerador electrónico
    private static final int ENGINE_MINIMUM_THROTTLE_PWM_PERIOD = 800; //Período donde el motor se encuentra en reposo
    private static final int ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD = 1700; //Valor máximo de inicialización del acelerador electrónico, y período donde el motor desarrolla su máxima potencia
    private static final int ENGINE_MAXIMUM_LIMITED_THROTTLE_PWM_PERIOD = 900;//Valor máximo del período para el motor con limitador activo, aprox. un 10% de su capacidad.

    private static final int IOIO_THROTTLE_PIN = 1;
    private static final int IOIO_LEFT_AILERON_PIN = 2;
    private static final int IOIO_RIGHT_AILERON_PIN = 3;
    private static final int IOIO_LAUNCHING_LED_PIN = 5; //IOIO no armado
    private static final int IOIO_ARMED_LED_PIN = 6; //IOIO armado
    private static final int IOIO_READY_LED_PIN = 7; //IOIO en modo de vuelo, inminente o en ejecución
    private static final int IOIO_APT_SENSOR_I2C_PORT = 2; //Puerto I2C del IOIO a usar, por defecto 2 (pines 25 y 26)
    private static final int IOIO_AIRSPEED_SENSOR_PIN = 34;
    private static final int IOIO_APT_I2C_ADDRESS = (byte)0x60;//Dirección del sensor A.P.T.
    //Sensor de presión dinámica
    private static final int AIRSPEED_SENSOR_VOLTS_TO_PASCAL = 819;
    private static final float AIRSPEED_SENSOR_VOLTAGE_OFFSET = 2.5f;

    //Variables de salidas y entradas del IOIO
    private static DigitalOutput launchLed;//Rojo, indica que el avión entró a conteo regresivo de lanzamiento
    private static DigitalOutput armedLed;//Amarillo, indica el estado de armado del motor
    private static DigitalOutput readyLed;//Verde, indica si el avión está listo para volar
    private static PwmOutput leftAileronControl;
    private static PwmOutput rightAileronControl;
    private static PwmOutput engineControl;
    private static AnalogInput airspeedSensor;
    private static TwiMaster aptSensor;
    private static PulseInput manualControlPWM;

    //Arreglos de bytes para la comunicación I2C con el sensor de altitud, presión estática, y temperatura
    private static byte[] request;
    private static byte[] response;
    //Variables varias, valores de PWM, estado del led status de IOIO, etcétera.
    static Boolean ledOn = false;//Esta variable NO sigue la lógica invertida del led STATUS del IOIO. La lógica se invierte en el método que actualiza el LED.
    static Boolean ledOn_old = false;
    static Boolean isReady = true; //¿Está listo para volar el UAV? (Todas las pruebas realizadas exitosamente)
    static Boolean isArmed = false; //¿Está armado el motor?
    static Boolean isLaunching = false; //¿El UAV está en cuenta regresiva para despegar?
    static Boolean icsEnabled = false; //Está funcionando el servicio con el IOIO?
    private static Boolean isManualMode = false;//¿Modo de mando manual?

    static Boolean engineSafe = true;//Seguro de motor, si está activo, el PWM siempre será 0
    static Boolean aileronSafe = true;//Si está activo, los alerones no se pueden (simula armado/desarmado alerones)
    static Boolean engineMinimalThrottle = false;//Si está activo, sólo acelerará a un 10% de su capacidad total.
    static Boolean isECSCalibrated = false;//¿Está calibrado el Acelerador Electrónico?
    private static long leftAileronPWMPeriod=AILERON_CENTER_PWM_PERIOD;
    private static long rightAileronPWMPeriod=AILERON_CENTER_PWM_PERIOD;
    private static long elevatorPWMPeriod=0;
    private static long enginePWMPeriod;
    private static long pitchDelta=0, oldPitchDelta=0;
    private static Float altDelta =0.0f;
    private static Float oldAlt = 0.0f, newAlt = 0.0f; //Altitudes ABSOLUTAS del sensor
    private static Float refAlt = 0.0f;
    private static Float relativeAlt = 0.0f;//Altitud RELATIVA, que usa el UAV en base a refAlt
    private static Float ambientTemp = 0.0f;//Temperatura ambiente medido por el sensor A.P.T.
    private static Float gpsAltitude = 0.0f;//Altitud GPS en caso de emergencia
    private static Double airspeed = 0.0d;//Airspeed real del UAV según sensor de presión dinámica
    private static Float gpsAirspeed = 0.0f;//Airspeed aparente según GPS para caso de emergencia
    private static Double throttleLevel = 0.0;//Porcentaje de válvula (acelerador)
    */

    //Si el IOIO llegara a desconectarse, los valores de altitud y velocidad pueden obtenerse desde el GPS mientras se
    private static boolean ioioEmergency = false;


    private static InterfaceIOIOService instanciaIOIOS;

    public static InterfaceIOIOService getIIOIOSInstance() {
        if (instanciaIOIOS == null)
            instanciaIOIOS = new InterfaceIOIOService();
        return instanciaIOIOS;
    }

    public InterfaceIOIOService() {
        //TODO analizar conveniencia de patrón singleton en IIOIOS
    } /*Necesitamos que el constructor sea private para poder utilizar el patrón de diseño Singleton*/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected IOIOLooper createIOIOLooper() {
        return new BaseIOIOLooper() {
            @Override
            protected void setup() throws ConnectionLostException, InterruptedException {
                Log.i("Fonocoptero - In-IOIO-Ser", "Comienza la configuración del IOIO");
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


                //Led_no_armado = ioio_.openDigitalOutput(PIN_IOIO_LED_NO_ARMADO);
                //Led_armado = ioio_.openDigitalOutput(PIN_IOIO_LED_ARMADO);
                //Led_listo = ioio_.openDigitalOutput(PIN_IOIO_LED_LISTO);

                Log.i("Fonocoptero - ICS","Declarados los pines físicos.");

                statLed.write(false);
                Thread.sleep(1000);
                statLed.write(true);
                Thread.sleep(1000);

                //CALIBRAR SENSORES

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
                servicio_enable = true;
                Thread.currentThread().setPriority(10);
                //TODO play a nice sound when setup is completed
                Log.i("Fonocoptero ICS","Interfaz configurada correctamente.");
            }
            @Override
            public void loop() throws ConnectionLostException, InterruptedException {
                if(servicio_enable){
                    Thread.sleep(20);//20ms, frecuencia de refresco de 50Hz aprox.
                    executeActuatorActions();
                }
            }

            @Override
            public void disconnected() {
                super.disconnected();
                //ServiceControlActivity.isInterfaceLinked = false;
                servicio_enable = false;
                stopSelf();
            }

        };
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //LocalBroadcastManager.getInstance(this).registerReceiver(ledStatBCReceiver, new IntentFilter("ToggleButtonStatus"));
        Toast svcToast;
        svcToast = Toast.makeText(getApplicationContext(), "[ICS]: Servicio IOIO creado", Toast.LENGTH_SHORT);
        svcToast.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //elevatorPWMPeriod = 0;
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(ledStatBCReceiver);
        servicio_enable = false;
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "[ICS]: Servicio IOIO iniciado", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }

    /*
    //Comanda al UAV a realizar un giro hacia la izquierda. @param degrees Número de grados del giro deseado.
    //El valor debe ser un número entre 0 y 15 por limitaciones de diseño.
    public static void turnLeft(double degrees) { //Implica package-local
        if (degrees < -30) degrees = -30;
        else if (degrees > 30) degrees = 30;
        leftAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD - 370 * degrees / 30 - elevatorPWMPeriod);
        rightAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD - 370 * degrees / 30 + elevatorPWMPeriod);
    }

    //Comanda al UAV a realizar un giro hacia la derecha. @param degrees Número de grados del giro deseado.
    //El valor debe ser un número entre 0 y 15 por limitaciones de diseño.
    public static void turnRight(double degrees) { //Implica package-local
        if (degrees < -30) degrees = -30;
        else if (degrees > 30) degrees = 30;
        leftAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD + 370 * degrees / 30 - elevatorPWMPeriod);
        rightAileronPWMPeriod = Math.round(AILERON_CENTER_PWM_PERIOD + 370 * degrees / 30 + elevatorPWMPeriod);
    }

    //Comanda al UAV a elevarse o descender sus elevadores en la cantidad de grados especificada.
    //@param degrees la cantidad de grados que se deben deflectar los alerones. Valor debe ser entre -15 y 15.
    public static void controlPitch(double degrees) {
        if (degrees < -30) degrees = -30;
        else if (degrees > 30) degrees = 30;
        elevatorPWMPeriod = Math.round(370 * degrees / 30); //Si grados > 0, asciende con respecto al alerón der, el izq hay que invertir el signo
        calculateElevonPWM();
    }

    //Controla las RPM del motor propulsor del UAV. @param rpmFactor Número entre 0 y 1 indicando el porcentaje de RPM del motor, donde
    //0 indica que el motor está en reposo y 1 que el motor estará a las máximas RPM posibles.
    public static void controlEngine(double rpmFactor) {
        //    Log.i("ICS","RPMFactor: " + String.valueOf(rpmFactor));
        if(rpmFactor>1){
            rpmFactor = 1.0;
        } else if(rpmFactor < 0.0){
            rpmFactor = 0.0;
        }
        if (engineSafe) { //Con el seguro activado el motor no se arma.
            enginePWMPeriod = 0;
            throttleLevel = 0.0;
        }else if(engineMinimalThrottle){ //El limitador está activado

            //Si la configuración del Acelerador Electrónico dejó bajo el período mínimo de acelerador al motor, hay que
            //restaurarlo
            if(enginePWMPeriod < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;

            long calculatedPWM = Math.round((double)ENGINE_MINIMUM_THROTTLE_PWM_PERIOD + rpmFactor * 900.0);
            //    Log.i("ICS","CalculatedPWM: " + String.valueOf(calculatedPWM));
            //Limita el rango de PWM que se pueden utilizar
            if (calculatedPWM < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;
                throttleLevel = 0.0;
                //        Log.i("ICS","calculado < minimo");
            } else if (calculatedPWM > ENGINE_MAXIMUM_LIMITED_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MAXIMUM_LIMITED_THROTTLE_PWM_PERIOD;
                throttleLevel = 0.1;
                //        Log.i("ICS","calculado > limitador");
            } else {
                enginePWMPeriod = calculatedPWM;
            }
        }else{
            //Si la configuración del Acelerador Electrónico dejó bajo el período mínimo de acelerador al motor, hay que
            //restaurarlo
            if(enginePWMPeriod < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;

            long calculatedPWM = Math.round((double)ENGINE_MINIMUM_THROTTLE_PWM_PERIOD + rpmFactor * 900.0);
            //    Log.i("ICS","CalculatedPWM: " + String.valueOf(calculatedPWM));

            //Limita el rango de PWM que se pueden utilizar
            if (calculatedPWM < ENGINE_MINIMUM_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MINIMUM_THROTTLE_PWM_PERIOD;
                throttleLevel = 0.0;
                //        Log.i("ICS","calculado < minimo");
            } else if (calculatedPWM > ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD) {
                enginePWMPeriod = ENGINE_MAXIMUM_THROTTLE_PWM_PERIOD;
                throttleLevel = 1.0;
                //        Log.i("ICS","calculado > maximo");
            } else {
                enginePWMPeriod = calculatedPWM;
                throttleLevel = rpmFactor;
            }
        }

    }
    */

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
            Log.i("Fonocoptero ICS","Aceleradores electrónicos calibrados");
            ECS_calibrado = true;
        } else {
            Log.i("Fonocoptero ICS", "Los aceleradores electrónicos ya han sido calibrados previamente.");
        }
    }

    /*Informa al Servicio de Control de Vuelo que la comunicación con la placa controladora se ha
    perdido, y por ende, es necesario dirigirse inmediatamente a la base, utilizando como apoyo
    los datos del sistema GPS.
    */
    private static void invokeIOIOEmergencyState(){
        //TODO Evaluar si es conveniente desplazar esta función a la clase Emergency como método estático
    }

/*
    //Combina los valores PWM de elevador y alerón para generar la salida final para cada alerón
    private static void calculateElevonPWM(){
        pitchDelta = elevatorPWMPeriod;
        long tempDelta = pitchDelta - oldPitchDelta;
        rightAileronPWMPeriod = rightAileronPWMPeriod + tempDelta;
        leftAileronPWMPeriod = leftAileronPWMPeriod - tempDelta;

        //Limitando el período para no sobresforzar los alerones ni su mecanismo
        if(leftAileronPWMPeriod > AILERON_MAX_PWM_PERIOD){
            leftAileronPWMPeriod = AILERON_MAX_PWM_PERIOD;
        } else if(leftAileronPWMPeriod < AILERON_MIN_PWM_PERIOD){
            leftAileronPWMPeriod = AILERON_MIN_PWM_PERIOD;
        }

        if(rightAileronPWMPeriod > AILERON_MAX_PWM_PERIOD){
            rightAileronPWMPeriod = AILERON_MAX_PWM_PERIOD;
        } else if (rightAileronPWMPeriod < AILERON_MIN_PWM_PERIOD){
            rightAileronPWMPeriod = AILERON_MIN_PWM_PERIOD;
        }

        oldPitchDelta = pitchDelta;
    }
    */

    /**
     * Actualiza los actuadores con los nuevos valores computados
     */
    private void executeActuatorActions() throws ConnectionLostException {

        /*
        Estas comprobaciones de engineSafe y aileronSafe son redundantes, en caso de falla de programación
        de los valores del período PWM para cada uno de los actuadores por alguno de los métodos que
        gobierna directamente al motor o a los servomotores.
         */
        if(seguro_motor){
            // Log.i("ICS","Engine safe is on, pulse width is 0");
            ESC_motor_1.setPulseWidth(0);
            ESC_motor_2.setPulseWidth(0);
            ESC_motor_3.setPulseWidth(0);
            ESC_motor_4.setPulseWidth(0);
        }else{
            ESC_motor_1.setPulseWidth(periodoPWMmotor_1);
            ESC_motor_2.setPulseWidth(periodoPWMmotor_2);
            ESC_motor_3.setPulseWidth(periodoPWMmotor_3);
            ESC_motor_4.setPulseWidth(periodoPWMmotor_4);
        }

    }

    //Activa o desactiva el seguro de motor. Si el seguro está activado, el período del motor siempre será 0.
    //@param active <b>true</b> si se desea activar el seguro, <b>false</b> si se desea desactivar
    public static void setEngineSafe(boolean active){
        if(active){
            periodoPWMmotor_1 = 0;
            periodoPWMmotor_2 = 0;
            periodoPWMmotor_3 = 0;
            periodoPWMmotor_4 = 0;
            seguro_motor = true;
            esta_armado = false;
        }else{
            seguro_motor = false;
        }

    }

    //Activa o desactiva el limitador del acelerador de motor. Si está activo, el motor sólo acelerará hasta un 10% de su capacidad máxima.
    //@param active <b>true</b> si se desea activar el limitador, <b>false</b> si se desea desactivar
    public static void setEngineMinimalThrottle(boolean active){
        seguro_motor_minimo = active;
        Log.i("ICS","Aceleracion minima: "+String.valueOf(active));
    }


    public static boolean armar_motores(boolean engineArming) throws InterruptedException {
        if(engineArming){
            if(seguro_motor){
                esta_armado = false;
                periodoPWMmotor_1 = 0;
                periodoPWMmotor_2 = 0;
                periodoPWMmotor_3 = 0;
                periodoPWMmotor_4 = 0;
                if(engineArming){
                    Log.e("ICS","Imposible armar motor: Seguro de motor está activado");
                }
            }else{
                esta_armado = true;
                calibrateECS();
                periodoPWMmotor_1 = ESC_PWM_MINIMO_RESPOSO;
                periodoPWMmotor_2 = ESC_PWM_MINIMO_RESPOSO;
                periodoPWMmotor_3 = ESC_PWM_MINIMO_RESPOSO;
                periodoPWMmotor_4 = ESC_PWM_MINIMO_RESPOSO;

                //ArmadoActivity.uavState = "Armado";
                Log.w("ICS","ADVERTENCIA: El motor está armado. ¡RETIRE SUS MANOS DEL PROPULSOR!");
            }
        }else{
            esta_armado = false;
            periodoPWMmotor_1 = 0;
            periodoPWMmotor_2 = 0;
            periodoPWMmotor_3 = 0;
            periodoPWMmotor_4 = 0;
            Log.i("ICS", "Motor desarmado.");
            //ArmadoActivity.uavState = "Desarmado";
        }
        return esta_armado;


    }






}