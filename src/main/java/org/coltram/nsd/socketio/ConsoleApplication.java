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
import org.coltram.nsd.communication.ProxyMessenger;

public class ConsoleApplication extends Application {

    public static void main(String args[]) {
        //Configuration.loggerConfiguration(args);
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
}
