package cl.tanmeza.fonocoptero;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
import android.content.SharedPreferences;
import android.widget.ToggleButton;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;

public class QuadcopterActivity extends IOIOActivity {
	public static final long UI_REFRESH_PERIOD_MS = 250;
	public static final String PREFS_NAME = "MyPrefsFile";
	public static final String PREFS_ID_LAST_IP = "lastServerIP";


	PwmOutput motor_1;
	PwmOutput motor_2;
	PwmOutput motor_3;
	PwmOutput motor_4;


	private ToggleButton button_;

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_action);
        // Get UI elements.
        //serverIpEditText = (EditText)findViewById(R.id.serverIpEditText);
        //connectToServerCheckBox = (CheckBox)findViewById(R.id.connectToServerCheckBox);
        
        // In the "server IP" field, insert the last used IP address.
        //SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        //String lastIP = settings.getString(PREFS_ID_LAST_IP, "192.168.0.8");
        //serverIpEditText.setText(lastIP);
		button_ = (ToggleButton) findViewById(R.id.button);
        // Create the main controller.
        mainController = new MainController(this);
    }

    /*
    // Deactivate some buttons.
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	if(keyCode == KeyEvent.KEYCODE_CALL)
    		return true;
    	else
    		return super.onKeyDown(keyCode, event);
    }
    */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if (keyCode == KeyEvent.KEYCODE_BACK ){
			//Intent intent_2 = new Intent(this , Inicio.class);
			//startActivity(intent_2);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
    protected void onResume(){
		super.onResume();
		// Prevent sleep mode.
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Start the main controller.
		try{
			mainController.start();
		}catch (Exception e){
			Toast.makeText(this, "The USB transmission could not start.", Toast.LENGTH_SHORT).show();
			e.printStackTrace();
		}

		/*
		// Allow the user starting the TCP client.
		serverIpEditText.setEnabled(true);
		// Connect automatically to the computer.
		// This way, it is possible to start the communication just by plugging
		// the ADK (if the auto-start of this application is checked).
		connectToServerCheckBox.setChecked(true);
		onConnectToServerCheckBoxToggled(null);
		*/
    }

    @Override
    protected void onPause(){
		super.onPause();
    	// Reallow sleep mode.
		getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Stop the main controller.
		mainController.stop();
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	// Save the server IP.
    	//SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        //SharedPreferences.Editor editor = settings.edit();
        //editor.putString(PREFS_ID_LAST_IP, serverIpEditText.getText().toString());
        //editor.commit();
    }
    
    public void onConnectToServerCheckBoxToggled(View v){
		/*
		// Connect.
    	if(connectToServerCheckBox.isChecked()){
    		mainController.startClient(serverIpEditText.getText().toString());
    		serverIpEditText.setEnabled(false);
    	}
    	else{ // Disconnect.
    		serverIpEditText.setEnabled(true);
    		mainController.stopClient();
    	}
    	*/
    }

	/*IOIO CONEXION*/
	class Looper extends BaseIOIOLooper {
		//private DigitalOutput led_;
		boolean probar;

		@Override
		protected void setup() throws ConnectionLostException {
			showVersions(ioio_, "IOIO conectado!");
			motor_1 = ioio_.openPwmOutput(1, 50);
			motor_2 = ioio_.openPwmOutput(2, 50);
			motor_3 = ioio_.openPwmOutput(3, 50);
			motor_4 = ioio_.openPwmOutput(4, 50);
			probar = true;

			//led_ = ioio_.openDigitalOutput(0, true);
			enableUi(true);
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			if(probar) {
				//led_.write(!button_.isChecked());
				motor_1.setPulseWidth(200);
				toast("IOIO a 200");
				Thread.sleep(5000);
				motor_1.setPulseWidth(40);
				toast("IOIO a 40");
				probar = false;
			}
			Thread.sleep(5000);
		}

		@Override
		public void disconnected() {
			enableUi(false);

			motor_1.close();
			motor_2.close();
			motor_3.close();
			motor_4.close();
			toast("IOIO desconectado");
		}

		@Override
		public void incompatible() {
			showVersions(ioio_, "Version de firmware incompatible!");
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}

	private void showVersions(IOIO ioio, String title) {
		toast(String.format("%s\n" + "IOIOLib: %s\n" + "Application firmware: %s\n" + "Bootloader firmware: %s\n" + "Hardware: %s",
				title, ioio.getImplVersion(IOIO.VersionType.IOIOLIB_VER), ioio.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER),
				ioio.getImplVersion(IOIO.VersionType.BOOTLOADER_VER), ioio.getImplVersion(IOIO.VersionType.HARDWARE_VER)));
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

	private int numConnected_ = 0;

	private void enableUi(final boolean enable) {
		// This is slightly trickier than expected to support a multi-IOIO use-case.
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (enable) {
					if (numConnected_++ == 0) {
						button_.setEnabled(true);
					}
				} else {
					if (--numConnected_ == 0) {
						button_.setEnabled(false);
					}
				}
			}
		});
	}



    //private CheckBox connectToServerCheckBox;
    //private EditText serverIpEditText;
    private MainController mainController;
}
