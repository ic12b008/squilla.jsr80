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

import org.squilla.util.Commons;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class CommandStatusWrapper {

    public static final int CSW_SIGNATURE = 0x53425355;
    public static final int CSW_PACKET_SIZE = 13;
    public static final byte STATUS_COMMAND_PASSED = 0x00;
    public static final byte STATUS_COMMAND_FAILED = 0x01;
    public static final byte STATUS_PHASE_ERROR = 0x02;

    private int tag;
    private int dataResidue;
    private byte status;

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
     * @return the dataResidue
     */
    public int getDataResidue() {
        return dataResidue;
    }

    /**
     * @param dataResidue the dataResidue to set
     */
    public void setDataResidue(int dataResidue) {
        this.dataResidue = dataResidue;
    }

    /**
     * @return the status
     */
    public byte getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(byte status) {
        this.status = status;
    }

    public static CommandStatusWrapper parse(int offset, byte[] buffer) {
        int signature = Commons.toIntLE(buffer, 0x00 + offset, 0x04, 1)[0];
        if (signature != CSW_SIGNATURE) {
            return null;
        }
        CommandStatusWrapper csw = new CommandStatusWrapper();
        csw.setTag(Commons.toIntLE(buffer, 0x04 + offset, 0x04, 1)[0]);
        csw.setDataResidue(Commons.toIntLE(buffer, 0x08 + offset, 0x04, 1)[0]);
        csw.setStatus(buffer[0x0C]);
        return csw;
    }
}
