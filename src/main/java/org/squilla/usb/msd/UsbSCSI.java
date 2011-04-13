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
public class UsbSCSI {

    public static final byte OPCODE_TEST_UNIT_READY = 0x00;
    public static final byte OPCODE_REQUEST_SENSE = 0x03;
    public static final byte OPCODE_INQUIRY = 0x12;
    public static final byte OPCODE_MODE_SENSE_6 = 0x1A;
    public static final byte OPCODE_READ_CAPACITY = 0x25;
    public static final byte OPCODE_READ_10 = 0x28;
    public static final byte OPCODE_WRITE_10 = 0x2A;
    private static final int BLOCK_BUFFER_SIZE = 64;
    private byte[] blockBuffer;
    private CommandBlockWrapper cbw;
    private BulkOnlyTransportDriver driver;
    private int blockLength;
    private byte logicalUnitNumber;

    public UsbSCSI(BulkOnlyTransportDriver driver) {
        this.driver = driver;
        blockBuffer = new byte[BLOCK_BUFFER_SIZE];
        cbw = new CommandBlockWrapper();
    }

    /**
     * @return the blockLength
     */
    public int getBlockLength() {
        return blockLength;
    }

    /**
     * @param blockLength the blockLength to set
     */
    public void setBlockLength(int blockLength) {
        this.blockLength = blockLength;
    }


    /**
     * @return the logicalUnitNumber
     */
    public byte getLogicalUnitNumber() {
        return logicalUnitNumber;
    }

    /**
     * @param logicalUnitNumber the logicalUnitNumber to set
     */
    public void setLogicalUnitNumber(byte logicalUnitNumber) {
        this.logicalUnitNumber = logicalUnitNumber;
    }

    public int testUnitReady(byte control) {
        blockBuffer[0] = OPCODE_TEST_UNIT_READY;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE)
                );
        blockBuffer[2] = 0;
        blockBuffer[3] = 0;
        blockBuffer[4] = 0;
        blockBuffer[5] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_OUT);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 6);
        cbw.setDataTransferLength(0);
        cbw.setData(null);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }

    public int requestSense(byte alloc, byte control, byte[] buffer) {
        blockBuffer[0] = OPCODE_REQUEST_SENSE;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE)
                );
        blockBuffer[2] = 0;
        blockBuffer[3] = 0;
        blockBuffer[4] = alloc;
        blockBuffer[5] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_IN);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 6);
        cbw.setDataTransferLength(alloc);
        cbw.setData(buffer);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }

    public int inquiry(boolean evpd, byte pageCode, short alloc, byte control, byte[] buffer) {
        blockBuffer[0] = OPCODE_INQUIRY;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE) |
                (evpd ? 1 : 0)
                );
        blockBuffer[2] = pageCode;
        blockBuffer[3] = (byte) ((alloc >> 8) & 0xFF);
        blockBuffer[4] = (byte) (alloc & 0xFF);
        blockBuffer[5] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_IN);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 6);
        cbw.setDataTransferLength(alloc);
        cbw.setData(buffer);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }

    public int modeSense6(boolean dbd, byte pageCode, byte pageControl, byte alloc, byte control, byte[] buffer) {
        blockBuffer[0] = OPCODE_MODE_SENSE_6;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE) |
                ((dbd ? 1 : 0) << 3)
                );
        blockBuffer[2] = (byte) (
                ((pageControl << 6) & 0xC) |
                (pageCode & 0x3F)
                );
        blockBuffer[3] = 0; // Reserved
        blockBuffer[4] = alloc;
        blockBuffer[5] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_IN);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 6);
        cbw.setDataTransferLength(alloc);
        cbw.setData(buffer);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }

    public int readCapacity(boolean relAddr, int lba, byte pmi, byte control, byte[] buffer) {
        blockBuffer[0] = OPCODE_READ_CAPACITY;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE) |
                (relAddr ? 1 : 0)
                );
        System.arraycopy(Commons.toByteBE(lba, 4), 0, blockBuffer, 2, 4);
        blockBuffer[6] = 0; // Reserved
        blockBuffer[7] = 0; // Reserved
        blockBuffer[8] = (byte) (pmi & 0x01);
        blockBuffer[9] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_IN);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 10);
        cbw.setDataTransferLength(8);
        cbw.setData(buffer);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }

    public int read10(boolean relAddr, boolean fua, boolean dpo, int lba, short transferLength, byte control, byte[] buffer) {
        blockBuffer[0] = OPCODE_READ_10;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE) |
                ((dpo ? 1 : 0) << 4) |
                ((fua ? 1 : 0) << 3) |
                (relAddr ? 1 : 0)
                );
        System.arraycopy(Commons.toByteBE(lba, 4), 0, blockBuffer, 2, 4);
        blockBuffer[6] = 0; // Reserved
        blockBuffer[7] = (byte) ((transferLength >> 8) & 0xFF);
        blockBuffer[8] = (byte) (transferLength & 0xFF);
        blockBuffer[9] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_IN);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 10);
        cbw.setDataTransferLength(transferLength * blockLength);
        cbw.setData(buffer);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }

    public int write10(boolean relAddr, boolean fua, boolean dpo, boolean ebp, int lba, short transferLength, byte control, byte[] buffer) {
        blockBuffer[0] = OPCODE_WRITE_10;
        blockBuffer[1] = (byte) (
                ((logicalUnitNumber << 5) & 0xE) |
                ((dpo ? 1 : 0) << 4) |
                ((fua ? 1 : 0) << 3) |
                ((ebp ? 1 : 0) << 2) |
                (relAddr ? 1 : 0)
                );
        System.arraycopy(Commons.toByteBE(lba, 4), 0, blockBuffer, 2, 4);
        blockBuffer[6] = 0; // Reserved
        blockBuffer[7] = (byte) ((transferLength >> 8) & 0xFF);
        blockBuffer[8] = (byte) (transferLength & 0xFF);
        blockBuffer[9] = control;
        cbw.setLUN(logicalUnitNumber);
        cbw.setFlags(CommandBlockWrapper.CBW_DIRECTION_OUT);
        cbw.setTag(0);
        cbw.setCB(blockBuffer);
        cbw.setCBLength((byte) 10);
        cbw.setDataTransferLength(transferLength * blockLength);
        cbw.setData(buffer);
        CommandStatusWrapper csw = driver.executeCommandBlock(cbw);
        return csw.getStatus();
    }
}
