package cl.tanmeza.fonocoptero;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import ioio.lib.api.PulseInput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.TwiMaster;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

/**
 * Created by Sebastian on 28-11-2016.
 */
public class InterfaceVuelo extends IOIOService {
    //Constantes
    private static final int IOIO_PIN_forward_right = 1;
    private static final int IOIO_PIN_forward_left = 2;
    private static final int IOIO_PIN_bottom_right = 3;
    private static final int IOIO_PIN_bottom_left = 4;

    private static PwmOutput forward_rightControl;
    private static PwmOutput forward_leftControl;
    private static PwmOutput bottom_rightControl;
    private static PwmOutput bottom_leftControl;

    private static InterfaceVuelo instanciaIV;

    public static InterfaceVuelo getICSInstance() {
        if (instanciaIV == null)
            instanciaIV = new InterfaceVuelo();
        return instanciaIV;
    }

    public InterfaceVuelo() {
        //TODO analizar conveniencia de patrón singleton en ICS
    } /*Necesitamos que el constructor sea private para poder
    utilizar el patrón de diseño Singleton*/

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


    @Override
    public void onCreate() {
        super.onCreate();
        //LocalBroadcastManager.getInstance(this).registerReceiver(ledStatBCReceiver, new IntentFilter("ToggleButtonStatus"));
        Toast toast_servicio;
        toast_servicio = Toast.makeText(getApplicationContext(), "[iv]: Servicio IOIO creado", Toast.LENGTH_SHORT);
        toast_servicio.show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(getApplicationContext(), "[iv]: Servicio IOIO iniciado", Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent, flags, startId);
    }





}


