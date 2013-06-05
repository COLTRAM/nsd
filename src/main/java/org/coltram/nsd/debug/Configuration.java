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

package org.coltram.nsd.debug;

import org.teleal.cling.protocol.RetrieveRemoteDescriptors;

import java.io.IOException;
import java.util.logging.*;

public class Configuration {
    private final static boolean LOGFILE = false;

    public static void loggerConfiguration(String args[]) {
        LogManager lm = LogManager.getLogManager();
        Logger logger = lm.getLogger("");
        Handler handler = logger.getHandlers()[0];
        handler.setLevel(Level.INFO);
        handler.setFormatter(new OneLiner());
        handler.setFilter(new Filter() {
            public boolean isLoggable(LogRecord logRecord) {
                return !logRecord.getSourceClassName().contains("RetrieveRemoteDescriptors");
            }
        });
        try {
            if (LOGFILE || (args.length > 0 && "-log".equalsIgnoreCase(args[0]))) {
                handler = new FileHandler("coltramAgent.log");
                handler.setLevel(Level.FINEST);
                handler.setFormatter(new OneLiner());
                logger.addHandler(handler);
            }
        } catch(IOException e) {}
        logger.setLevel(Level.FINEST);
        logger = Logger.getLogger(RetrieveRemoteDescriptors.class.getName());
        logger.setLevel(Level.SEVERE);
        logger = Logger.getLogger("org.teleal.cling");
        logger.setLevel(Level.FINEST);
        logger = Logger.getLogger("org.teleal.cling.registry");
        logger.setLevel(Level.INFO);
        logger = Logger.getLogger("org.coltram.agent");
        logger.setLevel(Level.INFO);
        logger = Logger.getLogger("java");
        logger.setLevel(Level.INFO);
        logger = Logger.getLogger("javax");
        logger.setLevel(Level.INFO);
    }
}
