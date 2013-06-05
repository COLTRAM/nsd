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

import org.coltram.nsd.types.LocalHost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class DiscoveredZCService {
    private static Logger log = Logger.getLogger(DiscoveredZCService.class.getName());
    private String name;
    private String type;
    private String url;
    private HashMap<String, String> config;
    private ServiceInfo info;
    private ArrayList<Action> actions = new ArrayList<Action>();
    private ArrayList<String> events = new ArrayList<String>();
    private String actionList = null;
    private String eventList = null;
    private Socket socket = null;
    private DataOutputStream socketDOS = null;

    public static class Argument {
        private String name, direction;

        public String getName() {
            return name;
        }

        public String getDirection() {
            return direction;
        }

        Argument(String name, String direction) {
            this.name = name;
            this.direction = direction;
        }
    }

    public static class Action {
        private String name;
        private ArrayList<Argument> args;

        public String getName() {
            return name;
        }

        Action(String name, ArrayList<Argument> args) {
            this.name = name;
            this.args = args;
        }

        static ArrayList<Argument> getArguments(JSONArray array) throws JSONException {
            ArrayList<Argument> args = new ArrayList<Argument>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                args.add(new Argument(object.getString("name"), object.getString("dir")));
            }
            return args;
        }
                }

    @SuppressWarnings("deprecation")
    public DiscoveredZCService(ServiceInfo info, String name, String type) {
        this.info = info;
        this.name = name;
        this.type = type;
        this.url = info.getURL();
        this.config = LocalExposedBonjourService.textBytestoHashMap(info.getTextBytes());
        String desc = getConfig("Desc");
        // desc is now a URL to the description
        try {
            if (desc != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(desc).openStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(inputLine);
                }
                bufferedReader.close();
                String serv = stringBuilder.toString();
                JSONObject service = new JSONObject(serv);
                JSONArray array = service.optJSONArray("actionList");
                actionList = array.toString();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    actions.add(new Action(object.getString("name"),
                            Action.getArguments(object.getJSONArray("args"))));
                }
                array = service.optJSONArray("eventList");
                if (array != null) {
                    eventList = array.toString();
                    for (int i = 0; i < array.length(); i++) {
                        events.add(array.getString(i));
            }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getEventList() {
        return eventList;
    }

    public Action hasAction(String name) {
        for (Action a : actions) {
            if (a.name.equals(name)) {
                return a;
            }
        }
        return null;
    }

    public String getActionList() {
        return actionList;
    }

    public String getId() {
        return info.getQualifiedName();
    }

    public int getPort() {
        return info.getPort();
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getUrl() {
        return url;
    }

    public String getConfig(String key) {
        return config.get(key);
    }

    public boolean equals(DiscoveredZCService other) {
        return getId().equals(other.getId()) &&
                name.equals(other.name) &&
                type.equals(other.type);
    }

    @SuppressWarnings("deprecation")
    public boolean isLocal() {
        return info.getInet4Address().equals(LocalHost.address);
    }

    @SuppressWarnings("deprecation")
    public Socket getSocket() throws IOException {
        log.fine("getSocket " + info.getInet4Address() + " " + info.getPort() + " " + socket);
        if (socket == null || socket.isClosed()) {
            if (socket != null) {
                socketDOS.close();
                socket.close();
            }
            log.finer("socket is null or closed, creating a new socket " + info.getInet4Address() + " " + info.getPort());
            socket = new Socket(info.getInet4Address(), info.getPort());
            socket.setKeepAlive(true);
            socketDOS = new DataOutputStream(socket.getOutputStream());
        } else {
            log.finer("socket is reusable");
        }
        return socket;
    }

    public DataOutputStream getSocketDOS() {
        return socketDOS;
    }
}
