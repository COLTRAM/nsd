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

import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.teleal.cling.UpnpService;

import javax.jmdns.JmDNS;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class WSServer extends WebSocketServer {
    private static Logger log = Logger.getLogger(WSServer.class.getName());

    private ConnectionManager connectionManager;
    private ProxyMessenger proxyMessenger;

    public WSServer() {
        super(new InetSocketAddress(0xDEAD));
        GeneralManager GeneralManager = new GeneralManager();
        connectionManager = GeneralManager.getConnectionManager();
        proxyMessenger = new ProxyMessenger(GeneralManager);
    }
    
    public ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public void setUpnpService(UpnpService upnpService) {
        connectionManager.setUpnpService(upnpService);
    }

    public void setBonjourService(JmDNS jmdns) {
        connectionManager.setBonjourService(jmdns);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.finer(conn + " onOpen");
        connectionManager.add(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.finer(conn + " onClose");
        connectionManager.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.finer(conn + ": " + message);
        //if (!message.contains("COLTRAMAgentLogger")) ColtramLogger.logln("in>"+message);
        try {
            log.finer(message);
            JSONObject object = new JSONObject(message);
            AtomConnection atomConnection = connectionManager.getAtomConnection(conn);
            if (atomConnection == null) {
                log.info("onMessage with an unknown WebSocket(?)");
                return;
            }
            boolean notBonjour = atomConnection.bonjourServices().isEmpty();
            if (notBonjour && atomConnection.isWaitingForReply() && object.getString("purpose").equals("reply")) {
                atomConnection.setReply(object);
                atomConnection.setWaitingForReply(false);
            } else if (notBonjour && object.getString("purpose").equals("reply")) {
                log.info("getting a reply when not expecting one");
            } else {
                proxyMessenger.dispatcher(object, conn);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        connectionManager.remove(conn);
        ex.printStackTrace();
    }
}

