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

import org.coltram.nsd.interfaces.Connection;
import org.coltram.nsd.bonjour.LocalExposedBonjourService;
import org.coltram.nsd.interfaces.BonjourServiceListener;
import org.coltram.nsd.types.LocalHost;
import org.json.JSONObject;
import org.teleal.cling.model.meta.LocalDevice;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

public class AtomConnection implements BonjourServiceListener {
    private static Logger log = Logger.getLogger(AtomConnection.class.getName());
    private Connection connection = null;
    private ArrayList<LocalDevice> associatedDevices = new ArrayList<LocalDevice>();
    private ArrayList<LocalExposedBonjourService> bonjourServices = new ArrayList<LocalExposedBonjourService>();
    private boolean waitingForReply = false;
    private String id;
    private static int nextId = 0;
    private JSONObject reply = null;
    private ServerSocket serverSocket = null;
    private String exposedService = null;

    public String getExposedService() { return exposedService; }
    public void setExposedService(String s) { exposedService = s; }

    public AtomConnection(Connection connection) {
        this.connection = connection;
        id = (nextId++)+"";
    }

    public ServerSocket getServerSocket() throws IOException {
        if (serverSocket == null) {
            serverSocket = new ServerSocket(0);
            serverSocket.setSoTimeout(2000);
        }
        return serverSocket;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getId() {
        return id;
    }

    public void add(LocalDevice d) {
        associatedDevices.add(d);
    }

    public void add(LocalExposedBonjourService coltramBonjourService) {
        bonjourServices.add(coltramBonjourService);
        coltramBonjourService.registerListener(this);
    }

    public Collection<LocalDevice> devices() {
        return associatedDevices;
    }

    public Collection<LocalExposedBonjourService> bonjourServices() {
        return bonjourServices;
    }

    public synchronized void setWaitingForReply(boolean b) {
        waitingForReply = b;
    }

    public synchronized boolean isWaitingForReply() {
        return waitingForReply;
    }

    public synchronized void setReply(JSONObject obj) {
        reply = obj;
    }

    public synchronized JSONObject getReply() {
        return reply;
    }

    public void receive(String message) {
        try {
            log.finer("receiving message from CBS: " + message);
            connection.send(message);
        } catch (NotYetConnectedException e) {
            log.throwing(getClass().getName(), "receive", e);
        }
    }
}
