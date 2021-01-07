package me.minhael.android;

import java.util.Arrays;

/**
 * Created by michaeln on 5/11/15.
 */
public final class ByteArrayUtils {

    public static char[] b2c(byte[] buf) {
        if (buf == null) return null;
        char[] re = new char[buf.length];
        for (int i = 0; i < buf.length; ++i)
            re[i] = (char) buf[i];
        return re;
    }

    public static byte[] c2b(char[] buf) {
        if (buf == null) return null;
        byte[] re = new byte[buf.length];
        for (int i = 0; i < buf.length; ++i)
            re[i] = (byte) buf[i];
        return re;
    }

    public static byte[] fixSize(byte[] array, int offset, int fromLength, int toLength, boolean isRightAligned) {
        if (fromLength < 1)
            fromLength = array.length;
        byte[] re = new byte[toLength];
        if (isRightAligned)
            System.arraycopy(array, offset + Math.max(0, fromLength - toLength), re, Math.max(0, toLength - fromLength), Math.min(fromLength, toLength));
        else
            System.arraycopy(array, offset, re, 0, Math.min(fromLength, toLength));
        return re;
    }

    /**
     * Concatenate all byte arrays into one array.
     *
     * @param a
     * @param b
     * @param arrays to be concatenated
     * @return resulting array
     */
    public static byte[] addAll(byte[] a, byte[] b, byte[]... arrays) {
        if (a == null)
            a = new byte[0];
        if (b == null)
            b = new byte[0];
        int total = a.length + b.length;
        if (arrays != null)
            for (byte[] array : arrays)
                if (array != null)
                    total += array.length;
        byte[] re = new byte[total];
        System.arraycopy(a, 0, re, 0, a.length);
        System.arraycopy(b, 0, re, a.length, b.length);
        if (arrays != null) {
            int length = a.length + b.length;
            for (int i = 0; i < arrays.length; ++i)
                if (arrays[i] != null) {
                    System.arraycopy(arrays[i], 0, re, length, arrays[i].length);
                    length += arrays[i].length;
                }
        }
        return re;
    }

    /**
     * Compare two byte array.
     *
     * @param a
     * @param b
     * @return positive if a > b; negative if b > a; 0 if equal
     */
    public static int compare(byte[] a, byte[] b) {
        if (a == null && b == null)
            return 0;
        if (a == null)
            return -b.length;
        if (b == null)
            return a.length;
        if (a.length != b.length)
            return a.length - b.length;
        return compare(a, b, Math.min(a.length, b.length));
    }

    public static int compare(byte[] a, byte[] b, int length) {
        length = Math.min(a.length, length);
        length = Math.min(b.length, length);
        for (int i = 0; i < length; ++i)
            if (a[i] != b[i])
                return (a[i] & 0xff) - (b[i] & 0xff);
        return 0;
    }

    public static byte[] inv(byte[] mask) {
        byte[] re = new byte[mask.length];
        for (int i = 0; i < re.length; ++i)
            re[i] = (byte) 0xff;
        return xor(mask, re);
    }

    public static int or(byte[] a, byte[] b, byte[] output, int offset, int length) {
        byte[] tmp = null;
        try {
            tmp = or(a, b);
            if (output.length < offset + length)
                throw new IllegalArgumentException("Output length smaller than defined.");
            else if (length < tmp.length)
                throw new IllegalArgumentException("Output length smaller than expected.");
            System.arraycopy(tmp, 0, output, offset, tmp.length);
            return tmp.length;
        } finally {
            if (tmp != null) Arrays.fill(tmp, (byte) 0);
        }
    }

    public static int and(byte[] a, byte[] b, byte[] output, int offset, int length) {
        byte[] tmp = null;
        try {
            tmp = and(a, b);
            if (output.length < offset + length)
                throw new IllegalArgumentException("Output length smaller than defined.");
            else if (length < tmp.length)
                throw new IllegalArgumentException("Output length smaller than expected.");
            System.arraycopy(tmp, 0, output, offset, tmp.length);
            return tmp.length;
        } finally {
            if (tmp != null) Arrays.fill(tmp, (byte) 0);
        }
    }

    public static int xor(byte[] a, byte[] b, byte[] output, int offset, int length) {
        byte[] tmp = null;
        try {
            tmp = xor(a, b);
            if (output.length < offset + length)
                throw new IllegalArgumentException("Output length smaller than defined.");
            else if (length < tmp.length)
                throw new IllegalArgumentException("Output length smaller than expected.");
            System.arraycopy(tmp, 0, output, offset, tmp.length);
            return tmp.length;
        } finally {
            if (tmp != null) Arrays.fill(tmp, (byte) 0);
        }
    }

    public static byte[] or(byte[] a, byte[] b, byte[]... arrays) {
        if (a == null && b == null)
            return new byte[0];
        else if (a == null)
            return b;
        else if (b == null)
            return a;
        int length = Math.max(a.length, b.length);
        for (byte[] array : arrays)
            if (array.length > length)
                length = array.length;
        byte[] re = new byte[length];
        System.arraycopy(a, Math.max(0, a.length - re.length), re, Math.max(0, re.length - a.length), Math.min(a.length, re.length));
        for (int i = 1; i <= b.length; ++i)
            re[re.length - i] |= b[b.length - i];
        for (byte[] array : arrays)
            for (int i = 1; i <= array.length; ++i)
                re[re.length - i] |= array[array.length - i];
        return re;
    }

    public static byte[] and(byte[] a, byte[] b, byte[]... arrays) {
        if (a == null || b == null)
            return new byte[0];
        int length = Math.min(a.length, b.length);
        for (byte[] array : arrays)
            if (array.length < length)
                length = array.length;
        byte[] re = new byte[length];
        System.arraycopy(a, a.length - re.length, re, 0, re.length);
        for (int i = 1; i <= re.length; ++i)
            re[re.length - i] &= b[b.length - i];
        for (byte[] array : arrays)
            for (int i = 1; i <= re.length; ++i)
                re[re.length - i] &= array[array.length - i];
        return re;
    }

    public static byte[] xor(byte[] a, byte[]... arrays) {
        if (arrays == null)
            return a;

        int i = 0;
        if (a == null) {
            a = arrays[i];
            i = 1;
        }

        int length = a.length;
        for (byte[] array : arrays)
            if (array.length < length)
                length = array.length;
        byte[] re = new byte[length];
        System.arraycopy(a, 0, re, 0, Math.min(a.length, re.length));

        while (i < arrays.length) {
            for (int j = 0; j < arrays[i].length; ++j)
                re[j] ^= arrays[i][j];
            ++i;
        }
        return re;
    }

    /**
     * Zero-padding right shift.
     *
     * @param a
     * @param bits
     * @return
     */
    public static byte[] rs(byte[] a, int bits) {
        if (a == null || a.length <= bits / 8)
            return new byte[0];
        if (bits == 0)
            return a.clone();

        byte[] re = new byte[a.length - bits / 8];
        System.arraycopy(a, 0, re, 0, re.length);
        shiftBits(re, bits % 8);
        re[0] &= (0xff >> (bits % 8));
        return re;
    }

    /**
     * Zero-padding left shift.
     *
     * @param a
     * @param bits
     * @return
     */
    public static byte[] ls(byte[] a, int bits) {
        if (a == null)
            return new byte[0];
        if (bits == 0)
            return a.clone();

        byte[] re = new byte[(int) (a.length + Math.ceil(bits / 8))];
        System.arraycopy(a, 0, re, 0, a.length);
        return shiftBits(re, -(bits % 8));
    }

    /**
     * Rotational right shift. Size from the right.
     *
     * @param a
     * @param bits
     * @param length
     * @return
     */
    public static byte[] rrs(byte[] a, int bits, int length) {
        if (length == a.length)
            return rrs(a, bits);
        else if (length < a.length)
            return Arrays.copyOfRange(rrs(a, bits), a.length - length, length);
        else {
            byte[] re = new byte[length];
            System.arraycopy(a, 0, re, length - a.length, a.length);
            return rrs(re, bits);
        }
    }

    /**
     * Rotational left shift. Size from the left.
     *
     * @param a
     * @param bits
     * @param length
     * @return
     */
    public static byte[] rls(byte[] a, int bits, int length) {
        if (length == a.length)
            return rls(a, bits);
        else if (length < a.length)
            return Arrays.copyOf(rls(a, bits), length);
        else {
            byte[] re = new byte[length];
            System.arraycopy(a, 0, re, length - a.length, a.length);
            return rls(a, bits);
        }
    }

    /**
     * Rotational right shift.
     *
     * @param a
     * @param bits
     * @return
     */
    public static byte[] rrs(byte[] a, int bits) {
        if (bits < 0)
            throw new IllegalArgumentException();

        int bytes = bits / 8;
        bits = bits % 8;

        return shiftBits(shiftBytes(a, bytes), bits);
    }

    /**
     * Rotational left shift.
     *
     * @param a
     * @param bits
     * @return
     */
    public static byte[] rls(byte[] a, int bits) {
        if (bits < 0)
            throw new IllegalArgumentException();

        int bytes = bits / 8;
        bits = bits % 8;

        return shiftBits(shiftBytes(a, -bytes), -bits);
    }

    /**
     * Shift numbers of bytes in the array.
     *
     * <p>
     *      Positive to the right. Negative to the left.
     * </p>
     *
     * @param a
     * @param bytes
     * @return
     */
    private static byte[] shiftBytes(byte[] a, int bytes) {
        if (a == null || a.length == 0 || bytes == 0)
            return a;

        bytes = bytes % a.length;
        byte[] tmp = new byte[Math.abs(bytes)];

        try {
            if (bytes > 0) {
                System.arraycopy(a, a.length - bytes, tmp, 0, bytes);
                System.arraycopy(a, 0, a, bytes, a.length - bytes);
                System.arraycopy(tmp, 0, a, 0, bytes);
            } else {
                System.arraycopy(a, 0, tmp, 0, -bytes);
                System.arraycopy(a, -bytes, a, 0, a.length + bytes);
                System.arraycopy(tmp, 0, a, a.length + bytes, -bytes);
            }
        } finally {
            Arrays.fill(tmp, (byte) 0);
        }

        return a;
    }

    /**
     * Shift numbers of bits in the array.
     *
     * <p>
     *      Positive to the right. Negative to the left.
     * </p>
     *
     * @param a
     * @param bits >= -8 && <= 8
     * @return
     */
    private static byte[] shiftBits(byte[] a, int bits) {
        if (a == null || a.length == 0 || bits == 0)
            return a;
        else if (bits > 8 || bits < -8)
            throw new IllegalArgumentException();

        byte carry = 0x00;
        if (bits < 0) {
            bits = -bits;
            byte rMask = (byte) (0xff >> bits);
            byte lMask = (byte) (0xff ^ rMask);
            carry = (byte) ((a[0] & lMask) >> (8 - bits));
            a[0] = (byte) ((a[0] & 0xff) << bits);
            for (int i = 1; i < a.length; ++i) {
                a[i - 1] |= (a[i] & lMask) >> (8 - bits);
                a[i] = (byte) ((a[i] & 0xff) << bits);
            }
            a[a.length - 1] |= carry;
        } else {
            byte lMask = (byte) (0xff >> bits << bits);
            byte rMask = (byte) (0xff ^ lMask);
            carry = (byte) ((a[a.length - 1] & rMask) << (8 - bits));
            a[a.length - 1] = (byte) ((a[a.length - 1] & 0xff) >> bits);
            for (int i = a.length - 2; i > -1; --i) {
                a[i + 1] |= (a[i] & rMask) << (8 - bits);
                a[i] = (byte) ((a[i] & 0xff) >> bits);
            }
            a[0] |= carry;
        }

        carry = 0x00;
        return a;
    }

    private ByteArrayUtils() { }
}
