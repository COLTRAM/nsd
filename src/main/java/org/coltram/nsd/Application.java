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
import org.coltram.nsd.communication.HTTPServer;
import org.coltram.nsd.communication.TopManager;
import org.coltram.nsd.interfaces.Frame;
import org.coltram.nsd.upnp.GenericService;
import org.teleal.cling.UpnpService;
import org.teleal.cling.UpnpServiceImpl;
import org.teleal.cling.model.message.header.STAllHeader;

import javax.jmdns.JmDNS;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class Application {
    private static Logger log = Logger.getLogger(Application.class.getName());
    private TopManager topManager = null;
    private HTTPServer httpServer = null;
    private static UpnpService upnpService = null;
    private static JmDNS jmdns1 = null;

    public TopManager getTopManager() {
        return topManager;
    }

    public Application() {
        //
        // setup of web server for bonjour descriptions and other non-UPnP related served filed
        //
        httpServer = webSetUp(getProperties());
        topManager = new TopManager(httpServer);
        agentSetUp(topManager, null);
    }

    public void waitFor() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
                upnpService.shutdown();
                try {
                    jmdns1.unregisterAllServices();
                    jmdns1.close();
                } catch (IOException ee) {
                }
            }
        }
    }

    public abstract Properties getProperties();

    public static HTTPServer webSetUp(Properties properties) {
        File webroot = new File(HTTPServer.webrootName);
        if (webroot.exists()) {
            if (webroot.isDirectory()) {
                for (File f : webroot.listFiles()) {
                    f.delete();
                }
            }
            webroot.delete();
        }
        webroot.mkdir();
        HTTPServer webServer = new HTTPServer(properties);
        webServer.start();
        return webServer;
    }

    public static void agentSetUp(TopManager topManager, Frame frame) {
        //
        // UPnP and Bonjour listener
        //
        DeviceChangeListener deviceChangeListener = new DeviceChangeListener(topManager.getConnectionManager(), frame);
        upnpService = new UpnpServiceImpl(deviceChangeListener);
        upnpService.getControlPoint().search(new STAllHeader());
        topManager.setUpnpService(upnpService);
        GenericService.init();
        // sandbox
        File f = new File("ColtramSandbox");
        if (f.exists()) {
            if (!f.isDirectory()) {
                f.delete();
                f.mkdir();
            }
        } else {
            f.mkdir();
        }
        // jmdns
        try {
            final JmDNS jmdns = JmDNS.create();
            jmdns1 = jmdns;
            topManager.setBonjourService(jmdns);
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
                    }
                }
            });
            log.info("JmDNS uses IP:" + jmdns1.getInterface());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
