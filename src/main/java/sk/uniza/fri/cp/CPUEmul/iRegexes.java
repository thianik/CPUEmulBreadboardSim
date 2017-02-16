package sk.uniza.fri.cp.CPUEmul;

/**
 * Created by Moris on 15.2.2017.
 */
public interface iRegexes {

    public String rByteDec = "^([0-1]?\\d?\\d|2[0-4]\\d|25[0-5])$";
    public String rByteHex = "^0[xX]\\p{XDigit}{1,2}$";
    public String rByteOct = "^0([0-2]?[0-7]{1,2}|3[0-7]{0,2})$";
    public String rByteBin = "^[0-1]{1,8}b$";
    public String rByteChar = "^'.'$";
    public String rByte = String.join("|", rByteDec, rByteHex, rByteBin, rByteOct, rByteChar);


    public String rShortDec = "^([0-5]?\\d?\\d?\\d?\\d|6[0-4]\\d\\d\\d|65[0-4]\\d\\d|655[0-2]\\d|6553[0-5])$";
    public String rShortHex = "^0[xX]\\p{XDigit}{1,4}$";
    public String rShortOct = "^0([0-7]{1,5}|1[0-7]{0,5})$";
    public String rShortBin = "^[0-1]{1,16}b$";
    public String rShort = String.join("|", rShortDec, rShortHex, rShortBin, rShortOct);

}
