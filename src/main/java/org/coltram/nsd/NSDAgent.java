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

package org.coltram.nsd;

import org.coltram.nsd.communication.DeviceChangeListener;
import org.coltram.nsd.communication.WSServer;
import org.coltram.nsd.debug.OneLiner;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.model.message.header.STAllHeader;
import org.teleal.cling.protocol.RetrieveRemoteDescriptors;

import javax.jmdns.JmDNS;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class NSDAgent {
    public static boolean goOn = true;

    public static void main(String args[]) {
        // configuration of the logger
        LogManager lm = LogManager.getLogManager();
        Logger root = lm.getLogger("");
        root.setLevel(Level.INFO);
        Logger rrd = Logger.getLogger(RetrieveRemoteDescriptors.class.getName());
        rrd.setLevel(Level.SEVERE);
        Handler h = root.getHandlers()[0];
        h.setFormatter(new OneLiner());
        h.setLevel(Level.INFO);
        //
        WSServer wsServer = new WSServer();
        wsServer.start();
        DeviceChangeListener deviceChangeListener = new DeviceChangeListener(wsServer.getConnectionManager(), null);
        final UpnpService upnpService = new UpnpServiceImpl(deviceChangeListener);
        upnpService.getControlPoint().search(new STAllHeader());
        wsServer.setUpnpService(upnpService);
        JmDNS jmdns1 = null;
        try {
            final JmDNS jmdns = JmDNS.create();
            jmdns1 = jmdns;
            wsServer.setBonjourService(jmdns);
            jmdns.addServiceTypeListener(deviceChangeListener);
            jmdns.registerServiceType("_tcp.local.");
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    upnpService.shutdown();
                    try {
                        jmdns.unregisterAllServices();
                        jmdns.close();
                    } catch (IOException e) {
                        Thread.yield();
                    }
                }
            });
            System.out.println("JmDNS uses IP:" + jmdns1.getInterface());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (goOn) {
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            upnpService.shutdown();
            try {
                if (jmdns1 != null) {
                    jmdns1.unregisterAllServices();
                    jmdns1.close();
                }
            } catch (IOException ee) {
                Thread.yield();
            }
        }

    }
}
