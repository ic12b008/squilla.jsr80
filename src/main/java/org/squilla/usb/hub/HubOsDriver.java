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

import javax.usb.UsbDevice;
import javax.usb.UsbException;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public interface HubOsDriver {
    
    public static final String DRIVER_CLASS = "org.squilla.usb.hub.driverClass";
    
    public void attach(UsbDevice usbDevice, int portSize);
    
    public int resetPort(int port) throws UsbException;
    
    public void hubStatusChanged();
    
    public void portStatusChanged(int port);
    
    public Object getDeviceAddress(UsbDevice usbDevice);
    
}
