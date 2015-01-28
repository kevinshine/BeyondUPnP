package com.kevinshen.beyondupnp.core;

import com.kevinshen.beyondupnp.SystemService;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDAServiceType;
import org.fourthline.cling.registry.Registry;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class SystemManager {
    private static final String TAG = SystemManager.class.getSimpleName();
    public static final ServiceType CONTENT_DIRECTORY_SERVICE = new UDAServiceType("ContentDirectory");
    public static final ServiceType AV_TRANSPORT_SERVICE = new UDAServiceType("AVTransport");
    public static final ServiceType RENDERING_CONTROL_SERVICE = new UDAServiceType("RenderingControl");
    private DeviceType dmrDeviceType = new UDADeviceType("MediaRenderer");

    private static SystemManager INSTANCE = null;
    private AndroidUpnpService mUpnpService = null;
    private SystemService.SystemServiceBinder mSystemServiceBinder;

    private SystemManager() {
    }

    public static SystemManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SystemManager();
        }
        return INSTANCE;
    }

    public void setUpnpService(AndroidUpnpService upnpService) {
        mUpnpService = upnpService;
    }

    public void setSystemServiceBinder(SystemService.SystemServiceBinder systemServiceBinder) {
        this.mSystemServiceBinder = systemServiceBinder;
    }

    public void searchAllDevices() {
        mUpnpService.getControlPoint().search();
    }

    public Collection<Device> getDmrDevices() {
        return mUpnpService.getRegistry().getDevices(dmrDeviceType);
    }

    public ControlPoint getControlPoint() {
        return mUpnpService.getControlPoint();
    }

    public Registry getRegistry() {
        return mUpnpService.getRegistry();
    }

    public Collection<Device> getDmcDevices() {
        if (mUpnpService == null) return Collections.EMPTY_LIST;

        return mUpnpService.getRegistry().getDevices(CONTENT_DIRECTORY_SERVICE);
    }

    public Device getSelectedDevice() {
        return mSystemServiceBinder.getSelectedDevice();
    }

    public void setSelectedDevice(Device selectedDevice) {
        mSystemServiceBinder.setSelectedDevice(selectedDevice,mUpnpService);
    }

    public int getDeviceVolume(){
        return mSystemServiceBinder.getDeviceVolume();
    }

    public void setDeviceVolume(int currentVolume){
        mSystemServiceBinder.setDeviceVolume(currentVolume);
    }
}
