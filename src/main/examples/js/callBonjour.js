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

NSDPlusPlus.logger = function (s) {
    var ta = document.getElementById("ta");
    ta.textContent = s + "\n" + ta.textContent;
};

var service = null;

function switchOn() {
    if (service != null) {
        service.lightSwitch(true);
    }
}

function switchOff() {
    if (service != null) {
        service.lightSwitch(false);
    }
}

function CB(services) {
    NSDPlusPlus.logger("CB " + services.length);
    services.onserviceavailable = onserviceavailable;
    if (services.length > 0) {
        // select first of matching services, only one is supposed to match anyway
        service = NSDPlusPlus.bindService(services[0].id);
    }
}

function onserviceavailable() {
    NSDPlusPlus.logger("onserviceavailable callback");
    NSD.getNetworkServices("zeroconf:_communicationtest._tcp.local.", CB);
}

function connectedCB() {
    NSDPlusPlus.logger("connected");
    NSD.getNetworkServices("zeroconf:_communicationtest._tcp.local.", CB);
}

window.onload = function () {
    NSDPlusPlus.addEventListener("connected", connectedCB);
    NSDPlusPlus.connect();
}