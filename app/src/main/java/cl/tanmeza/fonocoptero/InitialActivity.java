package cl.tanmeza.fonocoptero;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Sebastian on 14-12-2016.
 */
public class InitialActivity extends Activity {

    public Button button_prueba;
    public Button button_principal;

    /*
    public TextView mot_1,mot_2,mot_3,mot_4;
    //Se agrega un listener a los botones
    mot_1 = (TextView) findViewById(R.id.textView_motor1);
    mot_2 = (TextView) findViewById(R.id.textView_motor2);
    mot_3 = (TextView) findViewById(R.id.textView_motor3);
    mot_4 = (TextView) findViewById(R.id.textView_motor4);
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inicio);
        button_prueba = (Button) findViewById(R.id.button_prueba);
        button_principal = (Button) findViewById(R.id.button_principal);

        button_principal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(InitialActivity.this, FonoCopterActivity.class);
                InitialActivity.this.startActivity(myIntent);
            }
        });
        button_prueba.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(InitialActivity.this, PruebasActivity.class);
                InitialActivity.this.startActivity(myIntent);
            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
