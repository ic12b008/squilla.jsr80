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
package org.squilla.io;

import org.squilla.util.Commons;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class FrameBuffer extends ByteBuffer {

    public static final int BO_LITTLE_ENDIAN = 0;
    public static final int BO_BIG_ENDIAN = 1;

    private int byteOrder;

    public FrameBuffer(byte[] buffer, int offset, int length) {
        super(buffer, offset, length);
    }

    public FrameBuffer(byte[] buffer) {
        super(buffer);
    }

    public int getByteOrder() {
        return byteOrder;
    }

    public void setByteOrder(int byteOrder) {
        this.byteOrder = byteOrder;
    }

    public void putInt8(int i) {
        put((byte) (i & 0xFF));
    }

    public void putInt16(int i) {
        if (byteOrder == BO_LITTLE_ENDIAN) {
            put((byte) (i & 0xFF));
            put((byte) ((i >> 8) & 0xFF));
        } else {
            put((byte) ((i >> 8) & 0xFF));
            put((byte) (i & 0xFF));
        }
    }

    public void putInt32(int i) {
        if (byteOrder == BO_LITTLE_ENDIAN) {
            put((byte) (i & 0xFF));
            put((byte) ((i >> 8) & 0xFF));
            put((byte) ((i >> 16) & 0xFF));
            put((byte) ((i >> 24) & 0xFF));
        } else {
            put((byte) ((i >> 24) & 0xFF));
            put((byte) ((i >> 16) & 0xFF));
            put((byte) ((i >> 8) & 0xFF));
            put((byte) (i & 0xFF));
        }
    }

    public byte getInt8() {
        return get();
    }

    public short getInt16() {
        int s = 0;
        if (byteOrder == BO_LITTLE_ENDIAN) {
            s |= (get() & 0xFF);
            s |= (get() & 0xFF) << 8;
        } else {
            s |= (get() & 0xFF) << 8;
            s |= (get() & 0xFF);
        }
        return (short) s;
    }

    public int getInt32() {
        int s = 0;
        if (byteOrder == BO_LITTLE_ENDIAN) {
            s |= (get() & 0xFF);
            s |= (get() & 0xFF) << 8;
            s |= (get() & 0xFF) << 16;
            s |= (get() & 0xFF) << 24;
        } else {
            s |= (get() & 0xFF) << 24;
            s |= (get() & 0xFF) << 16;
            s |= (get() & 0xFF) << 8;
            s |= (get() & 0xFF);
        }
        return s;
    }

    public void putInt64(long l) {
        throw new UnsupportedOperationException();
    }

    public long getInt64() {
        byte[] b = getBytes(Commons.INT_64_SIZE);
        if (byteOrder == BO_LITTLE_ENDIAN) {
            return Commons.toLongLE(b, 0);
        } else {
            return Commons.toLongBE(b, 0);
        }
    }

    public byte[] getBytes(int length) {
        byte[] b = new byte[length];
        get(b);
        return b;
    }
}
