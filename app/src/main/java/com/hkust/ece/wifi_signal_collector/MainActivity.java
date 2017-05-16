package com.hkust.ece.wifi_signal_collector;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.Vibrator;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hkust.ece.locat;
import com.hkust.ece.route;
import com.hkust.ece.wifi_database.DatabaseGenerator;
import com.hkust.ece.wifi_localisation.CustomWiFISignal;
import com.hkust.ece.wifi_localisation.LocationService;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import au.com.bytecode.opencsv.CSVWriter;
import utility.Location;



public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2,View.OnTouchListener {
    private static final String TAG = "OCVSample::Activity";

    SharedPreferences mPrefs;

    private CameraBridgeViewBase mOpenCvCameraView;

    private boolean              mIsColorSelected = false;
    private Mat mRgba;
    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat mSpectrum;
    private Size SPECTRUM_SIZE;
    private Scalar CONTOUR_COLOR;

    public static Context mContext;
    public static double current_x;
    public static double current_y;
    public static String routeResult = null;
    public static int toRoomNo;
    public static int showXYZ = 1;
    public static int[] moveBuffer = {0,0};

    public final static int upMove = 1;
    public final static int downMove = 2;
    public final static int leftMove = 3;
    public final static int rightMove = 4;
    public final static int stopMove = 0;
    public int currentstate = upMove;
    int camcenterx = 640;
    int camcentery = 360;

    //public static Scalar mHsvColor = new Scalar(131.078125, 155.796875, 74.21875, 0.0);

    imageThread mimagethread = new imageThread();

    public static int onRevFlag = 0;

    private final Lock lock = new ReentrantLock();

    private final Condition onOnReceiveLock = lock.newCondition();
    private final Condition onThreadLock = lock.newCondition();
    //private static final String TAG = "MainActivity";


    /*
    static {
        if(!OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV not loaded");
        } else {
            Log.d(TAG, "OpenCV loaded");
        }
    }
    */


    private static int MIN_SCAN_COUNT = 50;
    private static String DATA_FOLDER = "wifi_data";

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    public BluetoothDevice mbluetoothdevice;


    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Set<BluetoothDevice> pairedDevices;
    ;
    int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter mBluetoothAdapter ;
    ArrayAdapter<String> mArrayAdapter ;
    ListView pairedListView ;

    private Vibrator vibrator;

    public static WifiManager wifiManager;
    private WifiReceiver wifiReceiver;

    private LocationService locationService;
    Hashtable<String, AccessPoint> results;

    public route mRoute;

    // UI references.
    private EditText mFloorView;
    private EditText mXView;
    private EditText mYView;
    private EditText roomNo;

    public Thread run;

    private Button mScan;
    private Button mSave;
    private Button mGenerate;
    private Button mAnalyse;
    private Button mLoad;
    private Button mCheck;
    private Button our_check;



    public final Object obj = new Object();

    private int currentScanCounter;

    private String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();

    public static int[][] maze;

    int flagbuffer0=0;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    mimagethread.start();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //colorinit();


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "Bluetooth is not supported on this hardware platform",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        String stInfo = mBluetoothAdapter.getName() + "\n" +
                mBluetoothAdapter.getAddress();
        Toast.makeText(this,
                stInfo,
                Toast.LENGTH_LONG).show();
        mArrayAdapter = new ArrayAdapter<String>(this, R.layout.list,R.id.title_paired_devices);
        pairedListView = (ListView) findViewById(R.id.paired_devices);

        pairedListView.setOnItemClickListener(mDeviceClickListener);

        findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);


        Button scan = (Button) findViewById(R.id.Scan);
        scan.setOnClickListener(bluetoothscan);

        /*
                 0   1   2
            0: [100 200 300]
            1: [ 0  400  0 ]
            2: [ 0  500  0 ]
            maze[y][x]
         */
        maze = new int[][]{{100, 200, 300}, {0, 400, 0}, {0, 500, 0}};

        mContext = getApplicationContext();
        mPrefs = mContext.getSharedPreferences("DATA",mContext.MODE_PRIVATE);

        mFloorView = (EditText) findViewById(R.id.f_floor);
        mXView = (EditText) findViewById(R.id.f_x);
        mYView = (EditText) findViewById(R.id.f_y);
        roomNo = (EditText) findViewById(R.id.room_num);

        mScan = (Button) findViewById(R.id.b_scan);
        mSave = (Button) findViewById(R.id.b_save);
        mGenerate = (Button) findViewById(R.id.b_generate);
        mAnalyse = (Button) findViewById(R.id.b_analyse);
        mLoad = (Button) findViewById(R.id.b_load);
        mCheck = (Button) findViewById(R.id.b_check);
        our_check = (Button) findViewById(R.id.our_check);

        mScan.setOnClickListener(scanButtonClickListener);
        mSave.setOnClickListener(saveButtonClickListener);
        mGenerate.setOnClickListener(generateButtonClickListener);
        mAnalyse.setOnClickListener(analyseButtonClickListener);
        mLoad.setOnClickListener(loadButtonClickListener);
        mCheck.setOnClickListener(checkButtonClickListener);
        our_check.setOnClickListener(ourCheckButtonClickListener);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wifiReceiver = new WifiReceiver();
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        results = new Hashtable<String, AccessPoint>();
    }

    protected void onPause() {
        unregisterReceiver(wifiReceiver);
        currentScanCounter = -2;
        super.onPause();

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    protected void onResume() {
        super.onResume();
        toggleAllButtons(true);
        currentScanCounter = -2;
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
   /* @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode == Activity.RESULT_OK){
                // setup();
            }else{
                Toast.makeText(this,
                        "BlueTooth NOT enabled",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }*/
    @Override
    protected void onStart() {
        super.onStart();

        //Turn ON BlueTooth if it is OFF
       /* if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);}

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        if(mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE)
        {  Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);}*/

        setup();
    }
    private void setup() {
        // textStatus.setText("setup()");

        cancelaccept();
        mInsecureAcceptThread = new AcceptThread();
        mInsecureAcceptThread.start();
    }



    private OnClickListener checkButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            if (locationService == null) {
                Toast.makeText(getApplicationContext(), "Load database first", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Please wait while getting signal data", Toast.LENGTH_SHORT).show();
                currentScanCounter = -1;
                wifiManager.startScan();
                System.out.println("underScanStart");
            }
        }
    };
    public class TaskRoute implements Runnable{
        public TaskRoute(){
            Integer toRoomNo = Integer.parseInt(roomNo.getText().toString());
            mRoute = new route(toRoomNo);

        }
        public void run(){
            Looper.prepare();
            int i = 0;
            while(routeResult != "Arrived"){
                System.out.println(routeResult);
                carMovementControl();
                System.out.println("Looping time " + Integer.toString(i++));

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            routeResult = "0";
            System.out.println("end while");
        }

        public void sendCarMovement(String mLetter){
            if(mConnectedThread!=null) {
                byte[] bytesToSend = mLetter.getBytes();
                mConnectedThread.write(bytesToSend);
            }
        }

        private void carMovementControl(){
            currentScanCounter = -1;
            showXYZ = -1;
            wifiManager.startScan();
            Toast.makeText(getApplicationContext(), "Start Scan Started", Toast.LENGTH_SHORT).show();
            synchronized (obj){
                try{
                    obj.wait();
                }catch(InterruptedException ex){
                    System.out.println("Dead in sync wait");
                }
            }
            routeResult = mRoute.calRoute();
            if(routeResult == "Arrived")return;

            compareMove(Integer.parseInt(routeResult));
            this.sendCarMovement("S");
        }

        private void compareMove(int next){
            /*

                    U
                L   S   R
                    D

                U = Go Up
                D = Go Down
                L = Go Left
                R = Go Right
                S = Stop
             */
                    /*
                 0   1   2
            0: [100 200 300]
            1: [ 0  400  0 ]
            2: [ 0  500  0 ]
            maze[y=i][x=j]
         */
            moveBuffer[0] = moveBuffer[1];//moveBuffer[0]=currentLocation,1 is next;
            moveBuffer[1] = next;
            if(moveBuffer[0]==moveBuffer[1]){
                flagbuffer0=0;
                return;
            }
            int currenti=0;int currentj=0;
            for ( int i = 0; i < 3; ++i ) {
                for ( int j = 0; j < 3; ++j ) {
                    if ( maze[i][j] == moveBuffer[0] ) {
                        currenti=i;
                        currentj=j;
                        break;// Found the correct i,j - print them or return them or whatever
                    }
                }
            }
            int nexti=0;int nextj=0;
            for ( int i = 0; i < 3; ++i ) {
                for ( int j = 0; j < 3; ++j ) {
                    if ( maze[i][j] == moveBuffer[1] ) {
                        nexti=i;
                        nextj=j;
                        break;// Found the correct i,j - print them or return them or whatever
                    }
                }
            }

            if( currenti== nexti){
                if(nextj > currentj && flagbuffer0==0){
                    /*uiShowToast("Going right");
                    System.out.println("Going right");
                    this.sendCarMovement("R");*/
                    uiShowToast("Going right");
                    this.sendCarMovement("R");
                    try {
                        Thread.sleep(550);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.sendCarMovement("U"); flagbuffer0=1;
                }
                else if(nextj < currentj && flagbuffer0==0){
                    uiShowToast("Going left");
                    this.sendCarMovement("L");
                    try {
                        Thread.sleep(550);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.sendCarMovement("U"); flagbuffer0=1;
                }
                else if(flagbuffer0==1){
                    this.sendCarMovement("U");
                }
            }
            if( currentj== nextj){
                if(nexti > currenti){
                    /*uiShowToast("Going down");
                    System.out.println("Going down");
                    this.sendCarMovement("D");*/
                    uiShowToast("Going down");
                    flagbuffer0=0;
                    this.sendCarMovement("L");
                    try {
                        Thread.sleep(1100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.sendCarMovement("U");

                }
                else if(nexti < currenti){
                    /*uiShowToast("Going up");
                    System.out.println("Going up");
                    this.sendCarMovement("U");*/
                    uiShowToast("Going up");
                    flagbuffer0=0;
                    this.sendCarMovement("U");
                    try {
                        Thread.sleep(550);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    this.sendCarMovement("U");
                }
            }
            /*if(movebuffer[0]==mvebuffer[1]){
                uiShowToast("Going up");
                this.sendCarMovement("U");
                try {
                    Thread.sleep(550);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.sendCarMovement("U");
            }else{

            }

            /*
            */
            /* Damon movement code
            if(moveBuffer[1] == 100 && moveBuffer[0] == 200){
                uiShowToast("Going left");
                System.out.println("Going left");
                this.sendCarMovement("L");
                try {
                    Thread.sleep(550);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.sendCarMovement("U");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }else{
                uiShowToast("Going up");
                //Toast.makeText(getApplicationContext(), "Going up", Toast.LENGTH_LONG).show();
                System.out.println("Going up");
                this.sendCarMovement("U");
                try {
                    Thread.sleep(2000);//wait 2 sec to next command;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            */
            return;
        }
    }
    public void uiShowToast(String temp){
        Toast.makeText(getApplicationContext(), temp, Toast.LENGTH_LONG).show();
    }

    private OnClickListener ourCheckButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            //Enter onReceive get position case
            currentScanCounter = -1;
            //
            toRoomNo = Integer.parseInt(roomNo.getText().toString());
            TaskRoute task = new TaskRoute();
            //task.run();
            run = new Thread(task);
            run.start();

            //roomNo.setEnabled(true);
            //our_check.setEnabled(true);

            //run = new Thread(task);
            //run.start();

            //Toast.makeText(MainActivity.mContext, "go cal", Toast.LENGTH_SHORT).show();

            //locat result = mRoute.calRoute();
            //System.out.println(result.current[0]);
     /*
            String abc = Integer.toString(maze[result.current[0]][result.current[1]]);
            while(result.previous != null){
                result = result.previous;
                abc = abc + "<-" + Integer.toString(maze[result.current[0]][result.current[1]]);
            }
            Toast.makeText(getApplicationContext(), abc, Toast.LENGTH_LONG).show();
*/


            //wifiManager.startScan();

           // synchronized (obj){
           // }
            //Toast.makeText(getApplicationContext(), "2429", Toast.LENGTH_SHORT).show();
            //Toast.makeText(getApplicationContext(), "current x:" + current_x + ", current y:" + current_y, Toast.LENGTH_SHORT).show();

        }
    };

    private View.OnClickListener bluetoothscan = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            pairedDevices = mBluetoothAdapter.getBondedDevices();


// If there are paired devices



            if (pairedDevices.size() > 0) {
                Toast.makeText(getApplicationContext(),
                        "Paring device",
                        Toast.LENGTH_LONG).show();

                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {

                    int checkredundant=0;
                    if(!mArrayAdapter.isEmpty())
                        for(int i=0;i<mArrayAdapter.getCount();i++)
                        {
                            String n = mArrayAdapter.getItem(i);
                            String p = device.getName() + "\n" + device.getAddress();
                            if(n.equals(p))
                                checkredundant=1;
                        }
                    // Add the name and address to an array adapter to show in a ListView
                    if(checkredundant==0)
                        mArrayAdapter.add(device.getName() + "\n" + device.getAddress());


                }
            }




            pairedListView.setAdapter(mArrayAdapter);



            //  mSecureAcceptThread = new AcceptThread();
            //     mSecureAcceptThread.run();
            //pairedListView.setAdapter(mArrayAdapter);


        }
    };

    private OnClickListener generateButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            CustomAsyncTask taskRunner = new CustomAsyncTask(MainActivity.this);
            taskRunner.execute("0");
        }
    };

    private OnClickListener analyseButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            CustomAsyncTask taskRunner = new CustomAsyncTask(MainActivity.this);
            taskRunner.execute("1");
        }
    };

    private OnClickListener loadButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {
            CustomAsyncTask taskRunner = new CustomAsyncTask(MainActivity.this);
            taskRunner.execute("2");
        }
    };

    private OnClickListener scanButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!wifiManager.isWifiEnabled()) {
                Toast.makeText(getApplicationContext(), "Turning on WiFi", Toast.LENGTH_SHORT).show();
                wifiManager.setWifiEnabled(true);
            }

            results.clear();
            currentScanCounter = 0;
            mScan.setEnabled(false);
            mSave.setEnabled(false);
            toggleSecondaryButtons(false);
            Toast.makeText(getApplicationContext(), "Please wait while scanning", Toast.LENGTH_SHORT).show();
            wifiManager.startScan();
        }
    };


    private OnClickListener saveButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View view) {

            if (results.size() == 0) {
                Toast.makeText(MainActivity.this, "Please collect signal data first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (currentScanCounter != -2) {
                Toast.makeText(MainActivity.this, "Signal data collection is now in progress", Toast.LENGTH_SHORT).show();
                return;
            }

            if (mFloorView.getText().toString().equals("") || mXView.getText().toString().equals("") || mYView.getText().toString().equals("")) {
                Toast.makeText(MainActivity.this, "Please enter floor, x and y", Toast.LENGTH_SHORT).show();
                return;
            }

            File dir = new File(baseDir + File.separator + DATA_FOLDER);
            if (dir.mkdir())
                Toast.makeText(getApplicationContext(), "The folder(wifi_data) has been created", Toast.LENGTH_SHORT).show();

            String filename = mFloorView.getText().toString() + "_" + mXView.getText().toString() + "_" + mYView.getText().toString() + ".csv";
            String filePath = baseDir + File.separator + DATA_FOLDER + File.separator + filename;

            Toast.makeText(MainActivity.this, "Please wait while saving", Toast.LENGTH_SHORT).show();
            try {
                //CSVWriter writer = new CSVWriter(new FileWriter(filePath), ',', CSVWriter.NO_QUOTE_CHARACTER);
                CSVWriter writer = new CSVWriter(new FileWriter(filePath), ',');

                for (String key : results.keySet()) {
                    AccessPoint obj = results.get(key);
                    List<String> tmp = new ArrayList<String>();

                    tmp.add(obj.bssid);
                    tmp.add(" " + obj.frequency);
                    tmp.add(" " + obj.ssid);
                    for (int level : obj.levels)
                        tmp.add(" " + level);

                    String[] str = new String[tmp.size()];
                    tmp.toArray(str);
                    writer.writeNext(str);
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            Toast.makeText(MainActivity.this, filename + " has been saved", Toast.LENGTH_SHORT).show();
            mSave.setEnabled(false);
            mXView.setText("");
            mYView.setText("");
        }
    };

    private void toggleSecondaryButtons(Boolean state) {
        mGenerate.setEnabled(state);
        mAnalyse.setEnabled(state);
        mLoad.setEnabled(state);
        mCheck.setEnabled(state);
    }

    private void toggleAllButtons(Boolean state) {
        mSave.setEnabled(state);
        mScan.setEnabled(state);
        mGenerate.setEnabled(state);
        mAnalyse.setEnabled(state);
        mLoad.setEnabled(state);
        mCheck.setEnabled(state);
    }
   /* @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // continueDoDiscovery();
                } else {
                    Toast.makeText(this,
                            "fail requesting bluetooth",
                            Toast.LENGTH_LONG).show();
                    // cancelOperation();
                }
                return;
            }
        }
    }*/
    private AdapterView.OnItemClickListener mDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {



            // pairedDevices = mBluetoothAdapter.getBondedDevices();

            cancelaccept();
            if(!pairedDevices.isEmpty())
            { mbluetoothdevice = mBluetoothAdapter.getBondedDevices().iterator().next();

                mConnectThread = new ConnectThread(mbluetoothdevice);
                mConnectThread.start();

            }


            // Cancel discovery because it's costly and we're about to connect
           /* mBluetoothAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra("device_address", address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();*/
        }
    };
    public synchronized void cancelaccept(){
        if(mInsecureAcceptThread!=null)
        {    mInsecureAcceptThread.interrupt();
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread=null;
        }
        if (mConnectThread!=null)
        {mConnectThread.cancel();
            mConnectThread=null;

        }
        if (mConnectedThread!=null){
            mConnectedThread.cancel();
            mConnectedThread=null;
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(mConnectThread !=null){
            mConnectThread .cancel();
        }
        if(mInsecureAcceptThread!=null){
            mInsecureAcceptThread.cancel();
        }

        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        if (mimagethread!=null)
            mimagethread.interrupt();
        //run.interrupt();

    }

    private class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            //System.out.println("Thread Counter: " + Thread.activeCount());
            if (currentScanCounter >= 0) {
                List<ScanResult> x = wifiManager.getScanResults();
                for (ScanResult tmp : x) {
                    String key = tmp.BSSID + tmp.frequency;
                    if (!results.containsKey(key))
                        results.put(key, new AccessPoint(tmp.SSID, tmp.BSSID, tmp.frequency));
                    results.get(key).addLevel(tmp.level);
                }

                if (currentScanCounter < MIN_SCAN_COUNT) {
                    wifiManager.startScan();
                    Toast.makeText(getApplicationContext(), "Do not interrupt while scanning (" + (MIN_SCAN_COUNT - currentScanCounter) + ")", Toast.LENGTH_LONG).show();
                    currentScanCounter++;
                } else {
                    mScan.setEnabled(true);
                    mSave.setEnabled(true);
                    toggleSecondaryButtons(true);
                    currentScanCounter = -2;
                    vibrator.vibrate(1000);
                }

            } else if (currentScanCounter == -1) {
                //Toast.makeText(getApplicationContext(),"here1",Toast.LENGTH_LONG).show();
                if (locationService != null) {
                    //Toast.makeText(getApplicationContext(),"here2",Toast.LENGTH_LONG).show();
                    List<ScanResult> x = wifiManager.getScanResults();
                    List<CustomWiFISignal> y = new ArrayList<CustomWiFISignal>();
                    for (ScanResult tmp : x) {
                        y.add(new CustomWiFISignal(tmp.SSID, tmp.BSSID, tmp.level, tmp.frequency));
                    }
                    Location location = locationService.getLocationByWifiSignals(y);
                    if (location != null)
                    {
                        synchronized (obj) {
                            current_x = location.get_x();
                            current_y = location.get_y();
                            obj.notify();
                        }

                        if(showXYZ == 1)
                        Toast.makeText(getApplicationContext(), "x:" + location.get_x() + " y:" + location.get_y() + " z:" + location.get_floor(), Toast.LENGTH_LONG).show();

                    }//}
                    else
                        Toast.makeText(getApplicationContext(), "No location yet", Toast.LENGTH_LONG).show();

                    currentScanCounter = -2;
                    showXYZ = 1;
                }
            }
        }
    }

    private class AccessPoint {
        private String ssid;
        private String bssid;
        private int frequency;
        private List<Integer> levels;

        public AccessPoint(String ssid, String bssid, int frequency) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.frequency = frequency;
            this.levels = new ArrayList<Integer>();
        }

        public void addLevel(int level) {
            levels.add(level);
        }
    }
    class imageThread extends Thread{
        public imageThread(){

        }
        public void run(){
            while (true) {


                synchronized (obj) {
                    //Log.i("current", Integer.toString(currentstate));
                    switch (currentstate) {
                        case 0:
                            if (mConnectedThread != null) {
                                byte[] bytesToSend = "S".getBytes();
                                mConnectedThread.write(bytesToSend);
                            }
                            break;
                        case 1:
                            if (mConnectedThread != null) {
                                byte[] bytesToSend = "U".getBytes();
                                mConnectedThread.write(bytesToSend);
                            }
                            break;
                        case 2:
                            if (mConnectedThread != null) {
                                byte[] bytesToSend = "D".getBytes();
                                mConnectedThread.write(bytesToSend);
                            }
                            break;
                        case 3:
                            if (mConnectedThread != null) {
                                byte[] bytesToSend = "L".getBytes();
                                mConnectedThread.write(bytesToSend);
                            }
                            break;
                        case 4:
                            if (mConnectedThread != null) {
                                byte[] bytesToSend = "R".getBytes();
                                mConnectedThread.write(bytesToSend);
                            }
                            break;


                    }
                }
                try {
                    Thread.sleep(300);
                } catch (Exception ex) {

                }

            }

        }
    }
    private class AcceptThread extends Thread {
        private BluetoothServerSocket mmServerSocket = null;

        public AcceptThread() {
            Toast.makeText(getApplicationContext(),
                    "acceptThread",
                    Toast.LENGTH_LONG).show();
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final

            try {

                // MY_UUID is the app's UUID string, also used by the client code
                mmServerSocket= mBluetoothAdapter.listenUsingRfcommWithServiceRecord(MY_UUID.toString(), MY_UUID);
            } catch (IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"Acceptthread constructor fail", Toast.LENGTH_SHORT).show();

                    }
                });

            }

        }

        public void run() {
            BluetoothSocket socket = null;

            if(mmServerSocket!=null){
                try {

                    socket = mmServerSocket.accept();

                    BluetoothDevice remoteDevice = socket.getRemoteDevice();

                    final String strConnected = "Connected:\n" +
                            remoteDevice.getName() + "\n" +
                            remoteDevice.getAddress();

                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this,"get device:"+ strConnected, Toast.LENGTH_SHORT).show();

                        }
                    });

                    //connected


                    //manageConnectedSocket(socket);

                } catch (IOException e) {
                    // TODO Auto-generated catch block


                }
                if(socket!=null)
                {manageConnectedSocket(socket);
                    try{
                        mmServerSocket.close();
                    }catch (IOException E){

                    }

                }
            }
            else{
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Acceptthread == null", Toast.LENGTH_SHORT).show();

                    }
                });

            }
            // Keep listening until exception occurs or a socket is returned

        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
                mmServerSocket=null;
            } catch (IOException e) { }
        }
    }
    public void manageConnectedSocket(BluetoothSocket socket){

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

    }
    private class ConnectThread extends Thread {


        private  BluetoothSocket mmSocket=null;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            Toast.makeText(getApplicationContext(),
                    "inside connectthread",
                    Toast.LENGTH_LONG).show();
            mmDevice = device;
            String dev = mmDevice.getName();
            Toast.makeText(getApplicationContext(),dev, Toast.LENGTH_LONG).show();

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                mmSocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                return;
            }

            //  Toast.makeText(getApplicationContext(),"abc", Toast.LENGTH_LONG).show();
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();
            boolean success = false;
            try {

                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception

                if(!mmSocket.isConnected()) {
                    mmSocket.connect();
                    success = true;
                }
            } catch (IOException e) {

            }

            if(mmSocket!=null){
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "successful", Toast.LENGTH_SHORT).show();

                    }
                });

                // Do work to manage the connection (in a separate thread)
                manageConnectedSocket(mmSocket);
            }
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream from hc-06
                    bytes = mmInStream.read(buffer);
                    String strReceived = new String(buffer, 0, bytes);
                    final String msgReceived = String.valueOf(bytes) + " bytes received:\n" + strReceived;
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, msgReceived, Toast.LENGTH_SHORT).show();

                        }
                    });

                    // Send the obtained bytes to the UI activity

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {mmInStream.close();
                mmOutStream.close();
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,255,0,255);
        preStartColor();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public void preStartColor(){
        String tmp = mPrefs.getString("hsv","");
        if(tmp.isEmpty()){
            return;
        }
        Gson gson = new Gson();
        mBlobColorHsv = gson.fromJson(tmp, Scalar.class);
        Log.i("hsv", tmp);
        System.out.println(mBlobColorHsv);
        //mBlobColorRgba = new Scalar(14.0, 27.0, 80.0, 255.0);
        //mBlobColorHsv = new Scalar(173.0, 174.0, 117.0, 255.0);
        mDetector.setHsvColor(mBlobColorHsv);
        mIsColorSelected = true;
    }

    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();

        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        System.out.println(mBlobColorHsv);
        System.out.println(mBlobColorRgba);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        mDetector.setHsvColor(mBlobColorHsv);

        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        Gson gson = new Gson();
        String json1 = gson.toJson(mBlobColorHsv);
        prefsEditor.putString("hsv", json1);
        prefsEditor.commit();

        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);

        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            //Log.e(TAG, "Contours count: " + contours.size());
            //Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
            double maxarea = 0;
            int thisX = 0;
            int rectWidth = 0;
            int rectHeight = 0;

            for (int i=0; i<contours.size(); i++)
            {
                //Convert contours(i) from MatOfPoint to MatOfPoint2f
                MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(i).toArray() );
                //Processing on mMOP2f1 which is in type MatOfPoint2f
                double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
                Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                //Convert back to MatOfPoint
                MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

                // Get bounding rect of contour
                Rect rect = Imgproc.boundingRect(points);

                // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                Imgproc.rectangle(mRgba, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height), CONTOUR_COLOR);// 0, 0, 255), 3);

                //push largest rect to maxarea
                if(calarea(maxarea,rect.width,rect.height)){
                    maxarea=rect.width*rect.height;
                    thisX=rect.x+rect.width/2;
                    rectWidth = rect.width;
                    rectHeight = rect.height;
                }

            }
            //Log.i("centerX", "x: "+thisX+" , area: "+maxarea );
            move_image(thisX, rectWidth, rectHeight);

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        }

        return mRgba;
    }
    public void move_image(int thisX, int w, int h){
        int dif = thisX-camcenterx;
        synchronized (obj) {
            if(w*h>camcenterx*camcentery*4*0.65 ) {
                Log.i("movement","Stop");
                currentstate=0;
            }else {
                if (dif < -200) {
                    Log.i("movement", "Left");
                    currentstate = 3;
                } else if (dif > 200) {
                    Log.i("movement", "Right");
                    currentstate = 4;
                } else {
                    currentstate = 1;
                    Log.i("movement", "forward");
                }
            }
        }
    }
    public boolean calarea(double maxarea, int w , int h){
       double result = w*h;
        if(result>maxarea)

        return true;
        else return false;
    }
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            Toast.makeText(getApplicationContext(),
                    "mReceiver",
                    Toast.LENGTH_LONG).show();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(getApplicationContext(),
                        "BluetoothDevice Action Found",
                        Toast.LENGTH_LONG).show();
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Add the name and address to an array adapter to show in a ListView
                mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    };



    private class CustomAsyncTask extends AsyncTask<String, String, String> {
        private Context mContext;

        public CustomAsyncTask(Context context) {
            this.mContext = context.getApplicationContext();
        }

        @Override
        protected void onPreExecute() {
            Toast.makeText(mContext, "Please wait", Toast.LENGTH_SHORT).show();
            toggleAllButtons(false);
        }

        protected String doInBackground(String... arg) {
            if (arg[0].equals("0")) {
                //Generate
                try {
                    String path = baseDir + File.separator + DATA_FOLDER;
                    DatabaseGenerator.getInstance().createDatabaseFile(new File(path), path);
                    return "WiFiDatabase_new.dbb has been created";
                } catch (Exception e) {
                    return "e.getMessage()";
                }
            } else if (arg[0].equals("1")) {
                //Analyse
                String path = baseDir + File.separator + DATA_FOLDER;
                File x = new File(path + File.separator + "WiFiDatabase_new.dbb");
                if (x.exists()) {
                    DatabaseGenerator.getInstance().checkDatabaseFile(new File(path), "WiFiDatabase_new.dbb");
                    return "Analysis completed";
                } else {
                    return "WiFiDatabase_new.dbb does not exist";
                }
            } else if (arg[0].equals("2")) {
                //Load
                String path = baseDir + File.separator + DATA_FOLDER;
                File x = new File(path + File.separator + "WiFiDatabase_new.dbb");
                if (x.exists()) {
                    locationService = new LocationService(new File(path), "WiFiDatabase_new.dbb");
                    return "WiFiDatabase_new.dbb has been loaded";
                } else {
                    return "WiFiDatabase_new.dbb does not exist";
                }
            }
            return "";
        }

        protected void onProgressUpdate(Integer... a) {
        }

        protected void onPostExecute(String result) {
            Toast.makeText(mContext, result, Toast.LENGTH_SHORT).show();
            toggleAllButtons(true);
        }
    }
}

