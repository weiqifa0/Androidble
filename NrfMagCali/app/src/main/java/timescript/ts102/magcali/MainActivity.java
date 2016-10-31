package timescript.ts102.magcali;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends FragmentActivity implements ScannerFragment.OnDeviceSelectedListener{
    private String TAG = "MagCali";
    private BluetoothAdapter mBluetoothAdapter;
    private DataProcess dataProcess;
    private TS102Device tsDevice;
    private TextView statusView;
    private TextView magDataView;
    private TextView magOffsetView;
    private Button devButton;
    private Button okButton;
    private ImageView imageView;
    private int eularData[] = new int[3];
    private int magData[];
    static byte bdata[] = new byte[39];
    private static int count=0;
    private Context context;
    private float currentDegree = 0;
    private float offsetCali[] = new float[3];
    private AlertDialog.Builder alert;
    private boolean alertShowing = false;
    private int filterCnt = 0;

    private SensorManager sensorManager;

    private final int STATUS_UPDATE_MSG = 0x0;
    private final int VALUES_UPDATE_MSG = 0x01;
    private final int BRUSH_UPDATE_MSG = 0x02;
    private final int MAGCALI_ENABLE_MSG = 0x03;
    private final int IMAGE_ROTATE_MSG = 0x04;
    private final int MAG_DATA_MSG = 0x05;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case STATUS_UPDATE_MSG:
                    if(msg.obj.equals("connected")) {
                        statusView.setTextColor(Color.BLUE);
                        statusView.setText(R.string.connected);
                    } else {
                        statusView.setTextColor(Color.RED);
                        statusView.setText(R.string.disconnected);
                        //magDataView.setText("");
                        //magOffsetView.setText("");
                    }
                    okButton.setEnabled(false);
                    Message newMsg = new Message();
                    newMsg.what = MAGCALI_ENABLE_MSG;
                    newMsg.obj = msg.obj;
                    mHandler.sendMessageDelayed(newMsg, 300);
                    break;
                case VALUES_UPDATE_MSG:
                    break;
                case BRUSH_UPDATE_MSG:
                    break;
                case MAGCALI_ENABLE_MSG:
                    if(msg.obj.equals("connected")) {
                        tsDevice.enableMagCali(true);
                    }
                    break;
                case MAG_DATA_MSG:
                    magData = (int[]) msg.obj;
                    offsetCali[0] = (magData[3]-magData[4])/2;
                    offsetCali[1] = (magData[5]-magData[6])/2;
                    offsetCali[2] = (magData[7]-magData[8])/2;
                    //Log.d("jimmy", String.format("%.2f", Math.sqrt(magData[0] * magData[0] + magData[1] * magData[1] + magData[2] * magData[2])) + ";" +
                    //        (magData[3]-magData[4])/2 + "," + (magData[5]-magData[6])/2 + ";" + (magData[7]-magData[8])/2);
                    //magDataView.setText(String.format("%.2f", Math.sqrt(magData[0] * magData[0] + magData[1] * magData[1] + magData[2] * magData[2])));
                    //magOffsetView.setText(offsetCali[0] + ", " + offsetCali[1] + ", " + offsetCali[2]);
                    //Log.d(TAG, offsetCali[0] + ", " + offsetCali[1] + ", " + offsetCali[2]);

                    if(offsetCali[0]>22 && offsetCali[0]<45 &&
                            offsetCali[1]>22 && offsetCali[1]<45 &&
                            offsetCali[2]>40 && offsetCali[2]<65) {
                        if(!okButton.isEnabled()) {
                            okButton.setEnabled(true);
                        }
                    }else {
                        if(okButton.isEnabled()) {
                            okButton.setEnabled(false);
                        }
                    }

                    if(offsetCali[0]>45 || offsetCali[1]>45 || offsetCali[2]>65) {
                        filterCnt++;
                        if(filterCnt>5) {
                            tsDevice.clearMagCali();
                            showDialog();
                            filterCnt=0;
                        }
                    }else{
                        filterCnt = 0;
                    }

                    float degree = msg.arg1;
                    if(currentDegree*degree<0) {
                        currentDegree = degree;
                        break;
                    }
                    RotateAnimation ra = new RotateAnimation(currentDegree, degree,
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF, 0.5f);
                    ra.setDuration(50);
                    ra.setFillAfter(true);
                    //imageView.startAnimation(ra);
                    currentDegree = degree;
                    break;

            }
        }
    };

    private void showDialog(){
        if(alertShowing){
            return;
        }
        alert = new AlertDialog.Builder(this);
        alert.setTitle("校准错误").setIcon(R.drawable.notification_template_icon_bg).setMessage("检测到强磁，请确保周围无磁场干扰!!!").setPositiveButton("确定",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        //finish();
                        alertShowing = false;
                        tsDevice.clearMagCali();
                    }
                });
        alert.create();
        alert.show();
        alertShowing = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this;
        statusView = (TextView) findViewById(R.id.status);
        //imageView = (ImageView) findViewById(R.id.image_view);
        devButton = (Button) findViewById(R.id.device);
        devButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ScannerFragment dialog = ScannerFragment.getInstance(context, null); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
                dialog.show(getSupportFragmentManager(), "scan_fragment");
            }
        });
        okButton = (Button) findViewById(R.id.cali_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tsDevice.isConnected()) {
                    tsDevice.enableMagCali(false);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    tsDevice.disConnect();
                }
            }
        });
        okButton.setEnabled(false);
        //magDataView = (TextView) findViewById(R.id.mag_data);
        //magOffsetView = (TextView) findViewById(R.id.mag_offset);


        dataProcess = new DataProcess() {
            @Override
            public void onResult(ResultData resultData) {
                super.onResult(resultData);
                String str = "x:" + String.format("%-5d", resultData.innerSide.xcount) +
                        "y:" + String.format("%-5d", resultData.innerSide.ycount) + "\n" +
                        "x:" + String.format("%-5d", resultData.outSide.xcount) +
                        "y:" + String.format("%-5d", resultData.outSide.ycount) + "\n" +
                        "x:" + String.format("%-5d", resultData.upSide.xcount) +
                        "y:" + String.format("%-5d", resultData.upSide.ycount) + "\n" +
                        "x:" + String.format("%-5d", resultData.downSide.xcount) +
                        "y:" + String.format("%-5d", resultData.downSide.ycount);
                Message msg = new Message();
                msg.what = VALUES_UPDATE_MSG;
                msg.obj = str;
                mHandler.sendMessage(msg);
            }
        };
        //dataProcess.addPlot((XYPlot) findViewById(R.id.historyPlot));

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        //开启蓝牙
        mBluetoothAdapter.enable();

        tsDevice = new TS102Device(this, mBluetoothAdapter) {
            @Override
            public void onDataRecived(int type, byte[] data) {
                if(type == TS102Device.DATA_TYPE_EULAR) {
                    String str = "R:" + String.format("%-5d", (data[1]<<8 | 0xff&data[0])) + "P:" + String.format("%-5d", (data[3]<<8 | 0xff&data[2]))
                            + "Y:" + String.format("%-5d", (data[5]<<8 | 0xff&data[4]));

                    eularData[0] = data[1]<<8 | 0xff & data[0];
                    eularData[1] = data[3]<<8 | 0xff & data[2];
                    eularData[2] = data[5]<<8 | 0xff & data[4];
                    Message msg = new Message();
                    msg.what = VALUES_UPDATE_MSG;
                    msg.obj = eularData;
                    mHandler.sendMessage(msg);
                } else if(type == TS102Device.DATA_TYPE_BRUSH) {
                    //Log.d("jimmy", new String(data));

                    if(data.length == 20) {
                        StringBuilder str = new StringBuilder();
                        for (byte b : data) {
                            str.append(String.format("%x:", b));
                        }
                        Log.d("jimmy", "20=====" + str.toString());
                    } else if(data.length == 19) {
                        StringBuilder str = new StringBuilder();
                        for (byte b : data) {
                            str.append(String.format("%x:", b));
                        }
                        Log.d("jimmy", "14=====" + str.toString());
                    }
                    if(data.length == 20) {
                        System.arraycopy(data, 0, bdata, 0, data.length);
                    } else if(data.length == 19) {
                        System.arraycopy(data, 0, bdata, 20, data.length);
                        Message msg = new Message();
                        msg.what = BRUSH_UPDATE_MSG;
                        msg.obj = bdata;
                        mHandler.sendMessage(msg);
                    }
                } else if(type == TS102Device.DATA_UART_RX) {
                    int magData[] = new int[9];
                    magData[0] = data[1]<<8 | 0xff & data[0];
                    magData[1] = data[3]<<8 | 0xff & data[2];
                    magData[2] = data[5]<<8 | 0xff & data[4];
                    magData[3] = data[7]<<8 | 0xff & data[6];
                    magData[4] = data[9]<<8 | 0xff & data[8];
                    magData[5] = data[11]<<8 | 0xff & data[10];
                    magData[6] = data[13]<<8 | 0xff & data[12];
                    magData[7] = data[15]<<8 | 0xff & data[14];
                    magData[8] = data[17]<<8 | 0xff & data[16];
                    /*
                    String rawData = new String();
                    for(int i=0; i<data.length; i++) {
                        rawData += ":" + Integer.toHexString(data[i]&0xff);
                    }
                    Log.d(TAG, "raw: " + rawData);
                    */
                    magCaliDetect(magData, eularData);
                }
            }

            @Override
            public void onConnectChanged(int state) {
                Message msg = new Message();
                msg.what = STATUS_UPDATE_MSG;
                if(state == BluetoothProfile.STATE_CONNECTED) {
                    msg.obj = "connected";
                } else {
                    msg.obj = "disconnected";
                }
                mHandler.sendMessage(msg);
            }

            @Override
            public void onBatteryChanged(int percent) {

            }
        };
    }

    private void magCaliDetect(int[] magData, int[] eData) {
        //Log.d(TAG, "MagData:" + magData[0] + "," + magData[1] + "," + magData[2]);
        //Log.d("jimmy", "CaliData:" + magData[3] + "," + magData[4] + "," + magData[5] + "," + magData[6] + "," +
        //        magData[7] + "," + magData[8]);
        //Log.d("jimmy", String.format("%.2f", Math.sqrt(magData[0] * magData[0] + magData[1] * magData[1] + magData[2] * magData[2])) + ";" +
        //        (magData[3]-magData[4])/2 + "," + (magData[5]-magData[6])/2 + ";" + (magData[7]-magData[8])/2);
        Log.d("jimmy", "x=" + magData[0] + ",y=" + magData[1] + ",z=" + magData[2]);
        int gx = magData[0];
        int gy = magData[1];
        int gz = magData[2];
        double rad = Math.atan2(gx, gy)*180/Math.PI;
        if(gz<0) {
            //rad = -rad;
        }
        double rad2 = Math.atan2(gz, gy)*180/Math.PI;
        if(gx<0) {
            rad2 = -rad2;
        }
        //Log.d("jimmy", "rad: " + String.format("%.2f", rad) + "; " + String.format("%.2f", rad2) + " data:" + magData[0] + " "
        //        + magData[1] + " " + magData[2]);
        Message msg = new Message();
        msg.what = MAG_DATA_MSG;
        msg.obj = magData;
        msg.arg1 = (int) rad;
        mHandler.sendMessage(msg);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(tsDevice.isConnected()) {
            tsDevice.enableMagCali(false);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            tsDevice.disConnect();
        }

        alertShowing = false;
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        if(tsDevice.isConnected()) {
            if(tsDevice.isConnected()) {
                tsDevice.enableMagCali(false);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                tsDevice.disConnect();
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tsDevice.connect(device);
    }

    @Override
    public void onDialogCanceled() {

    }
}
