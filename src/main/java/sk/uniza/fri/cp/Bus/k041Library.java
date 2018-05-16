package sk.uniza.fri.cp.Bus;

import com.sun.jna.Native;

/**
 * Trieda pre prístup k natívnym metódam knižnice k041.dll určenej pre 32bit platformu MS Windows.
 *
 * @author Tomáš Hianik
 * @created  11.3.2017.
 */
public class k041Library {
    public static native int USBInitDevice();
    public static native int USBSetAddress(int adress);
    public static native int USBSetData(int data);
    public static native int USBSetCommands(int bites, int read); //read - 0 - DB vystupna / 2 - DB vstupna (CPU odpojene)
    public static native int USBReadData();

    static {
        Native.register("k041");
    }
}
