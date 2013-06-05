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

import com.corundumstudio.socketio.SocketIOClient;
import org.coltram.nsd.interfaces.Connection;

public class ClientAdapter implements Connection {
    private SocketIOClient client;

    public ClientAdapter(SocketIOClient client) {
        this.client = client;
    }

    public void send(String message) {
        client.sendMessage(message);
    }

    public String getRemoteHostName() {
        return client.getRemoteAddress().toString();
    }

    public boolean is(Object o) {
        return client.equals(o);
    }
}
