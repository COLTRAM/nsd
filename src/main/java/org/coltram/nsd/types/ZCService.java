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

package org.coltram.nsd.types;

import org.coltram.nsd.services.BonjourService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class ZCService {
    private static Logger log = Logger.getLogger(ZCService.class.getName());
    private String name;
    private String type;
    private String url;
    private HashMap<String, String> config;
    private ServiceInfo info;
    private ArrayList<Action> actions = new ArrayList<Action>();
    private String actionList = null;
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

        public ArrayList<Argument> getArgs() {
            return args;
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

        public boolean hasOutArgs() {
            for (Argument arg : args) {
                if (arg.getDirection().equalsIgnoreCase("out")) {
                    return true;
                }
            }
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public ZCService(ServiceInfo info, String name, String type) {
        this.info = info;
        this.name = name;
        this.type = type;
        this.url = info.getURL();
        this.config = BonjourService.textBytestoHashMap(info.getTextBytes());
        String desc = getConfig("Desc");
        // desc is now a URL to the description
        try {
            if (desc != null) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL(desc).openStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String inputLine;
                while ((inputLine = bufferedReader.readLine()) != null) stringBuilder.append(inputLine);
                bufferedReader.close();
                actionList = stringBuilder.toString();
                JSONArray array = new JSONArray(actionList);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject object = array.getJSONObject(i);
                    actions.add(new Action(object.getString("name"),
                            Action.getArguments(object.getJSONArray("args"))));
                }
                //actionList = actionList.replaceAll("\"", "\\\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public boolean equals(ZCService other) {
        return getId().equals(other.getId()) &&
                name.equals(other.name) &&
                type.equals(other.type);
    }

    public static InetAddress localAddress;

    static {
        try {
            localAddress = InetAddress.getLocalHost();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public boolean isLocal() {
        return info.getInet4Address().equals(localAddress);
    }

    @SuppressWarnings("deprecation")
    public Socket getSocket() throws IOException {
        log.info("getSocket " + info.getInet4Address() + " " + info.getPort() + " " + socket);
        if (socket == null || socket.isClosed()) {
            if (socket != null) {
                socketDOS.close();
                socket.close();
            }
            log.info("socket is null or closed, creating a new socket " + info.getInet4Address() + " " + info.getPort() + " " + localAddress + " " + (info.getPort() - 1000));
            socket = new Socket(info.getInet4Address(), info.getPort(), localAddress, info.getPort() - 1000);
            socket.setKeepAlive(true);
            socketDOS = new DataOutputStream(socket.getOutputStream());
        } else {
            log.info("socket is reusable");
        }
        return socket;
    }

    public DataOutputStream getSocketDOS() {
        return socketDOS;
    }
}
