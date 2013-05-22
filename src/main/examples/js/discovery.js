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

var serviceType = null;

NSDPlusPlus.logger = function (s) {
    var ta = document.getElementById("ta");
    ta.textContent = s + "\n" + ta.textContent;
};

function getThisTypeLoose() {
    serviceType = document.getElementById("servicetype").value;
    NSDPlusPlus.discover(serviceType, CB1);
    NSDPlusPlus.logger("in getThisTypeLoose, called discover with "+serviceType);
}

function getThisType() {
    serviceType = document.getElementById("servicetype").value;
    NSD.getNetworkServices(serviceType, CB);
    NSDPlusPlus.logger("in getThisType, called getNetworkServices with "+serviceType);
}

function CB(services) {
    NSDPlusPlus.logger("CB " + services.length);
    services.onserviceavailable = onserviceavailable;
    var table = document.getElementById("servicetable");
    empty(table);
    table.appendChild(div("span12 label", "-"));
    for (var i = 0; i < services.length; i++) {
        var typ = (services[i].type.indexOf("upnp:") == 0 ? "span1 label label-success" : "span1 label label-info");
        table.appendChild(div(typ, "id"));
        table.appendChild(div("span10", services[i].id));
        table.appendChild(div(typ, "name"));
        table.appendChild(div("span10", services[i].name));
        table.appendChild(div(typ, "type"));
        table.appendChild(div("span10", services[i].type));
        table.appendChild(div(typ, "url"));
        table.appendChild(div("span10", services[i].url));
        table.appendChild(div(typ, "config"));
        table.appendChild(div("span10", services[i].config));
        table.appendChild(div("span2 label label-important", "online"));
        table.appendChild(div("span9", services[i].online));
        table.appendChild(div("span12 label", "-"));
    }
}

function CB1(services) {
    NSDPlusPlus.logger("CB1 " + services.length);
    services.onserviceavailable = onserviceavailable1;
    var table = document.getElementById("servicetable");
    empty(table);
    table.appendChild(div("span12 label", "-"));
    for (var i = 0; i < services.length; i++) {
        var typ = (services[i].type.indexOf("upnp:") == 0 ? "span1 label label-success" : "span1 label label-info");
        table.appendChild(div(typ, "id"));
        table.appendChild(div("span10", services[i].id));
        table.appendChild(div(typ, "name"));
        table.appendChild(div("span10", services[i].name));
        table.appendChild(div(typ, "type"));
        table.appendChild(div("span10", services[i].type));
        table.appendChild(div(typ, "url"));
        table.appendChild(div("span10", services[i].url));
        table.appendChild(div(typ, "config"));
        table.appendChild(div("span10", services[i].config));
        table.appendChild(div("span2 label label-important", "online"));
        table.appendChild(div("span9", services[i].online));
        table.appendChild(div("span12 label", "-"));
    }
}

function div(style, content) {
    var div = document.createElement("div");
    div.setAttribute("class", style);
    div.textContent = content;
    return div;
}

function empty(selector) {
    var child = selector.firstElementChild;
    while (child != null) {
        selector.removeChild(child);
        child = selector.firstElementChild;
    }
}

function onserviceavailable() {
    //NSDPlusPlus.logger("onserviceavailable callback");
    NSD.getNetworkServices(serviceType, CB);
    NSDPlusPlus.logger("in onserviceavailable, called getNetworkServices with "+serviceType);
}

function onserviceavailable1() {
    //NSDPlusPlus.logger("onserviceavailable1 callback");
    NSDPlusPlus.discover(serviceType, CB1);
    NSDPlusPlus.logger("in onserviceavailable1, called discover with "+serviceType);
}

function connectedCB() {
    NSDPlusPlus.logger("connected");
}

window.onload = function () {
    NSDPlusPlus.addEventListener("connected", connectedCB);
    NSDPlusPlus.connect();
}