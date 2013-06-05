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

package org.coltram.nsd.types;

import java.io.IOException;
import java.net.InetAddress;

public class LocalHost {
    public static InetAddress address;
    public static String name;

    static {
        try {
            address = InetAddress.getLocalHost();
            name = address.getHostName();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isLocal(String address) {
        return address.equals(LocalHost.address) || address.equals("localhost");
    }

}
