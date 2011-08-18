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

import org.squilla.usb.hub.HubOsDriver;
import java.util.Hashtable;
import javax.usb.UsbHostManager;
import javax.usb.UsbServices;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Driver;
import org.osgi.service.log.LogService;
import org.squilla.usb.hub.HubDriver;
import org.squilla.usb.msd.BulkOnlyTransportDriver;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class Activator implements BundleActivator {
    
    private LogService log;
    private UsbDeviceManager usbDeviceManager;
    
    public void start(BundleContext bc) throws Exception {
        refreshLogService(bc);
        
        log(LogService.LOG_INFO, "Start USB Services(JSR-80)");
        UsbServices services = UsbHostManager.getUsbServices();
        usbDeviceManager = new UsbDeviceManager(bc);
        services.addUsbServicesListener(usbDeviceManager);
        String driverClassName = bc.getProperty(HubOsDriver.DRIVER_CLASS);
        try {
            Class driverClass = Class.forName(driverClassName);
            bc.registerService(HubOsDriver.class.getName(), driverClass.newInstance(), null);
        } catch (Exception ex) {
            log(LogService.LOG_ERROR, "Can't load HubOsDriver implementation: " + driverClassName + ", " + ex);
        }
        
        log(LogService.LOG_INFO, "Load Standard Class Drivers");
        registerDriver(bc, new HubDriver(bc), "org.squilla.usb.HubDriver.1.0");
        registerDriver(bc, new BulkOnlyTransportDriver(bc), "org.squilla.usb.BulkOnlyTransportDriver.1.0");
    }
    
    private void registerDriver(BundleContext bc, Driver driver, String id) {
        Hashtable props = new Hashtable();
        props.put(org.osgi.service.device.Constants.DRIVER_ID, id);
        bc.registerService(Driver.class.getName(), driver, props);
    }

    public void stop(BundleContext bc) throws Exception {
        refreshLogService(bc);
        
        log(LogService.LOG_INFO, "Stop USB Services(JSR-80)");

        UsbServices services = UsbHostManager.getUsbServices();
        services.removeUsbServicesListener(usbDeviceManager);
        // Actual "Usb Services" cannot to stop.
    }
    
    private void refreshLogService(BundleContext bc) {
        ServiceReference logRef = bc.getServiceReference(LogService.class.getName());
        log = (LogService) bc.getService(logRef);
    }
    
    private void log(int level, String str) {
        try {
            log.log(level, str);
        } catch (Exception ex) {
            System.err.println("[UsbActivator] " + str);
        }
    }
}
