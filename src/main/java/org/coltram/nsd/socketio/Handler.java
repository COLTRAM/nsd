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

import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.*;
import org.coltram.nsd.communication.AtomConnection;
import org.coltram.nsd.communication.ProxyMessenger;
import org.coltram.nsd.communication.TopManager;
import org.json.JSONException;
import org.json.JSONObject;

public class Handler implements ConnectListener, DisconnectListener, DataListener<String> {
    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(Handler.class.getName());

    private TopManager topManager;
    private ProxyMessenger proxyMessenger;
    private final SocketIOServer socketIOServer;

    public Handler(TopManager topManager, ProxyMessenger proxyMessenger) {
        this.topManager = topManager;
        this.proxyMessenger = proxyMessenger;
        Configuration config = new Configuration();
        config.setHostname("localhost");
        config.setPort(0xDEAD);
        socketIOServer = new SocketIOServer(config);
        socketIOServer.addConnectListener(this);
        socketIOServer.addDisconnectListener(this);
        socketIOServer.addMessageListener(this);
        socketIOServer.start();
    }

    public void onConnect(SocketIOClient client) {
        topManager.getConnectionManager().add(new ClientAdapter(client));
    }

    public void onDisconnect(SocketIOClient client) {
        topManager.getConnectionManager().remove(client);
    }

    public void onData(SocketIOClient client, String message, AckRequest ackRequest) {
        log.finer(client + ": " + message);
        try {
            log.finer(message);
            JSONObject object = new JSONObject(message);
            AtomConnection atomConnection = topManager.getConnectionManager().getAtomConnection(client);
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
                proxyMessenger.dispatcher(object, client);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}