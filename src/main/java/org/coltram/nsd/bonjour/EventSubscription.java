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

package org.coltram.nsd.bonjour;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class EventSubscription {
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("EventSubscription");
    private String eventName;
    private String callback;
    private String address;
    private InetAddress iaddress;
    private int listenerPort;
    private String originAtom;
    private Socket socket = null;
    private DataOutputStream socketDOS = null;

    public String getEventName() {
        return eventName;
    }

    public String getCallback() {
        return callback;
    }

    public String getAddress() {
        return address;
    }

    public int getListenerPort() {
        return listenerPort;
    }

    public String getOriginAtom() {
        return originAtom;
    }

    public EventSubscription(String eventName, String callback,
                             String address, int listenerPort, String originAtom) {
        this.eventName = eventName;
        this.callback = callback;
        this.address = address;
        this.listenerPort = listenerPort;
        this.originAtom = originAtom;
        try {
            iaddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            Thread.yield();
        }
    }

    public Socket getSocket() throws IOException {
        if (socket == null || socket.isClosed()) {
            if (socket != null) {
                socketDOS.close();
                socket.close();
            }
            log.finer("new socket to "+iaddress.getHostAddress()+":"+listenerPort);
            socket = new Socket(iaddress, listenerPort);
            socket.setKeepAlive(true);
            socketDOS = new DataOutputStream(socket.getOutputStream());
        }
        return socket;
    }

    public DataOutputStream getSocketDOS() { return socketDOS; }
}
