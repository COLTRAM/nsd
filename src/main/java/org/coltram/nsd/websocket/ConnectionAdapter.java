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

package org.coltram.nsd.websocket;

import org.coltram.nsd.interfaces.Connection;
import org.java_websocket.WebSocket;

public class ConnectionAdapter implements Connection {
    private WebSocket connection;

    public ConnectionAdapter(WebSocket connection) {
        this.connection = connection;
    }

    public void send(String message) {
        connection.send(message);
    }

    public String getRemoteHostName() {
        return connection.getRemoteSocketAddress().getHostName();
    }

    public boolean is(Object o) {
        return connection == o;
    }
}
