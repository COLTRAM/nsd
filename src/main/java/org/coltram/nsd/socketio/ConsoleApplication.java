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

package org.coltram.nsd.socketio;

import org.coltram.nsd.Application;
import org.coltram.nsd.communication.HTTPServer;
import org.coltram.nsd.communication.ProxyMessenger;
import org.coltram.nsd.debug.Configuration;

import java.util.Properties;

public class ConsoleApplication extends Application {

    public static void main(String args[]) {
        Configuration.loggerConfiguration(args);
        new ConsoleApplication().waitFor();
    }

    public ConsoleApplication() {
        super();
        // start the socketio server
        new Handler(getTopManager(), new ProxyMessenger(getTopManager()));
        log.info("-------------------------------------");
        log.info("+ socket.io-based NSD agent started +");
        log.info("-------------------------------------");
    }

    public Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("endpoints", "http");
        properties.setProperty("urlhome", "/");
        properties.setProperty("http.port", HTTPServer.webrootPort);
        properties.setProperty("http.class", "pygmy.core.SingleThreadedHttpEndPoint");
        properties.setProperty("handler", "coltramhandler");
        properties.setProperty("coltramhandler.class", "pygmy.handlers.FileHandler");
        properties.setProperty("coltramhandler.root", HTTPServer.webrootName);
        properties.setProperty("coltramhandler.url-prefix", "/");
        properties.setProperty("mime.json", "application/json");
        properties.setProperty("mime.txt", "text/plain");
        properties.setProperty("resource.class", "org.coltram.nsd.socketio.Handler");
        properties.setProperty("resource.resourceMount", "/socket.io");
        properties.setProperty("resource.url-prefix", "/");
        return properties;
    }
}
