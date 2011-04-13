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

import java.util.List;
import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfaceDescriptor;
import javax.usb.UsbIrp;
import javax.usb.UsbPipe;
import javax.usb.util.StandardRequest;
import net.sf.microlog.core.Logger;
import net.sf.microlog.core.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.squilla.usb.UsbDeviceDriver;
import org.squilla.util.FifoQueue;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class BulkOnlyTransportDriver extends UsbDeviceDriver implements Runnable {

    public static final int STATE_INIT = 0;
    public static final int STATE_COMMAND_TRANSPORT = 1;
    public static final int STATE_DATA_IN = 2;
    public static final int STATE_DATA_OUT = 3;
    public static final int STATE_STATUS_TRANSPORT_1ST = 4;
    public static final int STATE_STATUS_TRANSPORT_2ND = 5;
    public static final int STATE_RESET_RECOVERY = 6;
    public static final int STATE_DONE = 7;
    public static final byte REQUEST_MASS_STORAGE_RESET = (byte) 0xFF;
    public static final byte REQUEST_GET_MAX_LUN = (byte) 0xFE;
    private static final int COMMAND_BUFFER_SIZE = 64;
    private static final int QUEUE_SIZE = 32;

    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private UsbEndpoint bulkIn;
    private UsbEndpoint bulkOut;
    private UsbPipe bulkInPipe;
    private UsbPipe bulkOutPipe;
    private byte[] commandBuffer;
    private int state;
    private byte maxLUN;
    private FifoQueue commandBlockQueue;
    private FifoQueue statusQueue;
    private CommandBlockWrapper currentCBW;
    private CommandStatusWrapper currentCSW;
    private Thread processThread;
    private Logger logger = LoggerFactory.getLogger(BulkOnlyTransportDriver.class);

    public BulkOnlyTransportDriver(BundleContext bc) {
        super(bc);
        commandBuffer = new byte[COMMAND_BUFFER_SIZE];
        commandBlockQueue = new FifoQueue(QUEUE_SIZE);
        statusQueue = new FifoQueue(QUEUE_SIZE);
        maxLUN = -1;
    }

    public int getClassCode() {
        return CLASS_MSD;
    }
    
    public boolean attach(UsbDevice usbDevice, UsbInterface usbInterface) throws UsbException {
        this.usbDevice = usbDevice;
        this.usbInterface = usbInterface;
        
        UsbInterfaceDescriptor desc = usbInterface.getUsbInterfaceDescriptor();
        logger.debug("Subclass: " + desc.bInterfaceSubClass());
        if (desc.bInterfaceProtocol() != 0x50) {
            logger.error("Attached Device is NOT BBB");
            return false;
        }
        
        List epList = usbInterface.getUsbEndpoints();
        for (int index = 0; index < epList.size(); index++) {
            UsbEndpoint ep = (UsbEndpoint) epList.get(index);
            if (ep.getType() == UsbConst.ENDPOINT_TYPE_BULK) {
                if (ep.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN) {
                    bulkIn = ep;
                } else {
                    bulkOut = ep;
                }
            }
        }

        if ((bulkIn != null) && (bulkOut != null)) {
            usbInterface.claim();
            bulkInPipe = bulkIn.getUsbPipe();
            bulkOutPipe = bulkOut.getUsbPipe();
            bulkInPipe.open();
            bulkOutPipe.open();
            activate();
            return true;
        } else {
            return false;
        }
    }
    
    private void activate() {
        if (processThread != null) {
            return;
        }
        
        processThread = new Thread(this, BulkOnlyTransportDriver.class.getName());
        processThread.start();
        
        UsbSCSI usbSCSI = new UsbSCSI(this);
        getBundleContext().registerService(UsbSCSI.class.getName(), usbSCSI, null);
    }

    public void run() {
        setState(STATE_INIT);
        while (true) {
            try {
                process();
            } catch (UsbException ex) {
                logger.warn(ex);
            }
        }
    }

    protected void setState(int state) {
        this.state = state;
    }

    public CommandStatusWrapper executeCommandBlock(CommandBlockWrapper cbw) {
        executeCommandBlockAsync(cbw);
        return waitCommandStatus();
    }

    public void executeCommandBlockAsync(CommandBlockWrapper cbw) {
        commandBlockQueue.enqueue(cbw);
    }

    public CommandStatusWrapper waitCommandStatus() {
        return (CommandStatusWrapper) statusQueue.blockingDequeue();
    }

    public void process() throws UsbException {
        logger.trace("BBB - Process");
        UsbIrp irp;
        switch (state) {
        case STATE_INIT:
            logger.trace("BBB - Init");
            getMaxLUN();
            logger.trace("MAX LUN is : " + getMaxLUN());
            setState(STATE_COMMAND_TRANSPORT);
            break;
        case STATE_COMMAND_TRANSPORT:
            logger.trace("BBB - Command Transport");
            currentCBW = (CommandBlockWrapper) commandBlockQueue.blockingDequeue();
            logger.trace("BBB - Dequeue");
            irp = bulkOutPipe.createUsbIrp();
            currentCBW.getPacket(0, commandBuffer);
            irp.setData(commandBuffer);
            irp.setLength(CommandBlockWrapper.CBW_PACKET_SIZE);
            logger.trace("BBB - Sync Submit");
            bulkOutPipe.syncSubmit(irp);
            if (currentCBW.getDataTransferLength() > 0) {
                if (currentCBW.getFlags() == CommandBlockWrapper.CBW_DIRECTION_IN) {
                    logger.trace("BBB - Next Data IN");
                    setState(STATE_DATA_IN);
                } else {
                    logger.trace("BBB - Next Data OUT");
                    setState(STATE_DATA_OUT);
                }
            } else {
                logger.trace("BBB - Next Status");
                setState(STATE_STATUS_TRANSPORT_1ST);
            }
            break;
        case STATE_DATA_IN:
            logger.trace("BBB - Data IN");
            irp = bulkInPipe.createUsbIrp();
            irp.setData(currentCBW.getData());
            irp.setLength(currentCBW.getDataTransferLength());
            try {
                bulkInPipe.syncSubmit(irp);
            } catch (UsbException ex) {
                logger.trace(ex);
                clearEndpoint(bulkIn);
            }
            setState(STATE_STATUS_TRANSPORT_1ST);
            break;
        case STATE_DATA_OUT:
            logger.trace("BBB - Data OUT");
            irp = bulkOutPipe.createUsbIrp();
            irp.setData(currentCBW.getData());
            irp.setLength(currentCBW.getDataTransferLength());
            try {
                bulkOutPipe.syncSubmit(irp);
            } catch (UsbException ex) {
                logger.trace(ex);
                clearEndpoint(bulkOut);
            }
            setState(STATE_STATUS_TRANSPORT_1ST);
            break;
        case STATE_STATUS_TRANSPORT_1ST:
        case STATE_STATUS_TRANSPORT_2ND:
            logger.trace("BBB - Status Tranport");
            irp = bulkInPipe.createUsbIrp();
            irp.setData(commandBuffer);
            irp.setLength(CommandStatusWrapper.CSW_PACKET_SIZE);
            try {
                bulkInPipe.syncSubmit(irp);
            } catch (UsbException ex) {
                logger.trace(ex);
                if (state == STATE_STATUS_TRANSPORT_1ST) {
                    // clear endpoint then 2nd attempt
                    clearEndpoint(bulkIn);
                    setState(STATE_STATUS_TRANSPORT_2ND);
                    break;
                } else {
                    setState(STATE_RESET_RECOVERY);
                    break;
                }
            }
            currentCSW = CommandStatusWrapper.parse(0, irp.getData());
            // CSW Valid ?
            if (currentCSW != null && currentCSW.getTag() == currentCBW.getTag()) {
                // Phase Error Status ?
                if (currentCSW.getStatus() != CommandStatusWrapper.STATUS_PHASE_ERROR) {
                    setState(STATE_DONE);
                    break;
                }
            }
            setState(STATE_RESET_RECOVERY);
            break;
        case STATE_DONE:
            logger.trace("BBB - Done");
            statusQueue.enqueue(currentCSW);
            setState(STATE_COMMAND_TRANSPORT);
            break;
        case STATE_RESET_RECOVERY:
            logger.trace("BBB - Reset Recovery");
            massStorageReset();
            // clear feature HALT to bulk-in
            clearEndpoint(bulkIn);
            // clear feature HALT to bulk-out
            clearEndpoint(bulkOut);
            statusQueue.enqueue(currentCSW);
            setState(STATE_COMMAND_TRANSPORT);
            break;
        }
    }

    private void clearEndpoint(UsbEndpoint ep) throws UsbException {
        StandardRequest.clearFeature(
                usbDevice,
                UsbConst.REQUESTTYPE_RECIPIENT_ENDPOINT,
                UsbConst.FEATURE_SELECTOR_ENDPOINT_HALT,
                ep.getUsbEndpointDescriptor().bEndpointAddress());
    }

    public void massStorageReset() throws UsbException {
        byte bmRequestType = UsbConst.REQUESTTYPE_TYPE_CLASS | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE | UsbConst.REQUESTTYPE_DIRECTION_OUT;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_MASS_STORAGE_RESET, (short) 0x0000, (short) 0);
        usbDevice.syncSubmit(controlIrp);
    }

    public byte getMaxLUN() throws UsbException {
        if (maxLUN == -1) {
            byte bmRequestType = UsbConst.REQUESTTYPE_TYPE_CLASS | UsbConst.REQUESTTYPE_RECIPIENT_INTERFACE | UsbConst.REQUESTTYPE_DIRECTION_IN;
            UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_GET_MAX_LUN, (short) 0x0000, (short) 0);
            controlIrp.setData(commandBuffer);
            controlIrp.setLength(1);
            usbDevice.syncSubmit(controlIrp);
            maxLUN = commandBuffer[0];
        }
        return maxLUN;
    }
}
