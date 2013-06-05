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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class EventListener implements Runnable {
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("EventListener");
    private Thread thread;
    private boolean threadStopper = false;
    private ServerSocket serverSocket;
    private AtomConnection connection;
    private int port;
    private String callback; // to differentiate listeners from the same atom and same eventName

    public EventListener(AtomConnection connection, String callback) {
        this.connection = connection;
        this.callback = callback;
        try {
            serverSocket = new ServerSocket(0);
            port = serverSocket.getLocalPort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        thread = new Thread(this);
        thread.start();
    }

    public boolean is(AtomConnection connection, String callback) {
        return this.connection == connection && callback.equals(this.callback);
    }

    public int getListenerPort() { return port; }

    //todo a stop that will work, because currently it does not because of blocking io: readline()
    public void stop() {
        if (thread != null) {
            threadStopper = true;
        }
    }

    public void run() {
        try {
            while (!threadStopper) {
                Socket socket = serverSocket.accept();
                InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String message;
                while ((message = bufferedReader.readLine()) != null) {
                    //log.info("relaying bonjour message to:" + serviceId + " -- " + message);
                    log.finer("receiveBonjourMsg " + message);
                    connection.getConnection().send(message);
                }
                bufferedReader.close();
                inputStreamReader.close();
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
