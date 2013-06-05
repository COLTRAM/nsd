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

import org.coltram.nsd.communication.AtomConnection;
import org.coltram.nsd.communication.ConnectionManager;
import org.coltram.nsd.communication.ProxyMessenger;
import org.coltram.nsd.communication.TopManager;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.InetSocketAddress;

public class WSServer extends WebSocketServer {
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(WSServer.class.getName());

    private TopManager topManager;
    private ProxyMessenger proxyMessenger;

    public WSServer(TopManager topManager, ProxyMessenger proxyMessenger) {
        super(new InetSocketAddress(0xDDDD));
        this.topManager = topManager;
        this.proxyMessenger = proxyMessenger;
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        log.finer(conn + " onOpen");
        topManager.getConnectionManager().add(new ConnectionAdapter(conn));
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        log.finer(conn + " onClose");
        topManager.getConnectionManager().remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        log.finer(conn + ": " + message);
        //if (!message.contains("COLTRAMAgentLogger")) ColtramLogger.logln("in>"+message);
        try {
            log.finer(message);
            JSONObject object = new JSONObject(message);
            AtomConnection atomConnection = topManager.getConnectionManager().getAtomConnection(conn);
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
        topManager.getConnectionManager().remove(conn);
        ex.printStackTrace();
    }
}

