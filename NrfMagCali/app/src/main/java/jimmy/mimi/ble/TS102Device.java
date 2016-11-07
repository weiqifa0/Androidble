package jimmy.mimi.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * Created by jimmy on 3/15/16.
 */
public abstract class TS102Device{
    private String TAG = "Mimi";
    private Context context;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService mSensorService;
    private BluetoothGattCharacteristic mSensorCharEnable;
    private BluetoothGattCharacteristic mEularCharx;
    private BluetoothGattCharacteristic mTimeChar;
    private BluetoothGattCharacteristic mSensorCharz;
    private BluetoothGattCharacteristic mSensorCharBrush;
    private BluetoothGattCharacteristic mUartTxChar;
    private BluetoothGattCharacteristic mUartRxChar;

    private BluetoothGattService mMimiService;
    private static final int[] mVibModes = new int[] { 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA6};
    public static final int S_BEGAN_CMD = 0xDD;
    public static final int S_VIB_START_CMD = 0xA0;
    public static final int S_VIB_STOP_CMD = 0xA7;
    public static final int R_BATTERY_CMD = 0xDC;
    public static final int S_SET_VIB_TIME_CMD = 0xDB;
    public static final int S_SET_LEFT_STRENGTH_CMD = 0xDA;
    public static final int S_SET_RIGHT_STRENGTH_CMD = 0xD9;
    private boolean connected = false;

    private volatile boolean mBusy = false;
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID SENSOR_SERVICE_UUID = UUID.fromString("0000ffa0-0000-1000-8000-00805f9b34fb");
    public static final UUID UART_TX_UUID = UUID.fromString("0000ffa1-0000-1000-8000-00805f9b34fb");
    public static final UUID UART_RX_UUID = UUID.fromString("0000ffa2-0000-1000-8000-00805f9b34fb");
    public static final UUID EULAR_UUID = UUID.fromString("0000ffa3-0000-1000-8000-00805f9b34fb");
    public static final UUID TIME_UUID = UUID.fromString("0000ffa4-0000-1000-8000-00805f9b34fb");
    public static final UUID SENSOR_Z_UUID = UUID.fromString("0000ffa5-0000-1000-8000-00805f9b34fb");
    public static final UUID SENSOR_BRUSH_UUID = UUID.fromString("0000ffa6-0000-1000-8000-00805f9b34fb");

    public static final UUID MIMI_SERVICE_UUID = UUID.fromString("dddd0001-dddd-dddd-dddd-dddddddddddd");
    public static final UUID MIMI_TX_UUID = UUID.fromString("dddd0002-dddd-dddd-dddd-dddddddddddd");
    public static final UUID MIMI_RX_UUID = UUID.fromString("dddd0003-dddd-dddd-dddd-dddddddddddd");

    public static final int DATA_UART_RX = 0x01;
    public static final int DATA_TYPE_BATTERY = 0x02;

    public static boolean readRunning = false;
    private int count = 0;

    public void onDataRecived(int type, byte[] data) {
    }

    public void onConnectChanged(int state) {
    }

    public TS102Device(Context mContext, BluetoothAdapter adapter) {
        context = mContext;
        mBluetoothAdapter = adapter;
    }

    public void startConnect() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "mBluetoothAdapter is null");
            return;
        }
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    public void startVib(int mode)    {
        byte[] bytes = new byte[3];
        bytes[0] = (byte) S_BEGAN_CMD;
        bytes[1] = (byte) S_VIB_START_CMD;
        bytes[2] = (byte) mVibModes[mode];
        mimiTxSend(bytes);
    }

    public void stopVib()    {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) S_BEGAN_CMD;
        bytes[1] = (byte) S_VIB_STOP_CMD;
        mimiTxSend(bytes);
    }

    public void setVibTime(int time) {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) S_SET_VIB_TIME_CMD;
        bytes[1] = (byte) time;
        mimiTxSend(bytes);
    }

    public void setLeftStrength(int strength)
    {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) S_SET_LEFT_STRENGTH_CMD;
        bytes[1] = (byte) strength;
        mimiTxSend(bytes);
    }

    public void setRightStrength(int strength)
    {
        byte[] bytes = new byte[2];
        bytes[0] = (byte) S_SET_RIGHT_STRENGTH_CMD;
        bytes[1] = (byte) strength;
        mimiTxSend(bytes);
    }

    public void mimiTxSend(byte[] bytes) {
        Log.d(TAG, "mimiTxSend");
        if(!connected) {
            return;
        }

        waitIdle();
        mBusy = true;
        mUartTxChar.setValue(bytes);
        mBluetoothGatt.writeCharacteristic(mUartTxChar);
    }

    public void enableBatteryNotify(boolean enable) {
        Log.d(TAG, "enableBatteryNotify");
        if(!connected) {
            return;
        }
        waitIdle();
        mBusy = true;
        setCharacteristicNotification(mUartRxChar, enable);
    }

    public void enableMagCali(boolean enable) {
        Log.d(TAG, "enableMagCali");
        if(!connected) {
            return;
        }
        waitIdle();
        mBusy = true;
        setCharacteristicNotification(mUartRxChar, enable);

        byte[] val = new byte[1];
        if(enable) {
            val[0] = (byte) 0xa0;
        }else {
            val[0] = (byte) 0xa1;
        }
        waitIdle();
        mBusy = true;
        mUartTxChar.setValue(val);
        mBluetoothGatt.writeCharacteristic(mUartTxChar);
        Log.d(TAG, "enableMagCali end");
    }

    public void clearMagCali() {
        Log.d(TAG, "clearMagCali");
        if(!connected) {
            return;
        }

        byte[] val = new byte[1];
        val[0] = (byte) 0xa2;
        waitIdle();
        mBusy = true;
        mUartTxChar.setValue(val);
        mBluetoothGatt.writeCharacteristic(mUartTxChar);
        Log.d(TAG, "clearMagCali end");
    }

    public void notifyEnable(boolean enable) {
        count = 0;
        if(!connected) {
            return;
        }

        waitIdle();
        mBusy = true;
        setCharacteristicNotification(mSensorCharBrush, enable);
        waitIdle();
        mBusy = true;
        setCharacteristicNotification(mEularCharx, enable);
    }

    public void enableEularNotify(boolean enable) {
        waitIdle();
        mBusy = true;
        setCharacteristicNotification(mEularCharx, enable);
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if(characteristic == null || mBluetoothGatt == null) {
            Log.d(TAG, "char is null");
            return false;
        }
        if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
            Log.w(TAG, "setCharacteristicNotification failed");
            return false;
        }

        BluetoothGattDescriptor clientConfig = characteristic
                .getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (clientConfig == null) {
            Log.d(TAG, "get char config error");
            return false;
        }

        if (enable) {
            Log.i(TAG, "enable notification");
            clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            Log.i(TAG, "disable notification");
            clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }

        mBusy = true;
        return mBluetoothGatt.writeDescriptor(clientConfig);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String tmp = device.getName();

            Log.d(TAG, device.getName());
            Log.d(TAG, "Find the target");
            mBluetoothAdapter.stopLeScan(this);
            mDevice = device;

            mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallBack);
        }

    };

    public BluetoothGattCallback mGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            onConnectChanged(newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "GATT Connected");
                gatt.discoverServices();
                connected = true;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT Disconnected");
                if(connected) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                    mDevice = null;
                    mUartTxChar = null;
                    mUartRxChar = null;
                    connected = false;
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            int type = 0;
            Log.d(TAG, "onCharacteristicChanged");
            if(characteristic == mUartRxChar) {

                Log.d(TAG, "rec:" + (characteristic.getValue()[0]&0xFF) + ":" + characteristic.getValue()[1]);
                if((characteristic.getValue()[0]&0xFF) == R_BATTERY_CMD) {
                    type = DATA_TYPE_BATTERY;
                }
            }
            onDataRecived(type, characteristic.getValue());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                /*
                mSensorService = gatt.getService(SENSOR_SERVICE_UUID);
                mEularCharx = mSensorService.getCharacteristic(EULAR_UUID);
                mTimeChar = mSensorService.getCharacteristic(TIME_UUID);
                mSensorCharz = mSensorService.getCharacteristic(SENSOR_Z_UUID);
                mSensorCharBrush = mSensorService.getCharacteristic(SENSOR_BRUSH_UUID);
                mUartTxChar = mSensorService.getCharacteristic(UART_TX_UUID);
                mUartRxChar = mSensorService.getCharacteristic(UART_RX_UUID);
                */
                mMimiService = gatt.getService(MIMI_SERVICE_UUID);
                mUartTxChar = mMimiService.getCharacteristic(MIMI_TX_UUID);
                mUartRxChar = mMimiService.getCharacteristic(MIMI_RX_UUID);
                mBusy = false;
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            mBusy = false;
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            int type = 0;
            if(characteristic == mUartRxChar) {
                type = DATA_UART_RX;
            }
            onDataRecived(type, characteristic.getValue());
            mBusy = false;
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            mBusy = false;
        }

    };

    public boolean waitIdle() {
        int i = 1000;
        while (--i > 0) {
            if (mBusy)
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            else
                break;
        }

        return i > 0;
    }

    public void connect(BluetoothDevice dev) {
        mDevice = dev;
        mBusy = true;
        mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallBack);
    }

    public void disConnect() {
        mDevice = null;
        if(connected) {
            mBluetoothGatt.disconnect();
        }
        mBusy = false;
    }

    public boolean isConnected() {
        return connected;
    }
}
