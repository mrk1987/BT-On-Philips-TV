package com.gathin.btcontrol;

import android.app.Activity;
import android.app.AlertDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.bluetooth.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.media.AudioManager;
import android.view.*;
import android.widget.*;

/** Small TV audio switcher; uses the stock Android 8 Bluetooth profile APIs. */
public class MainActivity extends Activity {
    private BluetoothAdapter adapter;
    private BluetoothA2dp a2dp;
    private BluetoothDevice target;
    private TextView status;
    private Button connect, speakers, radio;
    private boolean connectWhenReady;
    private boolean restoreVolume;
    private long restoreAt;
    private AudioManager audio;
    private SharedPreferences prefs;
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() {
        public void run() { update(); handler.postDelayed(this, 1000); }
    };
    private final BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile service) {
            a2dp = (BluetoothA2dp) service;
            update();
        }
        public void onServiceDisconnected(int profile) { a2dp = null; update(); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        prefs = getSharedPreferences("volume", MODE_PRIVATE);
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) { toast("Bluetooth is unavailable on this TV"); finish(); return; }
        selectDefault();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(Color.rgb(12, 20, 30));
        TextView title = new TextView(this);
        title.setText("Bluetooth Audio"); title.setTextSize(26); title.setTextColor(Color.WHITE);
        layout.addView(title);
        status = new TextView(this); status.setTextSize(16); status.setMinLines(2); status.setPadding(0, 12, 0, 16);
        status.setTextColor(Color.rgb(160, 202, 237)); layout.addView(status);
        connect = button(layout, "Connect Bluetooth Audio", v -> {
            if (target == null && adapter.isEnabled()) { chooseHeadset(); return; }
            connectWhenReady = true;
            restoreVolume = target != null && prefs.contains(volumeKey());
            restoreAt = 0;
            if (!adapter.isEnabled() && !adapter.enable()) {
                connectWhenReady = false; toast("Could not enable Bluetooth");
            }
            update();
        });
        button(layout, "Choose audio device", v -> chooseHeadset());
        speakers = button(layout, "Use TV speakers", v -> {
            rememberVolume();
            connectWhenReady = false;
            restoreVolume = false;
            if (a2dp != null) {
                for (BluetoothDevice device : a2dp.getConnectedDevices()) call("disconnect", device);
            }
            update();
        });
        radio = button(layout, "Turn Bluetooth off", v -> {
            rememberVolume();
            connectWhenReady = false;
            restoreVolume = false;
            boolean ok = adapter.isEnabled() ? adapter.disable() : adapter.enable();
            if (!ok) toast("Bluetooth could not be switched");
            update();
        });
        button(layout, "Pair another device", v -> {
            try { startActivity(new Intent().setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity"))); }
            catch (Exception e) { toast("Pairing settings unavailable"); }
        });
        TextView hint = new TextView(this);
        hint.setText("Volume + / - adjusts the active output.\nConnect restores your Bluetooth level.\nBACK closes this panel.");
        hint.setTextSize(14); hint.setTextColor(Color.LTGRAY); hint.setPadding(0, 18, 0, 0);
        layout.addView(hint);
        setContentView(layout);
        getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
        getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.40), WindowManager.LayoutParams.MATCH_PARENT);
        adapter.getProfileProxy(this, listener, BluetoothProfile.A2DP);
        connect.requestFocus();
    }
    private String volumeKey() { return "volume_" + target.getAddress(); }
    private List<BluetoothDevice> headsets() {
        List<BluetoothDevice> result = new ArrayList<>();
        if (!adapter.isEnabled()) return result;
        for (BluetoothDevice d : adapter.getBondedDevices()) {
            boolean sink = false;
            android.os.ParcelUuid[] uuids = d.getUuids();
            if (uuids != null) for (android.os.ParcelUuid u : uuids)
                if (u.toString().equalsIgnoreCase("0000110b-0000-1000-8000-00805f9b34fb")) sink = true;
            BluetoothClass cls = d.getBluetoothClass();
            int type = cls == null ? 0 : cls.getDeviceClass();
            if (sink || type == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                type == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                type == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
                type == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                (a2dp != null && a2dp.getConnectedDevices().contains(d))) result.add(d);
        }
        Collections.sort(result, (a,b) -> String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName())));
        return result;
    }
    private void selectDefault() {
        List<BluetoothDevice> list = headsets();
        String selected = prefs.getString("selected", "");
        for (BluetoothDevice d : list) if (d.getAddress().equals(selected)) { target = d; return; }
        if (a2dp != null && !a2dp.getConnectedDevices().isEmpty()) target = a2dp.getConnectedDevices().get(0);
        else if (!list.isEmpty()) target = list.get(0);
    }
    private void chooseHeadset() {
        if (!adapter.isEnabled()) { toast("Turn Bluetooth on first"); return; }
        List<BluetoothDevice> list = headsets();
        if (list.isEmpty()) { toast("No paired audio devices. Use Pair another device first."); return; }
        String[] names = new String[list.size()];
        for (int i=0; i<names.length; i++) names[i] = list.get(i).getName();
        new AlertDialog.Builder(this).setTitle("Choose Bluetooth audio device")
            .setItems(names, (dialog, which) -> {
                rememberVolume(); target = list.get(which);
                prefs.edit().putString("selected", target.getAddress()).apply();
                connectWhenReady = false; restoreVolume = false; update();
            }).setNegativeButton("Cancel", null).show();
    }
    private Button button(LinearLayout parent, String text, View.OnClickListener action) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(18);
        b.setOnClickListener(action);
        float density = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(56 * density));
        p.bottomMargin = 6; parent.addView(b, p); return b;
    }
    private boolean call(String method, BluetoothDevice device) {
        try {
            boolean ok = (Boolean) BluetoothA2dp.class.getMethod(method, BluetoothDevice.class).invoke(a2dp, device);
            if (!ok) toast("Request not accepted. Try again in a moment.");
            return ok;
        } catch (Exception e) { toast("Bluetooth request failed"); android.util.Log.e("BtControl", method, e); return false; }
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private boolean isBluetoothRoute() {
        try {
            int devices = (Integer) Class.forName("android.media.AudioSystem").getMethod("getDevicesForStream", int.class).invoke(null, AudioManager.STREAM_MUSIC);
            return (devices & 0x380) != 0 && (devices & 2) == 0;
        } catch (Exception e) { return false; }
    }
    private void rememberVolume() {
        if (!restoreVolume && target != null && a2dp != null && a2dp.getConnectionState(target) == BluetoothProfile.STATE_CONNECTED && isBluetoothRoute())
            prefs.edit().putInt(volumeKey(), audio.getStreamVolume(AudioManager.STREAM_MUSIC)).apply();
    }
    private void update() {
        if (status == null || adapter == null) return;
        if (target == null && adapter.isEnabled()) selectDefault();
        boolean on = adapter.isEnabled();
        int state = a2dp == null || target == null ? BluetoothProfile.STATE_DISCONNECTED : a2dp.getConnectionState(target);
        if (restoreVolume && state == BluetoothProfile.STATE_CONNECTED && isBluetoothRoute()) {
            if (restoreAt == 0) restoreAt = SystemClock.uptimeMillis() + 2000;
            if (SystemClock.uptimeMillis() >= restoreAt) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.min(prefs.getInt(volumeKey(), audio.getStreamVolume(AudioManager.STREAM_MUSIC)), audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
                restoreVolume = false;
            }
        }
        if (connectWhenReady && on && a2dp != null) {
            connectWhenReady = false;
            if (target == null) { restoreVolume = false; chooseHeadset(); }
            else if (target.getBondState() != BluetoothDevice.BOND_BONDED) toast("Pair the audio device first");
            else if (state != BluetoothProfile.STATE_CONNECTED) {
                call("connect", target);
                state = BluetoothProfile.STATE_CONNECTING;
            }
        }
        status.setText(!on ? "Bluetooth is off - TV speakers" :
            state == BluetoothProfile.STATE_CONNECTED ? "Connected: " + target.getName() :
            state == BluetoothProfile.STATE_CONNECTING ? "Connecting Bluetooth Audio..." :
            state == BluetoothProfile.STATE_DISCONNECTING ? "Switching to TV speakers..." :
            target == null ? "Bluetooth is on - choose an audio device" : "Disconnected: " + target.getName());
        radio.setText(on ? "Turn Bluetooth off" : "Turn Bluetooth on");
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        finish();
    }
    @Override public void onResume() { super.onResume(); handler.post(refresh); }
    @Override public void onPause() { rememberVolume(); handler.removeCallbacks(refresh); super.onPause(); }
    @Override public void onDestroy() {
        if (a2dp != null) adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dp);
        super.onDestroy();
    }
}
