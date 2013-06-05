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

var serviceImplementation = new NSDPlusPlus.ServiceImplementation();

serviceImplementation.lightSwitch = function (newTargetValue) {
    NSDPlusPlus.logger("Sw:" + newTargetValue + "\n");
    if (newTargetValue == false) {
        document.getElementById("light").style.cssText = "background-image: url('res/Lightbulb_off.svg')";
    } else {
        document.getElementById("light").style.cssText = "background-image: url('res/lightOn.svg')";
    }
    return {response: "switched on "+ new Date().toLocaleTimeString()};
};

window.onload = function () {
    NSDPlusPlus.addEventListener('connected', function () {
        NSDPlusPlus.expose("communicationtest", "upnp", serviceImplementation);
        NSDPlusPlus.logger("UPnP service 'communicationtest' exposed");
    });
    NSDPlusPlus.connect();
};