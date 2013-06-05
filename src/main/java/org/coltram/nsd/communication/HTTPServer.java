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

import org.coltram.nsd.types.LocalHost;
import pygmy.core.Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class HTTPServer extends Server {
    public static final String webrootName = "tmpwww";
    public static final String webrootPort = "55005";

    public HTTPServer(Properties config) {
        super(config);
    }

    /**
     * add a resource to the web server
     *
     * @param name    the name of the resource, from which its URL is constructed
     * @param content the content of the resource
     * @return the URL of the resource
     */
    public String addResource(String name, byte content[]) {
        File res = new File(webrootName + File.separatorChar + name);
        if (res.exists()) {
            res.delete();
        }
        try {
            FileOutputStream fos = new FileOutputStream(res);
            fos.write(content);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "http://" + LocalHost.name + ":" + webrootPort + "/" + name;
    }

}
