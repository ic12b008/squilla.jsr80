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
package org.squilla.usb.hub;

import javax.usb.UsbConst;
import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.util.UsbUtil;
import org.squilla.util.ByteUtil;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class HubRequest {

    public static final byte DESCRIPTOR_TYPE_HUB = 0x29;
    public static final byte REQUEST_GET_STATUS              = 0;
    public static final byte REQUEST_CLEAR_FEATURE           = 1;
    public static final byte REQUEST_SET_FEATURE             = 3;
    public static final byte REQUEST_GET_DESCRIPTOR          = 6;
    public static final byte REQUEST_SET_DESCRIPTOR          = 7;
    public static final byte REQUEST_CLEAR_TT_BUFFER         = 8;
    public static final byte REQUEST_RESET_TT                = 9;
    public static final byte REQUEST_GET_TT_STATE            = 10;
    public static final byte REQUEST_STOP_TT                 = 11;
    public static final short FEATURE_SELECTOR_C_HUB_LOCAL_POWER = 0;
    public static final short FEATURE_SELECTOR_C_HUB_OVER_CURRENT = 1;
    public static final short FEATURE_SELECTOR_PORT_CONNECTION = 0;
    public static final short FEATURE_SELECTOR_PORT_ENABLE = 1;
    public static final short FEATURE_SELECTOR_PORT_SUSPEND = 2;
    public static final short FEATURE_SELECTOR_PORT_OVER_CURRENT = 3;
    public static final short FEATURE_SELECTOR_PORT_RESET = 4;
    public static final short FEATURE_SELECTOR_PORT_POWER = 8;
    public static final short FEATURE_SELECTOR_PORT_LOW_SPEED = 9;
    public static final short FEATURE_SELECTOR_C_PORT_CONNECTION = 16;
    public static final short FEATURE_SELECTOR_C_PORT_ENABLE = 17;
    public static final short FEATURE_SELECTOR_C_PORT_SUSPEND = 18;
    public static final short FEATURE_SELECTOR_C_PORT_OVER_CURRENT = 19;
    public static final short FEATURE_SELECTOR_C_PORT_RESET = 20;
    public static final short FEATURE_SELECTOR_PORT_TEST = 21;
    public static final short FEATURE_SELECTOR_PORT_INDICATOR = 22;
    public static final short PC_C_PORT_CONNECTION = 0x01;
    public static final short PC_C_PORT_ENABLE = 0x02;
    public static final short PC_C_PORT_SUSPEND = 0x04;
    public static final short PC_C_PORT_OVER_CURRENT = 0x08;
    public static final short PC_C_PORT_RESET = 0x10;
    public static final short PS_PORT_POWER = 0x0100;
    public static final short PS_PORT_LOW_SPEED = 0x0200;
    public static final short PS_PORT_HIGH_SPEED = 0x0400;
    public static final short PS_PORT_TEST = 0x0800;
    public static final short PS_PORT_INDICATOR = 0x1000;
    public static final int PORT_STATUS_BITS = 0;
    public static final int PORT_STATUS_CHANGE = 1;

    private UsbDevice usbDevice;

    public HubRequest(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    public void clearHubFeature(short featureSelector) throws UsbException {
        clearFeature(UsbConst.REQUESTTYPE_RECIPIENT_DEVICE, featureSelector, (short) 0);
    }

    public void clearPortFeature(short featureSelector, byte selector, byte port) throws UsbException {
        short wIndex = (short) ((selector << 8) | port);
        clearFeature(UsbConst.REQUESTTYPE_RECIPIENT_OTHER, featureSelector, wIndex);
    }

    protected void clearFeature(byte recipient, short featureSelector, short wIndex) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_DIRECTION_OUT;
        bmRequestType |= recipient;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_CLEAR_FEATURE, featureSelector, wIndex);

        usbDevice.syncSubmit(controlIrp);
    }

    public void clearTTBuffer(int deviceAddress, UsbEndpoint endpoint, short ttIndex) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_RECIPIENT_OTHER |
                UsbConst.REQUESTTYPE_DIRECTION_OUT;
        short wValue = (short)
                ((endpoint.getUsbEndpointDescriptor().bEndpointAddress() & 0x0F) |
                ((deviceAddress & 0x7F) << 4) |
                ((endpoint.getType() & 0x01) << 11) |
                (endpoint.getDirection() << 8));
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_CLEAR_TT_BUFFER, wValue, ttIndex);

        usbDevice.syncSubmit(controlIrp);
    }

    public int getBusState() {
        throw new UnsupportedOperationException("This request is no longer defined.");
    }

    public int getHubDescriptor(byte[] data) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_RECIPIENT_DEVICE |
                UsbConst.REQUESTTYPE_DIRECTION_IN;
        short wValue = (short) DESCRIPTOR_TYPE_HUB << 8;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_GET_DESCRIPTOR, wValue, (short) 0);
        controlIrp.setData(data);

        usbDevice.syncSubmit(controlIrp);

        return controlIrp.getActualLength();
    }

    public int[] getHubStatus() throws UsbException {
        return getStatus(UsbConst.REQUESTTYPE_RECIPIENT_DEVICE, (short) 0);
    }

    public int[] getPortStatus(short port) throws UsbException {
        return getStatus(UsbConst.REQUESTTYPE_RECIPIENT_OTHER, port);
    }

    protected int[] getStatus(byte recipient, short wIndex) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_DIRECTION_IN;
        bmRequestType |= recipient;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_GET_STATUS, (short) 0, wIndex);
        byte[] data = new byte[ByteUtil.INT_16_SIZE * 2];
        controlIrp.setData(data);

        usbDevice.syncSubmit(controlIrp);

        return ByteUtil.LITTLE_ENDIAN.toInt32Array(data, 0, ByteUtil.INT_16_SIZE, 2);
    }

    public int getTTState(short ttFlags, short ttPort, byte[] data) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_RECIPIENT_OTHER |
                UsbConst.REQUESTTYPE_DIRECTION_IN;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_GET_TT_STATE, ttFlags, ttPort);
        controlIrp.setData(data);

        usbDevice.syncSubmit(controlIrp);

        return controlIrp.getActualLength();
    }

    public void resetTT(short ttPort) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_RECIPIENT_OTHER |
                UsbConst.REQUESTTYPE_DIRECTION_OUT;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_RESET_TT, (short) 0, ttPort);
        
        usbDevice.syncSubmit(controlIrp);
    }

    public void setHubDescriptor(byte[] data) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_RECIPIENT_DEVICE |
                UsbConst.REQUESTTYPE_DIRECTION_OUT;
        short wValue = (short) DESCRIPTOR_TYPE_HUB << 8;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_SET_DESCRIPTOR, wValue, (short) 0);
        controlIrp.setData(data);

        usbDevice.syncSubmit(controlIrp);
    }

    public void stopTT(short ttPort) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_RECIPIENT_OTHER |
                UsbConst.REQUESTTYPE_DIRECTION_OUT;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_STOP_TT, (short) 0, ttPort);

        usbDevice.syncSubmit(controlIrp);
    }

    public void setHubFeature(short featureSelector) throws UsbException {
        setFeature(UsbConst.REQUESTTYPE_RECIPIENT_DEVICE, featureSelector, (short) 0);
    }

    public void setPortFeature(short featureSelector, byte selector, byte port) throws UsbException {
        short wIndex = (short) ((selector << 8) | port);
        setFeature(UsbConst.REQUESTTYPE_RECIPIENT_OTHER, featureSelector, wIndex);
    }

    protected void setFeature(byte recipient, short featureSelector, short wIndex) throws UsbException {
        byte bmRequestType =
                UsbConst.REQUESTTYPE_TYPE_CLASS |
                UsbConst.REQUESTTYPE_DIRECTION_OUT;
        bmRequestType |= recipient;
        UsbControlIrp controlIrp = usbDevice.createUsbControlIrp(bmRequestType, REQUEST_SET_FEATURE, featureSelector, wIndex);

        usbDevice.syncSubmit(controlIrp);
    }
    
    public static UsbHubDescriptor parseUsbHubDescriptor(byte[] buffer) {
        byte bLength = buffer[0];
        byte bDescriptorType = buffer[1];
        byte bNbrPorts = buffer[2];
        short wHubCharacteristics = UsbUtil.toShort(buffer[4], buffer[3]);
        byte bPwrOn2PwrGood = buffer[5];
        byte bHubContrCurrent = buffer[6];
        int l;
        if (bNbrPorts < 8) {
            l = 1;
        } else {
            int r = bNbrPorts % 8;
            int q = (bNbrPorts - r) / 8;
            l = r + q;
        }
        byte[] deviceRemovable = new byte[l];
        System.arraycopy(buffer, 7, deviceRemovable, 0, l);
        byte[] portPwrCtrlMask = new byte[l];
        System.arraycopy(buffer, 7 + l, portPwrCtrlMask, 0, l);
        return new UsbHubDescriptor(bLength, bDescriptorType, bNbrPorts, wHubCharacteristics,
                bPwrOn2PwrGood, bHubContrCurrent, deviceRemovable, portPwrCtrlMask);
    }
}
