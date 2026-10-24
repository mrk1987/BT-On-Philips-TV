import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Looper;
/** Explicit one-shot commands for an explicitly specified owned device. No persistent changes. */
public class BtPair {
 public static void main(String[] args) throws Exception {
  Looper.prepareMainLooper();
  BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
  if (args.length != 2 || !(args[0].equals("status") || args[0].equals("pair") || args[0].equals("confirm"))) {
   throw new IllegalArgumentException("Usage: BtPair status|pair|confirm AA:BB:CC:DD:EE:FF");
  }
  if (adapter == null) throw new IllegalStateException("Bluetooth unavailable");
  BluetoothDevice device = adapter.getRemoteDevice(args[1]);
  System.out.println("enabled=" + adapter.isEnabled() + " bond=" + device.getBondState());
  if (args.length > 0 && args[0].equals("pair")) {
   adapter.cancelDiscovery();
   System.out.println("createBond=" + device.createBond());
  }
  if (args.length > 0 && args[0].equals("confirm")) {
   if (device.getBondState() != BluetoothDevice.BOND_BONDING) throw new IllegalStateException("Not bonding");
   System.out.println("confirmation=" + BluetoothDevice.class.getMethod("setPairingConfirmation", boolean.class).invoke(device, true));
  }
  System.exit(0);
 }
}
