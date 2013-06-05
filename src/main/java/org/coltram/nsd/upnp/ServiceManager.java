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

package org.coltram.nsd.upnp;

import org.teleal.cling.model.DefaultServiceManager;
import org.teleal.cling.model.meta.LocalService;

public class ServiceManager<T> extends DefaultServiceManager<T> {
    public ServiceManager(LocalService<T> service, Class<T> serviceClass) {
        super(service, serviceClass);
    }

    protected void init() {
        try {
            // The actual instance we ware going to use and hold a reference to (1:1 instance for manager)
            serviceImpl = (T)service;

            // How the implementation instance will tell us about property changes
            propertyChangeSupport = createPropertyChangeSupport(serviceImpl);
            propertyChangeSupport.addPropertyChangeListener(createPropertyChangeListener(serviceImpl));

        } catch (Exception ex) {
            throw new RuntimeException("Could not initialize implementation: " + ex, ex);
        }
    }

}
