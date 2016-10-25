package timescript.ts102.magcali;

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
    private String TAG = "TS102Device";
    public String DEVICE_NAME = "ts-102";
    public String DEVICE_NAME2 = "Nordic_TS102";
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

    public static final int DATA_TYPE_EULAR = 0x01;
    public static final int DATA_TYPE_Y = 0x02;
    public static final int DATA_TYPE_Z = 0x03;
    public static final int DATA_TYPE_BRUSH = 0x04;
    public static final int DATA_UART_RX = 0x05;

    public static boolean readRunning = false;
    private int count = 0;

    public void onDataRecived(int type, byte[] data) {
    }

    public void onConnectChanged(int state) {
    }

    public void onBatteryChanged(int percent) {

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
            if (DEVICE_NAME.equalsIgnoreCase(device.getName()) || DEVICE_NAME2.equalsIgnoreCase(device.getName())) {
                Log.d(TAG, "Find the target");
                mBluetoothAdapter.stopLeScan(this);
                mDevice = device;

                mBluetoothGatt = mDevice.connectGatt(context, false, mGattCallBack);
            }
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
            if(characteristic == mEularCharx) {
                count++;
                type = DATA_TYPE_EULAR;
            } else if(characteristic == mTimeChar) {
                Log.d("jimmy", "timechar notify ...");
                type = DATA_TYPE_Y;
            } else if(characteristic == mSensorCharz) {
                type = DATA_TYPE_Z;
            } else if(characteristic == mSensorCharBrush) {
                Log.d("jimmy", "brush data notify ...");
                type = DATA_TYPE_BRUSH;
            } else if(characteristic == mUartRxChar) {
                type = DATA_UART_RX;
            }
            onDataRecived(type, characteristic.getValue());
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (BluetoothGatt.GATT_SUCCESS == status) {
                mSensorService = gatt.getService(SENSOR_SERVICE_UUID);
                mEularCharx = mSensorService.getCharacteristic(EULAR_UUID);
                mTimeChar = mSensorService.getCharacteristic(TIME_UUID);
                mSensorCharz = mSensorService.getCharacteristic(SENSOR_Z_UUID);
                mSensorCharBrush = mSensorService.getCharacteristic(SENSOR_BRUSH_UUID);
                mUartTxChar = mSensorService.getCharacteristic(UART_TX_UUID);
                mUartRxChar = mSensorService.getCharacteristic(UART_RX_UUID);
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
            if(characteristic == mEularCharx) {
                type = DATA_TYPE_EULAR;
            } else if(characteristic == mTimeChar) {
                type = DATA_TYPE_Y;
            } else if(characteristic == mSensorCharz) {
                type = DATA_TYPE_Z;
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
