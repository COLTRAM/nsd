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

import org.coltram.nsd.interfaces.Connection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.channels.NotYetConnectedException;

public class ReplyListener implements Runnable {
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger("ReplyListener");
    private ServerSocket serverSocket;
    private Connection connection;

    public ReplyListener(ServerSocket serverSocket, Connection connection) {
        this.serverSocket = serverSocket;
        this.connection = connection;
    }

    public void run() {
        try {
            Socket socket = serverSocket.accept();
            BufferedReader dis = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connection.send(dis.readLine());
            dis.close();
            socket.close();
        } catch (SocketTimeoutException e) {
            log.fine("reply socket timed out");
        } catch (NotYetConnectedException e) {
            Thread.yield();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
