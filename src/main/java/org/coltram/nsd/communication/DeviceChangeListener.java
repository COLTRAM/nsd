/*
 * Copyright (c) 2012-2013. Telecom ParisTech/TSI/MM/GPAC Jean-Claude Dufourd
 * This code was developed with the Coltram project, funded by the French ANR.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * This notice must stay in all subsequent versions of this code.
 */

package org.coltram.nsd.communication;

import org.coltram.nsd.types.Frame;
import org.teleal.cling.model.meta.LocalDevice;
import org.teleal.cling.model.meta.RemoteDevice;
import org.teleal.cling.registry.Registry;
import org.teleal.cling.registry.RegistryListener;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import java.util.logging.Logger;

public class DeviceChangeListener implements RegistryListener, ServiceListener, ServiceTypeListener {
    private static Logger log = Logger.getLogger(RegistryListener.class.getName());
    private ConnectionManager connectionManager;
    private Frame mainFrame;

    public DeviceChangeListener(ConnectionManager connectionManager, Frame mainFrame) {
        this.connectionManager = connectionManager;
        this.mainFrame = mainFrame;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Bonjour
    public void serviceAdded(ServiceEvent event) {
        log.fine("Service added   : " + event.getName() + "." + event.getType());
    }

    public void serviceRemoved(ServiceEvent event) {
        log.fine("Service removed : " + event.getName() + "." + event.getType());
        connectionManager.remove(event);
        if (mainFrame != null) mainFrame.remove(event);
    }

    public void serviceResolved(ServiceEvent event) {
        log.fine("Service resolved: " + event.getInfo());
        connectionManager.add(event);
        if (mainFrame != null) mainFrame.add(event);
    }

    public void serviceTypeAdded(ServiceEvent event) {
        event.getDNS().addServiceListener(event.getType(), this);
    }

    public void subTypeForServiceTypeAdded(ServiceEvent event) {
    }

    /////////////////////////////////////////////////////////////////////////////
    // UPnP
    public void remoteDeviceDiscoveryStarted(Registry registry, RemoteDevice device) {
        log.finer("Discovery started: " + device.getDisplayString());
    }

    public void remoteDeviceDiscoveryFailed(Registry registry,
                                            RemoteDevice device,
                                            Exception ex) {
        log.fine("Discovery failed: " + device.getDetails().getFriendlyName() + " => " + ex);
    }

    public void remoteDeviceAdded(Registry registry, RemoteDevice device) {
        log.fine("Remote device available: " + device.getDetails().getFriendlyName());
        connectionManager.add(device);
        if (mainFrame != null) mainFrame.add(device);
    }

    public void remoteDeviceUpdated(Registry registry, RemoteDevice device) {
        log.finer("Remote device updated: " + device.getDetails().getFriendlyName());
    }

    public void remoteDeviceRemoved(Registry registry, RemoteDevice device) {
        log.finer("Remote device removed: " + device.getDetails().getFriendlyName());
        connectionManager.remove(device);
        if (mainFrame != null) mainFrame.remove(device);
    }

    public void localDeviceAdded(Registry registry, LocalDevice device) {
        log.finer("Local device added: " + device.getDetails().getFriendlyName());
        connectionManager.add(device);
        if (mainFrame != null) mainFrame.add(device);
    }

    public void localDeviceRemoved(Registry registry, LocalDevice device) {
        log.finer("Local device removed: " + device.getDetails().getFriendlyName());
        connectionManager.remove(device);
        if (mainFrame != null) mainFrame.remove(device);
    }

    public void beforeShutdown(Registry registry) {
        log.finer("Before shutdown, the registry has devices: "+ registry.getDevices().size());
    }

    public void afterShutdown() {
        log.finer("Shutdown of registry complete!");

    }
}
