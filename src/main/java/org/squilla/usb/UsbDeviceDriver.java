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
package org.squilla.usb;

import java.util.List;
import java.util.Vector;
import javax.usb.UsbDevice;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public abstract class UsbDeviceDriver implements Driver {
    
    public static final int CLASS_HID = 0x03;
    public static final int CLASS_MSD = 0x08;
    public static final int CLASS_VENDOR = 0xFF;
    public static final int MATCH_CLASS = 4;
    public static final int MATCH_VID_PID = 2;
    private List deviceIDList;
    private BundleContext bc;
    
    public UsbDeviceDriver(BundleContext bc) {
        this.bc = bc;
        deviceIDList = new Vector();
    }
    
    protected BundleContext getBundleContext() {
        return bc;
    }
    
    public abstract int getClassCode();
    
    public void addDeviceID(int vid, int pid) {
        deviceIDList.add(UsbDeviceManager.getDeviceID(vid, pid));
    }
    
    private boolean isIDMatched(String id) {
        for (int i = 0; i < deviceIDList.size(); i++) {
            String myID = deviceIDList.get(i).toString();
            if (myID.equals(id)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isClassMatched(String deviceClass) {
        return deviceClass.equals(UsbDeviceManager.getClassCode(this.getClassCode()));
    }
    
    public abstract boolean attach(UsbDevice usbDevice, UsbInterface usbInterface) throws UsbException;

    public int match(ServiceReference sr) throws Exception {
        // Make sure device category is jsr80
        String cat = sr.getProperty(org.osgi.service.device.Constants.DEVICE_CATEGORY).toString();
        if (!cat.equals(UsbDeviceManager.DEVICE_CATEGORY_NAME)) {
            return Device.MATCH_NONE;
        }
        
        String deviceClass = sr.getProperty(UsbDeviceManager.DEVICE_CLASS).toString();
        if (isClassMatched(deviceClass)) {
            String driverID = sr.getProperty(UsbDeviceManager.DEVICE_VID_PID).toString();
            if (isIDMatched(driverID)) {
                return MATCH_CLASS | MATCH_VID_PID;
            } else {
                return MATCH_CLASS;
            }
        } else {
            return Device.MATCH_NONE;
        }
    }

    public String attach(ServiceReference sr) throws Exception {
        final UsbDevice usbDevice;
        final UsbInterface usbInterface;
        
        Object device = bc.getService(sr);
        if (device instanceof UsbDevice) {
            usbDevice = (UsbDevice) device;
            usbInterface = (UsbInterface) usbDevice
                    .getActiveUsbConfiguration()
                    .getUsbInterfaces().get(0);
        } else if (device instanceof UsbInterface) {
            usbInterface = (UsbInterface) device;
            usbDevice = usbInterface.getUsbConfiguration().getUsbDevice();
        } else {
            throw new UsbException("Service is not UsbDevice");
        }
        
        attach(usbDevice, usbInterface);

        return null;
    }
}
