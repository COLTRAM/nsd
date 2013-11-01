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

import org.coltram.nsd.communication.AtomConnection;
import org.coltram.nsd.communication.TopManager;
import org.coltram.nsd.types.LocalHost;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

public class BonjourProcessor {
    private static Logger log = Logger.getLogger(BonjourProcessor.class.getName());

    private TopManager topManager;
    private ArrayList<EventListener> listeners = new ArrayList<EventListener>();

    public BonjourProcessor(TopManager topManager) {
        this.topManager = topManager;
    }

    public void updateEvent(String eventName, String eventValue, String serviceId) {
        log.fine("updateEvent Bonjour "+eventName);
        LocalExposedBonjourService exposedService = LocalExposedBonjourService.getServiceById(serviceId);
        if (exposedService == null) {
            throw new RuntimeException("updating event without an exposed service");
        }
        exposedService.updateEvent(eventName, eventValue);
    }

    public void xscribe(JSONObject object, String serviceId, AtomConnection connection)
            throws JSONException {
        final DiscoveredZCService bonjourService = topManager.getServiceManager().findBonjourService(serviceId);
        if (bonjourService == null) {
            log.info("no service with id " + serviceId + " in un/subscribe");
            return;
        }
        log.finer("xscribe Bonjour " + object.getString("eventName")+" local?"+bonjourService.isLocal());
        if (bonjourService.isLocal()) {
            // find the LocalExposedBonjourService in question
            LocalExposedBonjourService localcbs = LocalExposedBonjourService.getServiceById(serviceId);
            // send the info to that service
            object.put("address", "localhost");
            object.put("port", bonjourService.getPort());
            object.put("originAtom", connection.getId());
            if ("subscribe".equals(object.optString("purpose"))) {
                localcbs.subscribe(object);
            } else {
                localcbs.unsubscribe(object);
            }
        } else {
            try {
                Socket socket = bonjourService.getSocket();
                final int port = socket.getPort();
                int listenerPort = 0;
                String callback = object.optString("callback");
                if ("subscribe".equals(object.optString("purpose"))) {
                    EventListener eventListener = new EventListener(connection, callback);
                    listeners.add(eventListener);
                    listenerPort = eventListener.getListenerPort();
                } else {
                    for (EventListener l : listeners) {
                        if (l.is(connection, callback)) {
                            // todo implement pool of listener ports
                            listeners.remove(l);
                            listenerPort = l.getListenerPort();
                            l.stop();
                            break;
                        }
                    }
                }
                object.put("address", LocalHost.name);
                object.put("port", port + "");
                object.put("listenerPort", listenerPort + "");
                object.put("originAtom", connection.getId());
                DataOutputStream dos = bonjourService.getSocketDOS();
                dos.writeBytes(object.toString() + "\n");
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void callAction(JSONObject object, String serviceId,
                           final AtomConnection connection, String callBack) throws JSONException {
        final DiscoveredZCService bonjourService = topManager.getServiceManager().findBonjourService(serviceId);
        if (bonjourService == null) {
            log.info("no service with id " + serviceId + " in callAction");
            return;
        }
        if (bonjourService.isLocal()) {
            // find the LocalExposedBonjourService in question
            LocalExposedBonjourService localcbs = LocalExposedBonjourService.getServiceById(serviceId);
            // send the info to that service
            object.put("originAtom", connection.getId());
            localcbs.notifyListeners(object.toString());
        } else {
            try {
                Socket socket = bonjourService.getSocket();
                int replyPort = -1;
                final InetAddress inetAddress = socket.getInetAddress();
                if (callBack != null) {
                    // wait for reply on the same socket
                    ServerSocket serverSocket = connection.getServerSocket();
                    replyPort = serverSocket.getLocalPort();
                    log.finer("start server for reply " + serverSocket.getLocalPort());
                    new Thread(new ReplyListener(serverSocket, connection.getConnection())).start();
                    Thread.yield();
                }
                String ia = inetAddress.toString();
                if (ia.startsWith("/")) {
                    ia = ia.substring(1);
                }
                object.put("address", LocalHost.name);
                object.put("replyPort", replyPort + "");
                object.put("originAtom", connection.getId());
                DataOutputStream dos = bonjourService.getSocketDOS();
                dos.writeBytes(object.toString() + "\n");
                dos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static final char[] _nibbleToHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static String toHex(byte[] code) {
        StringBuilder result = new StringBuilder(2 * code.length);

        for (byte aCode : code) {
            int b = aCode & 0xFF;
            result.append(_nibbleToHex[b / 16]);
            result.append(_nibbleToHex[b % 16]);
        }

        return result.toString();
    }

    public void exposeService(String serviceType, String friendlyName, String deviceType,
                              JSONObject service, AtomConnection connection) throws JSONException {
        try {
            String type = serviceType.substring(9);
            final HashMap<String, String> values = new HashMap<String, String>();
            values.put("DName", friendlyName);
            String desc = service.toString();
            values.put("Desc", topManager.getHttpServer().addResource(UUID.randomUUID().toString() + ".json", desc.getBytes()));
            values.put("txtvers", "1");
            Random random = new Random();
            byte[] name = new byte[6];
            random.nextBytes(name);
            ServerSocket serverSocket = new ServerSocket(0);
            int bonjourServicePort = serverSocket.getLocalPort();
            ServiceInfo si = ServiceInfo.create(type, deviceType + "_" + toHex(name), bonjourServicePort, 0, 0, values);
            LocalExposedBonjourService exposedService = new LocalExposedBonjourService(topManager, serverSocket, si,
                    si.getQualifiedName(), service);
            connection.add(exposedService);
            topManager.getConnectionManager().getJmdns().registerService(si);
            exposedService.start();
            connection.setExposedService(si.getQualifiedName());
            log.fine("register " + si.getQualifiedName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void unexposeService(String serviceId, AtomConnection connection) throws JSONException {
        LocalExposedBonjourService exposedService = LocalExposedBonjourService.getServiceById(serviceId);
        ServiceInfo si = exposedService.getServiceInfo();
        exposedService.stop();
        connection.remove(exposedService);
        topManager.getConnectionManager().getJmdns().unregisterService(si);
        log.fine("register " + si.getQualifiedName());
    }

    /**
     * Process purpose:reply messages
     * only happens in Bonjour
     *
     * @param object the JSON message
     * @throws JSONException as usual when manipulating JSON objects
     */
    public void processReply(JSONObject object) throws JSONException {
        String address = object.getString("address");
        if ("localhost".equals(address)) {
            AtomConnection connection = topManager.getConnectionManager().getConnectionByAtomId(object.getString("originAtom"));
            try {
                connection.getConnection().send(object.toString());
            } catch (NotYetConnectedException e) {
            }
        } else {
            try {
                InetAddress inetAddress = InetAddress.getByName(address);
                int port = object.getInt("replyPort");
                log.fine("new socket to " + inetAddress.getHostAddress() + ":" + port);
                Socket socket = new Socket(inetAddress, port);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
                BufferedWriter bw = new BufferedWriter(outputStreamWriter);
                System.out.println("sending reply");
                bw.write(object.toString());
                bw.flush();
                bw.close();
                outputStreamWriter.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
