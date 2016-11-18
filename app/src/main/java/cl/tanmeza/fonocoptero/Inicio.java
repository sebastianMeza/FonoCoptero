package cl.tanmeza.fonocoptero;

import android.app.Activity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.*;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class Inicio extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {



    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));



    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        //Log.v("Menu", "position: " + position);
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        switch (position) {
            default:
                fragmentManager.beginTransaction()
                        .replace(R.id.container,Section_1.newInstance(position + 1))
                .commit();
                break;
            case 0:
                fragmentManager.beginTransaction()
                        .replace(R.id.container,Section_1.newInstance(position + 1))
                .commit();
                break;
            case 1:
                fragmentManager.beginTransaction()
                        .replace(R.id.container, Section_2.newInstance(position + 1))
                .commit();
                break;
            case 2:
                fragmentManager.beginTransaction()
                        .replace(R.id.container,Section_3.newInstance(position + 1))
                .commit();
                break;
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.inicio, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class Section_1 extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        private SensorManager mSensorManager;

        ListView listview_1;
        List<String> list_sensores;
        ArrayAdapter<String> adapter_sensorslist;

        public static Section_1 newInstance(int sectionNumber) {
            Section_1 fragment = new Section_1();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public Section_1() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_1, container, false);

            listview_1 = (ListView) rootView.findViewById(R.id.listView_1);
            list_sensores = new ArrayList<>();

            mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            int count_sensor = 0;
            for (Sensor sensor : sensors) {
                count_sensor++;
                list_sensores.add(count_sensor+". "+sensor.getName());
                //Log.i("Sensores", "senk: " + sensor.getName());
            }
            adapter_sensorslist = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, list_sensores);
            listview_1.setAdapter(adapter_sensorslist);



            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((Inicio) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

    public static class Section_2 extends Fragment implements SensorEventListener {

        private static final String ARG_SECTION_NUMBER = "section_number";


        private SensorManager sensorManager;
        private Sensor s_presion;

        //for accelerometer values
        TextView outputX;
        TextView outputY;
        TextView outputZ;

        //for orientation values
        TextView outputX2;
        TextView outputY2;
        TextView outputZ2;


        public static Section_2 newInstance(int sectionNumber) {
            Section_2 fragment = new Section_2();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public Section_2() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_2, container, false);


            sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            s_presion = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

            outputX = (TextView) rootView.findViewById(R.id.textView_2_01);
            outputY = (TextView) rootView.findViewById(R.id.textView_2_02);
            outputZ = (TextView) rootView.findViewById(R.id.textView_2_03);

            outputX2 = (TextView) rootView.findViewById(R.id.textView_2_04);
            outputY2 = (TextView) rootView.findViewById(R.id.textView_2_05);
            outputZ2 = (TextView) rootView.findViewById(R.id.textView_2_06);

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((Inicio) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

        @Override
        public void onResume() {
            super.onResume();
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), sensorManager.SENSOR_DELAY_GAME);
        }

        @Override
        public void onStop() {
            super.onStop();
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
            sensorManager.unregisterListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        }

        public void onSensorChanged(SensorEvent event) {
            synchronized (this) {
                switch (event.sensor.getType()){
                    case Sensor.TYPE_ACCELEROMETER:
                        outputX.setText("x:"+Float.toString(event.values[0]));
                        outputY.setText("y:"+Float.toString(event.values[1]));
                        outputZ.setText("z:"+Float.toString(event.values[2]));
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        outputX2.setText("x:"+Float.toString(event.values[0]));
                        outputY2.setText("y:"+Float.toString(event.values[1]));
                        outputZ2.setText("z:"+Float.toString(event.values[2]));
                        break;

                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    }


    public static class Section_3 extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";

        public static Section_3 newInstance(int sectionNumber) {
            Section_3 fragment = new Section_3();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public Section_3() {
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_3, container, false);

            Button button= (Button) rootView.findViewById(R.id.button_play);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity() , QuadcopterActivity.class);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                }
            });

            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((Inicio) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }


}
