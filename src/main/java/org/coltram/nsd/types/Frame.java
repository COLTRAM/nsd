package org.coltram.nsd.types;

import org.teleal.cling.model.meta.Device;

import javax.jmdns.ServiceEvent;

public interface Frame {
    public void add(Device device);
    public void remove(Device device);
    public void add(ServiceEvent event);
    public void remove(ServiceEvent event);
}
