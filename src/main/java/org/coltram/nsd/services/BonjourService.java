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

package org.coltram.nsd.services;

import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class BonjourService implements Runnable {
    private static final Logger log = Logger.getLogger("BonjourService");
    private ServiceInfo serviceInfo;
    private String serviceId;
    private String ColtramServiceId;
    private int port;
    private ServerSocket serverSocket;
    private ArrayList<BonjourServiceListener> listeners = new ArrayList<BonjourServiceListener>();
    private Thread thread;
    private boolean threadStopper = false;
    private static ArrayList<BonjourService> services = new ArrayList<BonjourService>();

    public BonjourService(int port, ServiceInfo serviceInfo, String serviceId) {
        this.serviceInfo = serviceInfo;
        this.serviceId = serviceId;
        ColtramServiceId = serviceInfo.getKey();
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        services.add(this);
        log.finer("created coltram bonjour service on port " + port + " id:" + serviceId);
    }

    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public static BonjourService getServiceById(String serviceId) {
        for (BonjourService cbs : services) {
            if (serviceId.equals(cbs.ColtramServiceId)) return cbs;
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
            log.info("starting coltram bonjour service on port " + port + " id:" + serviceId);
            while (!threadStopper) {
                Socket socket = serverSocket.accept();
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message;
                while ((message = bufferedReader.readLine()) != null) {
                    log.info("relaying bonjour message to:" + serviceId + " -- " + message);
                    log.finer("receiveBonjourMsg " + message);
                    //ColtramLogger.logln("receiveBonjourMsg " + message);
                    notifyListeners(message);
                }
                bufferedReader.close();
                inputStreamReader.close();
                socket.close();
            }
        } catch (IOException e) {
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
            while (j < len && i+j < text.length && text[i + j] != '=') {
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
}
