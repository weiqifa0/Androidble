package jimmy.mimi.ble;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import jimmy.mimi.ui.CircleMenuLayout;
import jimmy.mimi.ui.CircleMenuLayout.OnMenuItemClickListener;


public class MainActivity extends FragmentActivity implements ScannerFragment.OnDeviceSelectedListener{
    private String TAG = "MagCali";
    private BluetoothAdapter mBluetoothAdapter;
    private DataProcess dataProcess;
    private TS102Device tsDevice;
    private Context context;
    private AlertDialog.Builder alert;
    private boolean alertShowing = false;

    private ImageView statusImage;
    private ImageView batteryImage;
    private TextView baterryText;
    private CircleMenuLayout mCircleMenuLayout;
    private boolean vibStarted = false;

    private int selectedPos;
    private String[] mItemTexts = new String[] { "自动", "揉", "推", "压", "拍", "锤"};
    private int[] mItemImgs2;

    private SensorManager sensorManager;

    private final int STATUS_UPDATE_MSG = 0x0;
    private final int VIB_START_MSG = 0x01;
    private final int VIB_STOP_MSG = 0x02;
    private final int VIB_READ_BAT_MSG = 0x03;
    private final int IMAGE_ROTATE_MSG = 0x04;
    private final int MAG_DATA_MSG = 0x05;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case STATUS_UPDATE_MSG:
                    if(msg.obj.equals("connected")) {
                        statusImage.setImageResource(R.drawable.connect_ok);
                    } else {
                        statusImage.setImageResource(R.drawable.connect_fail);
                    }
                    Message newMsg = new Message();
                    newMsg.what = VIB_READ_BAT_MSG;
                    newMsg.obj = msg.obj;
                    mHandler.sendMessageDelayed(newMsg, 300);
                    break;
                case VIB_READ_BAT_MSG:
                    tsDevice.readBattery();
                case VIB_START_MSG:
                    tsDevice.startVib(selectedPos);
                    break;
                case VIB_STOP_MSG:
                    tsDevice.stopVib();
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
                        //tsDevice.clearMagCali();
                    }
                });
        alert.create();
        alert.show();
        alertShowing = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main2);
        mCircleMenuLayout = (CircleMenuLayout) findViewById(R.id.id_menulayout);
        mCircleMenuLayout.setMenuItemIconsAndTexts(mItemImgs2, mItemTexts);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        context = this;
        statusImage = (ImageView) findViewById(R.id.status);
        statusImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ScannerFragment dialog = ScannerFragment.getInstance(context, null); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
                dialog.show(getSupportFragmentManager(), "scan_fragment");
            }
        });

        batteryImage = (ImageView) findViewById(R.id.battery_image);
        baterryText = (TextView) findViewById(R.id.battery_value);

        mCircleMenuLayout.setOnMenuItemClickListener(new OnMenuItemClickListener()
        {
            @Override
            public void itemClick(View view, int pos)
            {
                Log.d(TAG, "item pos " + pos);
                selectedPos = pos;
            }

            @Override
            public void itemCenterClick(View view)
            {
                Message msg = new Message();
                if(vibStarted) {
                    ((ImageView)view.findViewById(R.id.id_center_image)).setImageResource(R.drawable.start);
                    msg.what = VIB_STOP_MSG;
                } else {
                    ((ImageView)view.findViewById(R.id.id_center_image)).setImageResource(R.drawable.pause);
                    msg.what = VIB_START_MSG;
                }
                mHandler.sendMessage(msg);
                vibStarted = !vibStarted;
            }
        });


        dataProcess = new DataProcess() {
            @Override
            public void onResult(ResultData resultData) {
                super.onResult(resultData);
                /*
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
                */
            }
        };
        //dataProcess.addPlot((XYPlot) findViewById(R.id.historyPlot));

        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        //开启蓝牙
        mBluetoothAdapter.enable();

        tsDevice = new TS102Device(this, mBluetoothAdapter) {
            @Override
            public void onDataRecived(int type, byte[] data) {
                /*
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
                }
                */
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

    @Override
    protected void onPause() {
        super.onPause();
        if(tsDevice.isConnected()) {
        }

        alertShowing = false;
    }

    @Override
    public void onDeviceSelected(BluetoothDevice device) {
        if(tsDevice.isConnected()) {
            if(tsDevice.isConnected()) {
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
