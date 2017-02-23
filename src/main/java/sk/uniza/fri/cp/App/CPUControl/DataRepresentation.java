package sk.uniza.fri.cp.App.CPUControl;

/**
 * Created by Moris on 17.2.2017.
 */
class DataRepresentation {
    public enum eRepresentation{
        Dec,Hex,Bin,ASCII
    }

    static String getDisplayRepresentation(byte value, eRepresentation displayState){
        switch (displayState){
            case Dec:
                return Integer.toString(value & 0xFF );
            case Bin:
                return Integer.toBinaryString((value & 0xFF)  + 0x100).substring(1);
            case Hex:
                return "0x" + Integer.toHexString((value & 0xFF)  + 0x100).substring(1).toUpperCase();
            case ASCII:
                return String.valueOf((char) value);
        }
        return "";
    }

    static String getDisplayRepresentation(short value, eRepresentation displayState){
        switch (displayState){
            case Dec:
                return Integer.toString(value & 0xFFFF);
            case Bin:
                return Integer.toBinaryString((value & 0xFFFF) + 0x10000).substring(1);
            case Hex:
                return "0x" + Integer.toHexString((value & 0xFFFF) + 0x10000).substring(1).toUpperCase();
            case ASCII:
                char L = (char) value;
                char H = (char) (value >> 4);
                return String.format("%c%c", H, L);
        }
        return "";
    }

}
