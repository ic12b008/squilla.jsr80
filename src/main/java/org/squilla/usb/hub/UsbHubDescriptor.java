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

import javax.usb.UsbDescriptor;

/**
 *
 * @author Shotaro Uchida <fantom@xmaker.mx>
 */
public class UsbHubDescriptor implements UsbDescriptor {

    private byte bLength;
    private byte bDescriptorType;
    private byte bNbrPorts;
    private short wHubCharacteristics;
    private byte bPwrOn2PwrGood;
    private byte bHubContrCurrent;
    private byte[] deviceRemovable;
    private byte[] portPwrCtrlMask;

    public UsbHubDescriptor(byte bLength, byte bDescriptorType, byte bNbrPorts, short wHubCharacteristics,
            byte bPwrOn2PwrGood, byte bHubContrCurrent, byte[] deviceRemovable, byte[] portPwrCtrlMask) {
        this.bLength = bLength;
        this.bDescriptorType = bDescriptorType;
        this.bNbrPorts = bNbrPorts;
        this.wHubCharacteristics = wHubCharacteristics;
        this.bPwrOn2PwrGood = bPwrOn2PwrGood;
        this.bHubContrCurrent = bHubContrCurrent;
        this.deviceRemovable = deviceRemovable;
        this.portPwrCtrlMask = portPwrCtrlMask;
    }

    public byte bLength() {
        return bLength;
    }

    public byte bDescriptorType() {
        return bDescriptorType;
    }

    public byte bNbrPorts() {
        return bNbrPorts;
    }

    public short wHubCharacteristics() {
        return wHubCharacteristics;
    }

    public byte bPwrOn2PwrGood() {
        return bPwrOn2PwrGood;
    }

    public byte bHubContrCurrent() {
        return bHubContrCurrent;
    }

    public byte[] deviceRemovable() {
        return deviceRemovable;
    }

    public byte[] portPwrCtrlMask() {
        return portPwrCtrlMask;
    }
}
