package me.minhael.android;

/**
 * Created by michaeln on 13/12/2016.
 */
public class HexUtils {

    public static int estimateSize(long value) {
        int i = 0;
        while (value != 0) {
            value >>= 8;
            ++i;
        }
        return i;
    }

    public static long readHex(int digits, byte[] buffer) {
        return readHex(digits, buffer, 0, buffer.length);
    }

    public static long readHex(byte[] buffer, int offset, int length) {
        if (buffer == null)
            return 0;

        long value = 0;
        for (int i = 0; i < length; ++i) {
            value <<= 8;
            value += buffer[offset + i] & 0xff;
        }
        return value;
    }

    public static int writeHex(long value, byte[] buffer, int offset, int length) {
        if (buffer == null)
            return 0;

        int i = 0;
        while (value != 0 && i < length) {
            buffer[offset + length - 1 - i] = (byte) (value & 0x00ff);
            value >>= 8;
            ++i;
        }
        return i;
    }

    public static byte[] writeHex(long value, int length) {
        byte[] result = new byte[length];
        writeHex(value, result, 0, result.length);
        return result;
    }

    /**
     * 0x12 0x34 -> 1234
     *
     * @param digits
     * @param buffer
     * @param offset
     * @param length
     * @return
     */
    public static long readHex(int digits, byte[] buffer, int offset, int length) {
        if (buffer == null)
            return 0;

        long value = 0;
        for (int i = 0; i < digits && i / 2 < length; ++i) {
            value <<= 4;
            if (i % 2 == 0)
                value += ((buffer[offset + i / 2] & 0x00f0) >> 4);
            else
                value += buffer[offset + i / 2] & 0x000f;
        }
        return value;
    }

    /**
     * 1234 -> 0x12 0x34
     *
     * @param value
     * @param digits
     * @param buffer
     * @param offset
     * @param length
     * @return
     */
    public static int writeHex(long value, int digits, byte[] buffer, int offset, int length) {
        if (buffer == null)
            return 0;
        int i = 0;
        while (value != 0 && i < digits && i / 2 < offset + length) {

            if (i % 2 == 0)
                buffer[offset + length - 1 - i / 2] = (byte) (value & 0x0f);
            else
                buffer[offset + length - 1 - i / 2] += (byte) ((value & 0x0f) << 4);

            value >>= 4;
            ++i;
        }
        return (i + 1) / 2;
    }

    private static final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    public static byte ordinalHex(char num) {
        switch (num) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'A':
            case 'a':
                return 0xa;
            case 'B':
            case 'b':
                return 0xb;
            case 'C':
            case 'c':
                return 0xc;
            case 'D':
            case 'd':
                return 0xd;
            case 'E':
            case 'e':
                return 0xe;
            case 'F':
            case 'f':
                return 0xf;
            default:
                return (byte) (num % 16);
        }
    }

    public static String readString(byte[] buffer, int offset, int length) {
        return new String(readCharArray(buffer, offset, length));
    }

    public static char[] readCharArray(byte[] buffer) {
        if (buffer == null) return new char[0];
        return readCharArray(buffer, 0, buffer.length);
    }

    public static String readString(byte[] buffer) {
        if (buffer == null) return "";
        return readString(buffer, 0, buffer.length);
    }

    public static int writeString(String value, byte[] buffer, int offset, int length) {
        if (value == null || buffer == null) return 0;

        value = value.replaceAll("[^0-9ABCDEFabcdef]", "");
        if (value.length() % 2 != 0)
            value = "0" + value;

        int i = 0;
        while (i < value.length() && i / 2 < length) {
            buffer[offset + i / 2] = (byte)Integer.parseInt(value.substring(i, i + 2), 16);
            i += 2;
        }
        return i / 2;
    }

    public static byte[] writeString(String value) {
        if (value == null) return null;
        value = value.replaceAll("[^0-9ABCDEFabcdef]", "");
        byte[] result = new byte[(value.length() + 1) / 2];
        writeString(value, result, 0, result.length);
        return result;
    }

    public static char[] readCharArray(byte[] buffer, int offset, int length) {
        if (buffer == null || length == 0) return new char[0];
        char[] hexChars = new char[length * 2];
        for ( int j = 0; j < length; j++ ) {
            int v = buffer[offset + j] & 0xFF;
            hexChars[j * 2] = hexArray[(v >>> 4) % 16];
            hexChars[j * 2 + 1] = hexArray[(v & 0x0F) % 16];
        }
        return hexChars;
    }

    public static char[] readChars(int digits, byte[] buffer, int offset, int length) {
        if (length == 0 || buffer == null) return new char[0];
        digits = Math.min(digits, length * 2);
        char[] hexChars = new char[digits];
        for (int j = 0; j < digits; j++) {
            int v = buffer[offset + j / 2] & 0xFF;
            if (j % 2 == 0)
                hexChars[j] = hexArray[(v >> 4) % 16];
            else
                hexChars[j] = hexArray[(v & 0x0f) % 16];
        }
        return hexChars;
    }

    /**
     * Right aligned. Left pad 0.
     *
     * @param value
     * @return
     */
    public static byte[] writeCharArray(char[] value) {
        if (value == null) return null;
        byte[] result = new byte[(value.length + 1) / 2];
        writeCharArray(value, result, 0, result.length);
        return result;
    }

    /**
     * Right aligned. Left pad 0.
     *
     * @param value
     * @return
     */
    public static int writeCharArray(char[] value, byte[] buffer, int offset, int length) {
        if (value == null || buffer == null) return 0;

        boolean isFlipped = value.length % 2 != 0;

        int i = 0;
        while (i < value.length && (i + (isFlipped ? 1 : 0)) / 2 < length) {
            if (i % 2 == 0)
                buffer[offset + (i + (isFlipped ? 1 : 0)) / 2] |= ((Character.getNumericValue(value[i]) & 0x0f) << (isFlipped ? 0 : 4));
            else
                buffer[offset + (i + (isFlipped ? 1 : 0)) / 2] |= ((Character.getNumericValue(value[i]) & 0x0f) << (isFlipped ? 4 : 0));
            ++i;
        }
        return (i + (isFlipped ? 1 : 0)) / 2;
    }

    /**
     * Left aligned
     *
     * @param value
     * @param digits
     * @param pad
     * @param buffer
     * @param offset
     * @param length
     * @return
     */
    public static int writeChars(char[] value, int digits, char pad, byte[] buffer, int offset, int length) {
        return writeChars(value, 0, digits, pad, buffer, offset, length);
    }

    /**
     * Left aligned
     *
     * @param value
     * @param iOffset
     * @param digits
     * @param pad
     * @param buffer
     * @param oOffset
     * @param length
     * @return
     */
    public static int writeChars(char[] value, int iOffset, int digits, char pad, byte[] buffer, int oOffset, int length) {
        if (buffer == null) return 0;
        byte ctrl = ordinalHex(pad);

        int i = 0;
        while (i < digits && i + iOffset < value.length && i / 2 < oOffset + length) {
            byte v = ordinalHex(value[i + iOffset]);

            if (i % 2 == 0)
                buffer[oOffset + i / 2] = (byte) (v << 4);
            else
                buffer[oOffset + i / 2] += v;
            ++i;
        }

        if (i % 2 == 1)
            buffer[oOffset + i / 2] += ctrl;

        return (i + 1) / 2;
    }

    private HexUtils() { }
}
