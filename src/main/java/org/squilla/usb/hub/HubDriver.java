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

import com.valleycampus.usb.hub.HubOsDriver;
import java.util.List;
import javax.usb.UsbConst;
import javax.usb.UsbDevice;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbInterface;
import javax.usb.UsbPipe;
import net.sf.microlog.core.Logger;
import net.sf.microlog.core.LoggerFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.squilla.usb.UsbDeviceDriver;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class HubDriver extends UsbDeviceDriver implements Runnable {

    public static final short HC_LPSM_MASK = 0x03;
    public static final short HC_LPSM_GANGED = 0x00;
    public static final short HC_LPSM_INDIVIDUAL = 0x01;
    public static final short HC_OCPM_MASK = 0x18;
    public static final short HC_OCPM_GLOBAL = 0x00;
    public static final short HC_OCPM_INDIVIDUAL = 0x01;
    public static final short HC_OCPM_NONE = 0x10;
    public static final short HC_PIS_MASK = 0x80;
    
    private static final int BUFFER_SIZE = 64;

    private UsbDevice usbDevice;
    private UsbInterface usbInterface;
    private byte[] readBuffer;
    private byte[] statusBuffer;
    private HubRequest hubRequest;
    private UsbHubDescriptor hubDescriptor;
    private UsbEndpoint intIn;
    private UsbPipe intInPipe;
    private HubOsDriver osDriver;
    private boolean portIndicatorSupported = false;
    private boolean individualPowerSupported = false;
    private Thread processThread;
    private Logger logger = LoggerFactory.getLogger(HubDriver.class);

    public HubDriver(BundleContext bc) {
        super(bc);
        readBuffer = new byte[BUFFER_SIZE];
    }
    
    public int getClassCode() {
        return UsbConst.HUB_CLASSCODE;
    }

    public boolean attach(UsbDevice usbDevice, UsbInterface usbInterface) throws UsbException {
        this.usbDevice = usbDevice;
        this.usbInterface = usbInterface;
        
        ServiceReference ref = getBundleContext().getServiceReference(HubOsDriver.class.getName());
        if (ref == null) {
            logger.error("No HubOsDriver Service!");
            return false;
        }
        osDriver = (HubOsDriver) getBundleContext().getService(ref);
        
        hubRequest = new HubRequest(usbDevice);
        hubRequest.getHubDescriptor(readBuffer);
        hubDescriptor = HubRequest.parseUsbHubDescriptor(readBuffer);
        statusBuffer = new byte[hubDescriptor.portPwrCtrlMask().length];

        logger.debug("[Hub] Number of Ports: " + hubDescriptor.bNbrPorts());
        logger.trace("[Hub] Buffer Size: " + statusBuffer.length);

        osDriver.attach(usbDevice, hubDescriptor.bNbrPorts());

        List epList = usbInterface.getUsbEndpoints();
        for (int index = 0; index < epList.size(); index++) {
            UsbEndpoint ep = (UsbEndpoint) epList.get(index);
            if (ep.getType() == UsbConst.ENDPOINT_TYPE_INTERRUPT) {
                if (ep.getDirection() == UsbConst.ENDPOINT_DIRECTION_IN) {
                    intIn = ep;
                }
            }
        }
        
        if (intIn == null) {
            return false;
        }

        usbInterface.claim();
        intInPipe = intIn.getUsbPipe();
        intInPipe.open();

        short hc = hubDescriptor.wHubCharacteristics();

        portIndicatorSupported = (hc & HC_PIS_MASK) > 0;
        individualPowerSupported = ((hc & HC_LPSM_MASK) == HC_LPSM_INDIVIDUAL);

        logger.trace("[Hub] Port Indicator Supported: " + portIndicatorSupported);
        logger.trace("[Hub] Individual Power Switching: " + individualPowerSupported);

        for (byte port = 1; port <= hubDescriptor.bNbrPorts(); port++) {
            hubRequest.clearPortFeature(HubRequest.FEATURE_SELECTOR_C_PORT_CONNECTION, (byte) 0, port);
            // Even if HubDriver support ganged power switching, we should set feature to all ports.
//            if (individualPowerSupported || port == 1) {
//                hubRequest.setPortFeature(HubRequest.FEATURE_SELECTOR_PORT_POWER, (byte) 0, port);
//            }
            hubRequest.setPortFeature(HubRequest.FEATURE_SELECTOR_PORT_POWER, (byte) 0, port);
            if (portIndicatorSupported) {
                hubRequest.setPortFeature(HubRequest.FEATURE_SELECTOR_PORT_INDICATOR, (byte) 0, port);
            }
            int status = hubRequest.getPortStatus(port)[HubRequest.PORT_STATUS_BITS];
            if ((status & HubRequest.PS_PORT_POWER) > 0) {
                logger.trace("[Hub] Port " + port + " Power Good");
            } else {
                logger.trace("[Hub] Port " + port + " Power Bad");
            }
        }

        activate();
        
        return true;
    }

    private void activate() {
        if (processThread != null) {
            return;
        }
        
        processThread = new Thread(this, HubDriver.class.getName());
        processThread.start();
    }

    public void run() {
        while (true) {
            try {
                if (intInPipe.syncSubmit(statusBuffer) > 0) {
                    int maxPort = hubDescriptor.bNbrPorts();
                    OUTER:
                    for (int index = 0; index < statusBuffer.length; index++) {
                        byte bitmap = statusBuffer[index];
                        byte mask = 0x01;
                        for (int port = 0; port < 8; port++) {
                            int actualPort = (index * 8) + port;
                            if (actualPort <= maxPort) {
                                if ((bitmap & mask) > 0) {
                                    if (actualPort == 0) {
                                        osDriver.hubStatusChanged();
                                    } else {
                                        osDriver.portStatusChanged(actualPort);
                                    }
                                }
                                mask <<= 1;
                            } else {
                                break OUTER;
                            }
                        }
                    }
                }
            } catch (UsbException ex) {
                logger.warn(ex);
            }
        }
    }
}
