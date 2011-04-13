/*
 * Copyright 2011 Shotaro Uchida
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.squilla.util;

import java.util.Random;
import org.squilla.io.Frame;
import org.squilla.io.FrameBuffer;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public abstract class Commons {

    public static final int INT_64_SIZE = 8;
    public static final int INT_32_SIZE = 4;
    public static final int INT_16_SIZE = 2;
    public static final int INT_8_SIZE = 1;
    public static final int BYTE_SIZE = 8;

    private static final FrameBuffer debugBuffer;

    static {
        debugBuffer = new FrameBuffer(new byte[4096]);
    }

    public static String toHexString(byte b) {
        String hex = Integer.toHexString(b);
        int len = hex.length();
        if (len == 2) {
            return hex;
        } else if (len == 1) {
            return "0" + hex;
        } else {
            return hex.substring(len - 2);
        }
    }

    public static String toHexString(byte[] data, int offset, int length) {
        String s = "";
        for (int i = offset; i < offset + length; i++) {
            s += toHexString(data[i]);
        }
        return s;
    }

    public static String toHexString(int[] data, int offset, int length) {
        String s = "";
        for (int i = offset; i < offset + length; i++) {
            s += toHexString((byte) data[i]);
        }
        return s;
    }

    public static void printDev(byte[] buffer, int off, int len, boolean resp) {
        if (resp) {
            System.out.print(">>(" + len + ") ");
        } else {
            System.out.print("<<(" + len + ") ");
        }
        for (int i = off; i < off + len; i++) {
            System.out.print(toHexString(buffer[i]) + " ");
        }
        System.out.println();
    }

    public static void printDev(Frame frame, boolean resp) {
        debugBuffer.rewind();
        frame.pull(debugBuffer);
        printDev(debugBuffer, resp);
    }

    public static void printDev(FrameBuffer buffer, boolean resp) {
        printDev(buffer.getRawArray(), buffer.getOffset(), buffer.getPosition(), resp);
    }

    public static long toLongLE(byte[] src, int off) {
        long dest = 0;
        for (int p = 0; p < INT_64_SIZE; p++) {
            int d = src[off + p] & 0xff;
            dest |= (d << (BYTE_SIZE * p));
        }
        return dest;
    }
    
    public static long toLongBE(byte[] src, int off) {
        long dest = 0;
        for (int p = 0; p < INT_64_SIZE; p++) {
            int d = src[off + p] & 0xff;
            dest |= (d << (BYTE_SIZE * (INT_64_SIZE - 1 - p)));
        }
        return dest;
    }

    public static short toShortLE(byte[] src, int off) {
        return (short) toIntLE(src, off, INT_16_SIZE, 1)[0];
    }

    public static short toShortBE(byte[] src, int off) {
        return (short) toIntBE(src, off, INT_16_SIZE, 1)[0];
    }

    public static int toIntLE(byte[] src, int off) {
        return toIntLE(src, off, INT_32_SIZE, 1)[0];
    }

    public static int toIntBE(byte[] src, int off) {
        return toIntBE(src, off, INT_32_SIZE, 1)[0];
    }

    public static int[] toIntLE(byte[] src, int off, int size, int length) {
        int[] dest = new int[length];
        for (int index = 0; index < length; index++) {
            dest[index] = 0;
            for (int p = 0; p < size; p++) {
                int d = src[off + (p + size * index)] & 0xff;
                dest[index] |= (d << (BYTE_SIZE * p));
            }
        }
        return dest;
    }

    public static int[] toIntBE(byte[] src, int off, int size, int length) {
        int[] dest = new int[length];
        for (int index = 0; index < length; index++) {
            dest[index] = 0;
            for (int p = 0; p < size; p++) {
                int d = src[off + (p + size * index)] & 0xff;
                dest[index] |= (d << (BYTE_SIZE * (size - 1 - p)));
            }
        }
        return dest;
    }

    public static byte[] toByteLE(int[] src, int size, int length) {
        byte[] dest = new byte[size * length];
        for (int index = 0; index < length; index++) {
            int mask = 0x000000ff;
            for (int p = 0; p < size; p++) {
                int d = src[index] & mask;
                dest[p + size * index] = (byte) ((d >> (BYTE_SIZE * p)) & 0xff);
                mask <<= BYTE_SIZE;
            }
        }
        return dest;
    }

    public static byte[] toByteBE(int[] src, int size, int length) {
        byte[] dest = new byte[size * length];
        for (int index = 0; index < length; index++) {
            int mask = 0x000000ff << ((size - 1) * BYTE_SIZE);
            for (int p = 0; p < size; p++) {
                int d = src[index] & mask;
                dest[p + size * index] = (byte) ((d >> (BYTE_SIZE * (size - 1 - p))) & 0xff);
                mask >>= BYTE_SIZE;
            }
        }
        return dest;
    }

    public static byte[] toByteLE(int src, int size) {
        byte[] dest = new byte[size];
        int mask = 0x000000ff;
        for (int p = 0; p < size; p++) {
            int d = src & mask;
            dest[p] = (byte) ((d >> (BYTE_SIZE * p)) & 0xff);
            mask <<= BYTE_SIZE;
        }
        return dest;
    }

    public static byte[] toByteBE(int src, int size) {
        byte[] dest = new byte[size];
        int mask = 0x000000ff << ((size - 1) * BYTE_SIZE);
        for (int p = 0; p < size; p++) {
            int d = src & mask;
            dest[p] = (byte) ((d >> (BYTE_SIZE * (size - 1 - p))) & 0xff);
            mask >>= BYTE_SIZE;
        }
        return dest;
    }

    public static void copyByteLE(int src, int size, byte[] dest, int offset) {
        int mask = 0x000000ff;
        for (int p = 0; p < size; p++) {
            int d = src & mask;
            dest[p + offset] = (byte) ((d >> (BYTE_SIZE * p)) & 0xff);
            mask <<= BYTE_SIZE;
        }
    }

    public static void copyByteBE(int src, int size, byte[] dest, int offset) {
        int mask = 0x000000ff << ((size - 1) * BYTE_SIZE);
        for (int p = 0; p < size; p++) {
            int d = src & mask;
            dest[p + offset] = (byte) ((d >> (BYTE_SIZE * (size - 1 - p))) & 0xff);
            mask >>= BYTE_SIZE;
        }
    }

    public static String[] split(String str, char delim) {
        char[] ca = str.toCharArray();
        int dc = 0;	//Delim Count
        for (int i = 0; i < ca.length; i++) {
            if (ca[i] == delim) {
                dc++;
            }
        }

        String[] sa = new String[dc + 1];
        // If there is no delim then return str itself.
        if (dc == 0) {
            sa[0] = str;
        } else {
            int index = 0;
            String s = "";
            for (int i = 0; i < ca.length; i++) {
                if (ca[i] != delim) {
                    s += ca[i];
                } else {
                    sa[index++] = s;
                    s = "";
                }
            }
            sa[index] = s;
        }

        return sa;
    }

    public static int[] randArray(int n) {
        Random rand = new Random();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        for (int i = (n - 1); i >= 1; i--) {
            int j = rand.nextInt(i + 1);
            if (i != j) {
                int temp = a[j];
                a[j] = a[i];
                a[i] = temp;
            }
        }
        return a;
    }

    public static int getField(int src, int offset, int length) {
        int val = src >> offset;
        int mask = ~(0xFFFFFFFF << length);
        return val & mask;
    }

    public static int hashcode(byte[] a) {
        if (a == null) {
            return 0;
        }

        int hash = 1;
        for (int i = 0; i < a.length; i++) {
            hash = 31 * hash + a[i];
        }
        
        return hash;
    }
    
//    public static void arrayCopy(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
//        rawJEM.bblkcpy(
//                rawJEM.toInt(src) + OBJECT.ARRAY_ELEMENT0 + srcPos,
//                rawJEM.toInt(dest) + OBJECT.ARRAY_ELEMENT0 + destPos,
//                length);
//    }
}
