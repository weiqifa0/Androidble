/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jimmy.mimi.ble;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

/**
 * ScannerFragment class scan required BLE devices and shows them in a list. This class scans and filter devices with standard BLE Service UUID and devices with custom BLE Service UUID. It contains a
 * list and a button to scan/cancel. There is a interface {@link OnDeviceSelectedListener} which is implemented by activity in order to receive selected device. The scanning will continue to scan
 * for 5 seconds and then stop.
 */
public class ScannerFragment extends DialogFragment {
    private final static String TAG = "ScannerFragment";
    private final static String DEVICE_NAME = "ts-";
    private final static String PARAM_UUID = "param_uuid";
    private final static long SCAN_DURATION = 13000;

    private final static int REQUEST_PERMISSION_REQ_CODE = 34; // any 8-bit number
    private static Context context;

    private SimpleAdapter mAdapter;
    private ArrayList<HashMap<String, Object>> listItem = new ArrayList<HashMap<String, Object>>();
    private final ArrayList<BluetoothDevice> mDeviceList = new ArrayList<>();
    private BluetoothAdapter mBluetoothAdapter;
    private OnDeviceSelectedListener mListener;
    private final int MSG_FOUND_DEVICE = 0x01;
    private final int MSG_SET_BUTTON_TEXT = 0x02;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_FOUND_DEVICE:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;

                    HashMap<String, Object> map = new HashMap<String, Object>();
                    if(device.getName() == null) {
                        map.put("name", "n/a");
                    } else {
                        map.put("name", device.getName());
                    }
                    map.put("mac", device.getAddress());
                    map.put("rssi", msg.arg1);
                    if(listItem.indexOf(map) != -1) {
                        return;
                    }
                    for(int i=0; i<listItem.size(); i++) {
                        if(listItem.get(i).get("mac").equals(device.getAddress())) {
                            return;
                        }
                    }
                    listItem.add(map);
                    Collections.sort(listItem, new Comparator() {
                        @Override
                        public int compare(Object lhs, Object rhs) {
                            HashMap<String, Object> map1 = (HashMap<String, Object>) lhs;
                            HashMap<String, Object> map2 = (HashMap<String, Object>) rhs;
                            if (((String) map1.get("mac")).equals((String) map2.get("mac"))) {
                                return 0;
                            } else {
                                return ((Integer) map2.get("rssi") - (Integer) map1.get("rssi"));
                            }
                        }
                    });
                    mAdapter.notifyDataSetChanged();
                    mDeviceList.add(device);
                    break;
                case MSG_SET_BUTTON_TEXT:
                    mScanButton.setText((Integer) msg.obj);
                    break;
            }
        }
    };
    private Button mScanButton;

    private ParcelUuid mUuid;

    private boolean mIsScanning = false;

    public static ScannerFragment getInstance(Context con, final UUID uuid) {
        context = con;
        final ScannerFragment fragment = new ScannerFragment();

        final Bundle args = new Bundle();
        if (uuid != null)
            args.putParcelable(PARAM_UUID, new ParcelUuid(uuid));
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Interface required to be implemented by activity.
     */
    public static interface OnDeviceSelectedListener {
        /**
         * Fired when user selected the device.
         *
         * @param device
         *            the device to connect to
         * @param name
         *            the device name. Unfortunately on some devices {@link BluetoothDevice#getName()} always returns <code>null</code>, f.e. Sony Xperia Z1 (C6903) with Android 4.3. The name has to
         *            be parsed manually form the Advertisement packet.
         */
        public void onDeviceSelected(final BluetoothDevice device);

        /**
         * Fired when scanner dialog has been cancelled without selecting a device.
         */
        public void onDialogCanceled();
    }

    /**
     * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity.
     */
    @Override
    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnDeviceSelectedListener) activity;
        } catch (final ClassCastException e) {
            e.printStackTrace();
            throw new ClassCastException(activity.toString() + " must implement OnDeviceSelectedListener");
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle args = getArguments();
        if (args.containsKey(PARAM_UUID)) {
            mUuid = args.getParcelable(PARAM_UUID);
        }

        final BluetoothManager manager = (BluetoothManager) getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    @Override
    public void onDestroyView() {
        stopScan();
        super.onDestroyView();
        Log.d(TAG, "onDestroyView");
        mHandler = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_device_selection, null);
        final ListView listview = (ListView) dialogView.findViewById(android.R.id.list);

        listview.setEmptyView(dialogView.findViewById(android.R.id.empty));
        listview.setAdapter(mAdapter = new SimpleAdapter(context, listItem, R.layout.item, new String[]{"name", "mac", "rssi"},
                new int[]{R.id.device_name, R.id.device_mac, R.id.device_rssi}));

        builder.setTitle(R.string.scanner_title);
        final AlertDialog dialog = builder.setView(dialogView).create();
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(final AdapterView<?> parent, final View view, final int position, final long id) {
                stopScan();
                dialog.dismiss();
                for(int i=0; i<mDeviceList.size(); i++) {
                    if(listItem.get(position).get("mac").equals(mDeviceList.get(i).getAddress())) {
                        mListener.onDeviceSelected(mDeviceList.get(i));
                        break;
                    }
                }
            }
        });

        mScanButton = (Button) dialogView.findViewById(R.id.action_cancel);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.action_cancel) {
                    if (mIsScanning) {
                        stopScan();
                    } else {
                        startScan();
                    }
                }
            }
        });

        if (savedInstanceState == null)
            startScan();
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);

        mListener.onDialogCanceled();
    }

    /**
     * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback is activated This will perform regular scan for custom BLE Service UUID and then filter out.
     * using class ScannerServiceParser
     */
    private void startScan() {
        // Since Android 6.0 we need to obtain either Manifest.permission.ACCESS_COARSE_LOCATION or Manifest.permission.ACCESS_FINE_LOCATION to be able to scan for
        // Bluetooth LE devices. This is related to beacons as proximity devices.
        // On API older than Marshmallow the following code does nothing.
        mDeviceList.clear();
        listItem.clear();
        mAdapter.notifyDataSetChanged();
        setButtonText(R.string.scanner_action_cancel);

        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mIsScanning = true;

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsScanning) {
                    stopScan();
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    startScan();
                }
            }
        }, SCAN_DURATION);

    }

    private void setButtonText(int strID) {
        Message msg = new Message();
        msg.what = MSG_SET_BUTTON_TEXT;
        msg.obj = strID;
        mHandler.sendMessage(msg);
    }

    /**
     * Stop scan if user tap Cancel button
     */
    private void stopScan() {
        if (mIsScanning) {
            setButtonText(R.string.scanner_action_scan);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mIsScanning = false;
            Log.d(TAG, "scan stoped");
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            String tmp = device.getName();
            if(tmp!=null) {
                //if ((scanRecord[28]&0xFF)==0xFF && (scanRecord[27]&0xFF)==0xA0) {
                    Log.d("jimmy", "get one");
                    //do not show the far devices
                    //if(rssi > -55) {
                        Message msg = new Message();
                        msg.what = MSG_FOUND_DEVICE;
                        msg.arg1 = rssi;
                        msg.obj = device;
                        mHandler.sendMessage(msg);

                        /*
                        StringBuilder str = new StringBuilder();
                        for (byte b : scanRecord) {
                            str.append(String.format("%x:", b));
                        }
                        Log.d(TAG, "scanRecord: " + str.toString());
                        */

                        if (mIsScanning) {
                            //stopScan();
                        }
                    //}
                //}
            }
        }


    };
}
