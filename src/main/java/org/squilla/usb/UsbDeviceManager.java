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

import java.util.Hashtable;
import java.util.List;
import javax.usb.UsbDevice;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfaceDescriptor;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class UsbDeviceManager implements UsbServicesListener {

    public static final String ID_VID = "VID";
    public static final String ID_PID = "PID";
    public static final String ID_CLASS = "CLASS";
    public static final String DEVICE_CLASS = "DEVICE_CLASS";
    public static final String DEVICE_VID_PID = "DEVICE_VID_PID";
    public static final String DEVICE_CATEGORY_NAME = "javax.usb";
    private BundleContext bc;

    public UsbDeviceManager(BundleContext bc) {
        this.bc = bc;
    }
    
    public static String getDeviceID(int vid, int pid) {
        return ID_VID + "_" + Integer.toHexString(vid & 0xFFFF) + "&" + 
               ID_PID + "_" + Integer.toHexString(pid & 0xFFFF);
    }
    
    public static String getClassCode(int classCode) {
        return ID_CLASS + "_" + Integer.toHexString(classCode & 0xFF);
    }
    
    public static String getDeviceID(UsbDevice usbDevice) {
        short vid = usbDevice.getUsbDeviceDescriptor().idVendor();
        short pid = usbDevice.getUsbDeviceDescriptor().idProduct();
        return getDeviceID(vid, pid);
    }
    
    public static String getDeviceSerial(UsbDevice usbDevice) {
        try {
            // Assume implementation is RI(jUSB).
            Class usbDeviceImpClass = Class.forName("com.ibm.jusb.UsbDeviceImp");
            Object usbDeviceOs = usbDeviceImpClass.getMethod("getUsbDeviceOsImp", null).invoke(usbDevice, null);
            // Assume OS-level implementation is aJ102.
            Class usbDeviceOsImpClass = Class.forName("com.valleycampus.usb.host.aJ102UsbDeviceOsImp");
            Object deviceAddress = usbDeviceOsImpClass.getMethod("getDeviceAddress", null).invoke(usbDeviceOs, null);

            return "DEVADDR_" + deviceAddress.toString();
        } catch (Exception ex) {
        }
        // FIXME: This is not unique number.
        return "PORT_" + usbDevice.getParentUsbPort().getPortNumber();
    }

    private void registerDevice(UsbDevice usbDevice, Object device, int classCode) {
        Hashtable props = new Hashtable();
        
        props.put(org.osgi.service.device.Constants.DEVICE_CATEGORY, DEVICE_CATEGORY_NAME);
        try {
            props.put(org.osgi.service.device.Constants.DEVICE_DESCRIPTION, usbDevice.getProductString());
        } catch (Exception ex) {
        }
        props.put(org.osgi.service.device.Constants.DEVICE_SERIAL, getDeviceSerial(usbDevice));
        
        props.put(DEVICE_VID_PID, getDeviceID(usbDevice));
        props.put(DEVICE_CLASS, getClassCode(classCode));
        bc.registerService(UsbDevice.class.getName(), device, props);
    }

    public void usbDeviceAttached(UsbServicesEvent event) {
        UsbDevice usbDevice = event.getUsbDevice();
        byte deviceClass = usbDevice.getUsbDeviceDescriptor().bDeviceClass();
        if (deviceClass == 0x00) {
            // Class defined at interface level
            List interfaceList = usbDevice.getActiveUsbConfiguration().getUsbInterfaces();
            for (int i = 0; i < interfaceList.size(); i++) {
                UsbInterface usbInterface = (UsbInterface) interfaceList.get(i);
                UsbInterfaceDescriptor desc = usbInterface.getUsbInterfaceDescriptor();
                int classCode = desc.bInterfaceClass();
                registerDevice(usbDevice, usbInterface, classCode);
            }
        } else {
            registerDevice(usbDevice, usbDevice, deviceClass);
        }
    }

    public void usbDeviceDetached(UsbServicesEvent event) {
        UsbDevice detachedDevice = event.getUsbDevice();
        try {
            ServiceReference[] refs = bc.getServiceReferences(
                    UsbDevice.class.getName(),
                    "(&(" + org.osgi.service.device.Constants.DEVICE_SERIAL + "=" +
                    getDeviceSerial(detachedDevice) + ")(" +
                    DEVICE_VID_PID + "=" + getDeviceID(detachedDevice) + "))"
                    );
            if (refs != null && refs.length > 0) {
                bc.ungetService(refs[0]);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
