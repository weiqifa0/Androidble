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
import android.widget.SeekBar;
import android.widget.TextView;

import jimmy.mimi.ui.CircleMenuLayout;
import jimmy.mimi.ui.CircleMenuLayout.OnMenuItemClickListener;
import jimmy.mimi.ui.MySeekBar;
import jimmy.mimi.ui.ProgressHintDelegate;


public class MainActivity extends FragmentActivity implements ScannerFragment.OnDeviceSelectedListener{
    private String TAG = "Mimi";
    private BluetoothAdapter mBluetoothAdapter;
    private DataProcess dataProcess;
    private TS102Device tsDevice;
    private Context context;
    private AlertDialog.Builder alert;
    private boolean alertShowing = false;

    private ImageView statusImage;
    private TextView statusText;
    private ImageView batteryImage;
    private TextView baterryText;
    private CircleMenuLayout mCircleMenuLayout;
    private boolean vibStarted = false;
    private MySeekBar timeSeekBar;
    private MySeekBar leftStrSeekBar;
    private MySeekBar rightStrSeekBar;
    private int vibTime;
    private final static int DEFAULT_TIME = 1;
    private final static int DEFAULT_STRENGHT = 80;
    private int leftVibStrenght = DEFAULT_TIME;
    private int rightVibStrenght = DEFAULT_STRENGHT;

    private int selectedPos = 0;
    private String[] mItemTexts = new String[] { "自动", "揉", "推", "压", "拍", "锤"};
    private int[] mItemImgs2;

    private SensorManager sensorManager;

    private final int STATUS_UPDATE_MSG = 0x0;
    private final int VIB_START_MSG = 0x01;
    private final int VIB_STOP_MSG = 0x02;
    private final int VIB_READ_BAT_MSG = 0x03;
    private final int VIB_BAT_UPDATE_MSG = 0x04;
    private final int VIB_SET_VIB_TIME = 0x05;
    private final int VIB_SET_LEFT_STRENGTH = 0x06;
    private final int VIB_SET_RIGHT_STRENGTH = 0x07;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case STATUS_UPDATE_MSG:
                    if(msg.obj.equals("connected")) {
                        statusImage.setImageResource(R.drawable.connect_ok);
                        statusText.setText(R.string.connected);
                    } else {
                        statusImage.setImageResource(R.drawable.connect_fail);
                        statusText.setText(R.string.disconnected);
                    }
                    Message newMsg = new Message();
                    newMsg.what = VIB_READ_BAT_MSG;
                    newMsg.obj = msg.obj;
                    mHandler.sendMessageDelayed(newMsg, 300);
                    break;
                case VIB_READ_BAT_MSG:
                    tsDevice.enableBatteryNotify(true);
                    break;
                case VIB_BAT_UPDATE_MSG:
                    baterryText.setText((Byte) msg.obj+"%");
                    break;
                case VIB_START_MSG:
                    tsDevice.startVib(selectedPos);
                    break;
                case VIB_STOP_MSG:
                    tsDevice.stopVib();
                    break;
                case VIB_SET_VIB_TIME:
                    tsDevice.setVibTime((int)msg.obj);
                    break;
                case VIB_SET_LEFT_STRENGTH:
                    tsDevice.setLeftStrength((int) msg.obj);
                    break;
                case VIB_SET_RIGHT_STRENGTH:
                    tsDevice.setRightStrength((int) msg.obj);
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
        statusText = (TextView) findViewById(R.id.status_text);

        batteryImage = (ImageView) findViewById(R.id.battery_image);
        baterryText = (TextView) findViewById(R.id.battery_value);

        timeSeekBar = (MySeekBar) findViewById(R.id.time_seekbar);
        timeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                vibTime = (progress==100)?99:progress;
                Message msg = new Message();
                msg.what = VIB_SET_VIB_TIME;
                msg.obj = (int) vibTime;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        timeSeekBar.getHintDelegate().setHintAdapter(new ProgressHintDelegate.SeekBarHintAdapter() {
                    @Override public String getHint(android.widget.SeekBar seekBar, int progress) {
                        progress = progress==100?99:progress;
                        return String.valueOf((int)(progress/20)+1)+"min";
                    }
                });
        timeSeekBar.setProgress(DEFAULT_TIME);
        leftStrSeekBar = (MySeekBar) findViewById(R.id.l_strength_seekbar);
        leftStrSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                leftVibStrenght = progress;
                Message msg = new Message();
                msg.what = VIB_SET_LEFT_STRENGTH;
                msg.obj = progress;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        leftStrSeekBar.setProgress(DEFAULT_STRENGHT);
        rightStrSeekBar = (MySeekBar) findViewById(R.id.r_strength_seekbar);
        rightStrSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rightVibStrenght = progress;
                Message msg = new Message();
                msg.what = VIB_SET_RIGHT_STRENGTH;
                msg.obj = progress;
                mHandler.sendMessage(msg);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        rightStrSeekBar.setProgress(DEFAULT_STRENGHT);

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
                if(!tsDevice.isConnected()) {
                    final ScannerFragment dialog = ScannerFragment.getInstance(context, null); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
                    if(dialog.getDialog() == null){
                        Log.d(TAG, "getDialog is null");
                    }else if(dialog.getDialog().getWindow() == null) {
                        Log.d(TAG, "getWindow is null");
                    }
                    dialog.show(getSupportFragmentManager(), "scan_fragment");
                    return;
                }
                Log.d(TAG, "itemCenterClick");
                Message msg = new Message();
                if(vibStarted) {
                    Log.d(TAG, "stop vib");
                    ((ImageView)view.findViewById(R.id.id_center_image)).setImageResource(R.drawable.start);
                    msg.what = VIB_STOP_MSG;
                } else {
                    Log.d(TAG, "start vib");
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
        if(mBluetoothAdapter!=null) {
            mBluetoothAdapter.enable();
        }

        tsDevice = new TS102Device(this, mBluetoothAdapter) {
            @Override
            public void onDataRecived(int type, byte[] data) {

                if(type == TS102Device.DATA_TYPE_BATTERY) {
                    //String str = "R:" + String.format("%-5d", (data[1]<<8 | 0xff&data[0])) + "P:" + String.format("%-5d", (data[3]<<8 | 0xff&data[2]))
                    //        + "Y:" + String.format("%-5d", (data[5]<<8 | 0xff&data[4]));
                    if(data[1] > 0) {
                        Message msg = new Message();
                        msg.what = VIB_BAT_UPDATE_MSG;
                        msg.obj = data[1];
                        mHandler.sendMessage(msg);
                    }
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
