package com.diz.sw01.custom;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.bluetooth.le.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.util.*;

/** Foreground, user-operated BLE scan/connect/disconnect test. No vendor SDK or data upload. */
public final class MainActivity extends Activity {
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private BluetoothAdapter adapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private LinearLayout deviceRows;
    private TextView status;
    private Button scanButton;
    private boolean scanning;
    private boolean destroyed;
    private final Runnable stopLater = new Runnable() { public void run() { stopScan(); } };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(48), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(244, 247, 251));
        root.setFitsSystemWindows(true);
        TextView title = text("Diz SW01 BLE Test", 25);
        root.addView(title);
        root.addView(text("워치 검색 · BLE 연결 · 연결 해제", 16));
        root.addView(text("근처 기기 권한만 사용합니다. 광고·계정·서버 통신이 없습니다. 제조사 바인딩과 건강 데이터 동기화는 포함하지 않은 연결 시험입니다.", 14));
        status = text("검색을 눌러 시작하세요.", 16);
        status.setPadding(0, dp(16), 0, dp(12));
        root.addView(status);
        scanButton = button("워치 검색 (20초)");
        scanButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { beginScan(); } });
        root.addView(scanButton);
        Button stop = button("검색 중지");
        stop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { stopScan(); } });
        root.addView(stop);
        Button disconnect = button("연결 해제");
        disconnect.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { disconnect(); } });
        root.addView(disconnect);
        root.addView(text("검색된 기기를 누르면 그 기기에만 연결을 시도합니다.", 13));
        ScrollView scroll = new ScrollView(this);
        deviceRows = new LinearLayout(this);
        deviceRows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(deviceRows);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        if (adapter == null) { say("이 기기에서 Bluetooth를 사용할 수 없습니다."); scanButton.setEnabled(false); }
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(Color.rgb(25, 43, 65)); view.setPadding(0, dp(4), 0, dp(4)); return view;
    }
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); return b; }
    private void say(final String message) {
        ui.post(new Runnable() { public void run() { if (!destroyed) status.setText(message); } });
    }
    private boolean permissionsReady() {
        if (Build.VERSION.SDK_INT >= 31) return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void beginScan() {
        if (adapter == null) return;
        if (!permissionsReady()) {
            requestPermissions(Build.VERSION.SDK_INT >= 31
                ? new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}
                : new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 10);
            return;
        }
        try {
            if (!adapter.isEnabled()) { startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 11); return; }
            stopScan(); devices.clear(); deviceRows.removeAllViews();
            scanner = adapter.getBluetoothLeScanner();
            if (scanner == null) { say("Bluetooth 검색을 시작할 수 없습니다."); return; }
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanning = true;
            scanner.startScan(null, settings, scanCallback);
            scanButton.setEnabled(false); say("근처 BLE 기기 검색 중…");
            ui.postDelayed(stopLater, 20000);
        } catch (SecurityException e) { scanning = false; scanButton.setEnabled(true); say("근처 기기 권한을 허용해 주세요."); }
          catch (RuntimeException e) { scanning = false; scanButton.setEnabled(true); say("검색 시작 실패: " + e.getClass().getSimpleName()); }
    }
    private void stopScan() {
        ui.removeCallbacks(stopLater);
        if (scanning && scanner != null) { try { scanner.stopScan(scanCallback); } catch (RuntimeException ignored) {} }
        boolean wasScanning = scanning; scanning = false;
        if (scanButton != null && adapter != null) scanButton.setEnabled(true);
        if (wasScanning) say("검색 종료 · 발견 " + devices.size() + "개. 이름을 확인한 후 기기를 누르세요.");
    }
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override public void onScanResult(int type, final ScanResult result) {
            ui.post(new Runnable() { public void run() {
                if (!scanning || destroyed) return;
                final BluetoothDevice device = result.getDevice();
                try {
                    String address = device.getAddress();
                    if (devices.containsKey(address)) return;
                    devices.put(address, device);
                    String name = result.getScanRecord() == null ? null : result.getScanRecord().getDeviceName();
                    if (name == null || name.length() == 0) name = device.getName();
                    if (name == null || name.length() == 0) name = "이름 없는 BLE 기기";
                    Button row = button(name + "\n" + address + " · " + result.getRssi() + " dBm");
                    row.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { connect(device); } });
                    deviceRows.addView(row);
                } catch (SecurityException e) { say("근처 기기 권한이 취소됐습니다."); stopScan(); }
            }});
        }
        @Override public void onScanFailed(final int error) { ui.post(new Runnable() { public void run() { stopScan(); say("검색 실패 코드: " + error); } }); }
    };
    private void connect(BluetoothDevice device) {
        stopScan(); disconnect();
        try { say("BLE 연결 시도 중…"); gatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE); if (gatt == null) say("연결 객체를 생성하지 못했습니다."); }
        catch (SecurityException e) { say("Bluetooth 연결 권한이 필요합니다."); }
        catch (RuntimeException e) { say("연결 시작 실패: " + e.getClass().getSimpleName()); }
    }
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override public void onConnectionStateChange(final BluetoothGatt connection, final int error, final int state) {
            ui.post(new Runnable() { public void run() {
                if (destroyed || connection != gatt) return;
                if (error == BluetoothGatt.GATT_SUCCESS && state == BluetoothProfile.STATE_CONNECTED) {
                    say("BLE 연결됨 · 서비스 확인 중…");
                    try { if (!connection.discoverServices()) say("BLE 연결됨 · 서비스 조회를 시작하지 못했습니다."); }
                    catch (SecurityException e) { say("서비스 조회 권한이 없습니다."); }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED || error != BluetoothGatt.GATT_SUCCESS) {
                    gatt = null; connection.close(); say("연결 종료 · GATT 상태 코드 " + error);
                }
            }});
        }
        @Override public void onServicesDiscovered(final BluetoothGatt connection, final int error) {
            ui.post(new Runnable() { public void run() {
                if (destroyed || connection != gatt) return;
                if (error != BluetoothGatt.GATT_SUCCESS) { say("BLE 연결됨 · 서비스 조회 실패 " + error); return; }
                List<BluetoothGattService> services = connection.getServices();
                StringBuilder message = new StringBuilder("BLE 연결 성공 · GATT 서비스 " + services.size() + "개\n");
                for (int i=0; i<Math.min(services.size(), 4); i++) message.append(services.get(i).getUuid()).append('\n');
                message.append("제조사 바인딩·건강 동기화 성공을 뜻하지는 않습니다."); say(message.toString());
            }});
        }
    };
    private void disconnect() {
        BluetoothGatt old = gatt; gatt = null;
        if (old != null) { try { old.disconnect(); } catch (RuntimeException ignored) {} finally { old.close(); } }
        say("연결 해제됨");
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 10) { if (permissionsReady()) beginScan(); else say("검색하려면 근처 기기 권한이 필요합니다."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data); if (request == 11 && result == RESULT_OK) beginScan();
    }
    @Override protected void onPause() { stopScan(); super.onPause(); }
    @Override protected void onDestroy() { destroyed = true; stopScan(); disconnect(); ui.removeCallbacksAndMessages(null); super.onDestroy(); }
}
