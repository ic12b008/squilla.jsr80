/*
 * Copyright 2011 Shotaro Uchida <fantom@xmaker.mx>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.squilla.usb.msd;

import org.squilla.io.Frame;
import org.squilla.io.FrameBuffer;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class CommandBlockWrapper implements Frame {

    public static final int CBW_SIGNATURE = 0x43425355;
    public static final int CBW_PACKET_SIZE = 31;
    public static final byte CBW_DIRECTION_OUT = 0x00;
    public static final byte CBW_DIRECTION_IN = (byte) 0x80;
    private int tag;
    private int dataTransferLength;
    private byte flags;
    private byte lun;
    private byte cbLength;
    private byte[] cb;
    private byte[] data;
    
    public void pull(FrameBuffer frameBuffer) {
        frameBuffer.setByteOrder(FrameBuffer.BO_LITTLE_ENDIAN);
        frameBuffer.putInt32(CBW_SIGNATURE);
        frameBuffer.putInt32(tag);
        frameBuffer.putInt32(dataTransferLength);
        frameBuffer.put(flags);
        frameBuffer.put(lun);
        frameBuffer.put(cbLength);
        frameBuffer.put(cb, 0, cbLength);
    }
    
    public int quote() {
        return 0x0F + cbLength;
    }

    public final void drain(FrameBuffer frameBuffer) {
        throw new UnsupportedOperationException("Pull Only");
    }

    /**
     * @return the tag
     */
    public int getTag() {
        return tag;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(int tag) {
        this.tag = tag;
    }

    /**
     * @return the dataTransferLength
     */
    public int getDataTransferLength() {
        return dataTransferLength;
    }

    /**
     * @param dataTransferLength the dataTransferLength to set
     */
    public void setDataTransferLength(int dataTransferLength) {
        this.dataTransferLength = dataTransferLength;
    }

    /**
     * @return the flags
     */
    public byte getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(byte flags) {
        this.flags = flags;
    }

    /**
     * @return the lun
     */
    public byte getLUN() {
        return lun;
    }

    /**
     * @param lun the lun to set
     */
    public void setLUN(byte lun) {
        this.lun = lun;
    }

    /**
     * @return the cbLength
     */
    public byte getCBLength() {
        return cbLength;
    }

    /**
     * @param cbLength the cbLength to set
     */
    public void setCBLength(byte cbLength) {
        this.cbLength = cbLength;
    }

    /**
     * @return the cb
     */
    public byte[] getCB() {
        return cb;
    }

    /**
     * @param cb the cb to set
     */
    public void setCB(byte[] cb) {
        this.cb = cb;
    }

    /**
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(byte[] data) {
        this.data = data;
    }
}
