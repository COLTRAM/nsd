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

import org.coltram.nsd.communication.TopManager;
import org.coltram.nsd.interfaces.BonjourServiceListener;
import org.coltram.nsd.types.LocalHost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class LocalExposedBonjourService implements Runnable {
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("LocalExposedBonjourService");
    private ServiceInfo serviceInfo;
    private String serviceId;
    private String ColtramServiceId;
    private ServerSocket serverSocket;
    private ArrayList<BonjourServiceListener> listeners = new ArrayList<BonjourServiceListener>();
    private Thread thread;
    private boolean threadStopper = false;
    private static ArrayList<LocalExposedBonjourService> services = new ArrayList<LocalExposedBonjourService>();
    private ArrayList<EventSubscription> subscriptions = new ArrayList<EventSubscription>();
    private ArrayList<EventVariable> eventVariables = null;
    private TopManager topManager;

    public LocalExposedBonjourService(TopManager topManager, ServerSocket serverSocket,
                                      ServiceInfo serviceInfo, String serviceId, JSONObject service) {
        this.serviceInfo = serviceInfo;
        this.serviceId = serviceId;
        this.topManager = topManager;
        ColtramServiceId = serviceInfo.getKey();
        this.serverSocket = serverSocket;
        services.add(this);
        log.finer("created coltram bonjour service on port " + serverSocket.getLocalPort() + " id:" + serviceId);
        JSONArray events = service.optJSONArray("eventList");
        if (events != null) {
            try {
                eventVariables = new ArrayList<EventVariable>();
                for (int i = 0; i < events.length(); i++) {
                    // create event variable
                    eventVariables.add(new EventVariable(events.getString(i)));
                }
            } catch (JSONException e) {
            }
        }
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public static LocalExposedBonjourService getServiceById(String serviceId) {
        for (LocalExposedBonjourService cbs : services) {
            if (serviceId.equals(cbs.ColtramServiceId)) {
                return cbs;
            }
        }
        return null;
    }

    public void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    public void stop() {
        if (thread != null) {
            threadStopper = true;
        }
    }

    public void registerListener(BonjourServiceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void notifyListeners(String message) {
        for (BonjourServiceListener l : listeners) {
            l.receive(message);
        }
    }

    public void run() {
        try {
            log.fine("starting coltram bonjour service on port " + serverSocket.getLocalPort() + " id:" + serviceId);
            while (!threadStopper) {
                Socket socket = serverSocket.accept();
                log.fine("socket accepted on " + socket.getPort());
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message;
                while (true) {
                    message = bufferedReader.readLine();
                    log.fine("BSS receive on port " + socket.getPort() + " :" + message);
                    if (message == null) {
                        break;
                    }
                    //log.info("relaying bonjour message to:" + serviceId + " -- " + message);
                    log.fine("receiveBonjourMsg " + message);
                    JSONObject msg = new JSONObject(message);
                    String purpose = msg.getString("purpose");
                    if ("subscribe".equals(purpose)) {
                        subscribe(msg);
                    } else if ("unsubscribe".equals(purpose)) {
                        unsubscribe(msg);
                    } else {
                        notifyListeners(message);
                    }
                }
                bufferedReader.close();
                inputStreamReader.close();
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static HashMap<String, String> textBytestoHashMap(byte text[]) {
        //System.out.println(new String(text));
        HashMap<String, String> values = new HashMap<String, String>();
        int len, j;
        for (int i = 0; i < text.length; i += len + 1) {
            //System.out.println(new String(text, i, 10));
            len = text[i] & 0xFF;
            j = 0;
            while (j < len && i + j < text.length && text[i + j] != '=') {
                j++;
            }
            if (text[i + j] == '=') {
                String s1 = new String(text, i + 1, j - 1);
                String s2 = new String(text, i + j + 1, len - j);
                //System.out.println(s1 + "=" + s2);
                values.put(s1, s2);
            }
        }
        return values;
    }

    public void updateEvent(String eventName, String eventValue) {
        // get the event variable
        log.finer("update " + eventName + "=" + eventValue);
        EventVariable cbev = null;
        for (EventVariable c : eventVariables) {
            if (eventName.equals(c.getEventName())) {
                cbev = c;
                break;
            }
        }
        if (cbev == null) {
            // wrong event name
            throw new RuntimeException("wrong event name for this service: " + eventName);
        }
        // check if the value changed really
        if (!eventValue.equals(cbev.getEventValue())) {
            for (EventSubscription cbes : subscriptions) {
                if (eventName.equals(cbes.getEventName())) {
                    // this is called by the local bonjour service that is updating an event
                    // we need to cmmunicate that to the subscribing atom (not a service)
                    try {
                        // if is local
                        if (LocalHost.isLocal(cbes.getAddress())) {
                            log.fine("updateEvent Bonjour local " + eventName);
                            JSONObject object = new JSONObject();
                            object.put("purpose", "updateEvent");
                            object.put("eventName", eventName);
                            object.put("eventValue", eventValue);
                            object.put("callback", cbes.getCallback());
                            //object.put("originAtom", cbes.getOriginAtom());
                            topManager.getConnectionManager().getConnection(cbes.getOriginAtom()).send(object.toString());
                        } else {
                            // send update to this listener
                            log.fine("updateEvent Bonjour remote " + eventName);
                            Socket socket = cbes.getSocket();
                            JSONObject object = new JSONObject();
                            object.put("purpose", "updateEvent");
                            object.put("eventName", eventName);
                            object.put("eventValue", eventValue);
                            object.put("callback", cbes.getCallback());
                            //object.put("originAtom", cbes.getOriginAtom());
                            DataOutputStream dos = cbes.getSocketDOS();
                            dos.writeBytes(object.toString() + "\n");
                            dos.flush();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void subscribe(JSONObject object) throws JSONException {
        // serviceId, eventName, callback, address, port, originAtom
        String eventName = object.getString("eventName");
        log.fine("subscribe " + eventName);
        String callback = object.getString("callback");
        String address = object.getString("address");
        String port = object.getString("port");
        String listenerPort = object.optString("listenerPort");
        String originAtom = object.getString("originAtom");
        String serviceId = object.getString("serviceId");
        for (EventSubscription cbse : subscriptions) {
            if (eventName.equals(cbse.getEventName()) &&
                    callback.equals(cbse.getCallback()) &&
                    address.equals(cbse.getAddress()) &&
                    listenerPort.equals(cbse.getListenerPort()) &&
                    originAtom.equals(cbse.getOriginAtom())) {
                return;
            }
        }
        int lp = -1;
        try {
            lp = Integer.parseInt(listenerPort);
        } catch (Exception e) {
        }
        subscriptions.add(new EventSubscription(eventName, callback,
                address, lp, originAtom));
    }

    public void unsubscribe(JSONObject object) throws JSONException {
        // serviceId, eventName, address, port, originAtom
        String eventName = object.getString("eventName");
        log.fine("unsubscribe " + eventName);
        String address = object.getString("address");
        String callback = object.getString("callback");
        String originAtom = object.getString("originAtom");
        for (EventSubscription cbse : subscriptions) {
            if (eventName.equals(cbse.getEventName()) &&
                    address.equals(cbse.getAddress()) &&
                    callback.equals(cbse.getCallback()) &&
                    originAtom.equals(cbse.getOriginAtom())) {
                subscriptions.remove(cbse);
                return;
            }
        }
    }
}
