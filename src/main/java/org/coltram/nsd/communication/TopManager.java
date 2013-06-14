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

import org.coltram.nsd.bonjour.DiscoveredZCService;
import org.teleal.cling.UpnpService;
import org.teleal.cling.model.meta.Device;

import javax.jmdns.JmDNS;
import java.util.ArrayList;
import java.util.Collection;

public class TopManager {
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(TopManager.class.getName());

    private ConnectionManager connectionManager;
    private ServiceManager serviceManager;
    private HTTPServer httpServer;
    //
    private final ArrayList<Device> devices = new ArrayList<Device>();
    private final ArrayList<DiscoveredZCService> services = new ArrayList<DiscoveredZCService>();


    public TopManager(HTTPServer httpServer) {
        this.httpServer = httpServer;
        //
        // setup of internal delegates
        //
        connectionManager = new ConnectionManager(this);
        serviceManager = new ServiceManager(this);
    }

    public HTTPServer getHttpServer() {
        return httpServer;
    }


    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public Collection<Device> getDevices() {
        return devices;
    }

    public Collection<DiscoveredZCService> getServices() {
        return services;
    }

    public void setUpnpService(UpnpService upnpService) {
        connectionManager.setUpnpService(upnpService);
    }

    public void setBonjourService(JmDNS jmdns) {
        connectionManager.setBonjourService(jmdns);
    }
}
