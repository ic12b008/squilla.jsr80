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

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class ByteBuffer extends Buffer {

    private int offset;
    private byte[] buffer;

    public ByteBuffer(byte[] buffer, int offset, int length) {
        super(length);
        this.buffer = buffer;
        this.offset = offset;
    }

    public ByteBuffer(byte[] buffer) {
        this(buffer, 0, buffer.length);
    }

    public int getOffset() {
        return offset;
    }

    public byte[] getRawArray() {
        return buffer;
    }

    public void put(byte src) {
        buffer[offset + getPosition()] = src;
        skip(1);
    }

    public void put(byte[] src) {
        System.arraycopy(src, offset, buffer, offset + getPosition(), src.length);
        skip(src.length);
    }

    public void put(byte[] src, int srcOff, int srcLen) {
        System.arraycopy(src, srcOff, buffer, offset + getPosition(), srcLen);
        skip(srcLen);
    }

    public void clean(int length) {
        for (int i = 0; i < length; i++) {
            put((byte) 0);
        }
    }

    public byte get() {
        if (getRemaining() < 1) {
            throw new BufferUnderflowException();
        }
        byte b = buffer[offset + getPosition()];
        skip(1);
        return b;
    }

    public void get(byte[] dst) {
        if (getRemaining() < dst.length) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(buffer, offset + getPosition(), dst, 0, dst.length);
        skip(dst.length);
    }

    public void get(byte[] dst, int dstOff, int dstLen) {
        if (getRemaining() < dstLen) {
            throw new BufferUnderflowException();
        }
        System.arraycopy(buffer, offset + getPosition(), dst, dstOff, dstLen);
        skip(dstLen);
    }
}
