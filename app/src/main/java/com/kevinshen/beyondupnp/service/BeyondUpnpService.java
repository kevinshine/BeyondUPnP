/*
 * Copyright (C) 2014 Kevin Shen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kevinshen.beyondupnp.service;

import android.content.Intent;
import android.os.IBinder;

import com.kevinshen.beyondupnp.BeyondApplication;
import com.kevinshen.beyondupnp.R;
import com.kevinshen.beyondupnp.core.upnp.AndroidJettyServletContainer;
import com.kevinshen.beyondupnp.core.upnp.BeyondContentDirectoryService;
import com.kevinshen.beyondupnp.util.Utils;

import org.fourthline.cling.UpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceConfiguration;
import org.fourthline.cling.android.AndroidUpnpServiceImpl;
import org.fourthline.cling.binding.annotations.AnnotationLocalServiceBinder;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.DefaultServiceManager;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.meta.DeviceDetails;
import org.fourthline.cling.model.meta.DeviceIdentity;
import org.fourthline.cling.model.meta.LocalDevice;
import org.fourthline.cling.model.meta.LocalService;
import org.fourthline.cling.model.types.UDADeviceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.Registry;
import org.fourthline.cling.transport.impl.AsyncServletStreamServerConfigurationImpl;
import org.fourthline.cling.transport.impl.AsyncServletStreamServerImpl;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.StreamServer;

import java.util.UUID;

public class BeyondUpnpService extends AndroidUpnpServiceImpl {
    private static final String TAG = BeyondUpnpService.class.getSimpleName();

    private LocalDevice mLocalDevice = null;

    @Override
    public void onCreate() {
        super.onCreate();

        //Create LocalDevice
        LocalService localService = new AnnotationLocalServiceBinder().read(BeyondContentDirectoryService.class);
        localService.setManager(new DefaultServiceManager<>(
                localService, BeyondContentDirectoryService.class));

        String macAddress = Utils.getMACAddress(Utils.WLAN0);
        //Generate UUID by MAC address
        UDN udn = UDN.valueOf(UUID.nameUUIDFromBytes(macAddress.getBytes()).toString());

        try {
            mLocalDevice = new LocalDevice(new DeviceIdentity(udn), new UDADeviceType("MediaServer"),
                    new DeviceDetails("Local Media Server"), new LocalService[]{localService});
        } catch (ValidationException e) {
            e.printStackTrace();
        }
        upnpService.getRegistry().addDevice(mLocalDevice);

        //LocalBinder instead of binder
        binder = new LocalBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected UpnpServiceConfiguration createConfiguration() {
        return new FixedAndroidUpnpServiceConfiguration();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public LocalDevice getLocalDevice() {
        return mLocalDevice;
    }

    class FixedAndroidUpnpServiceConfiguration extends AndroidUpnpServiceConfiguration {
        @Override
        public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
            // Use Jetty, start/stop a new shared instance of JettyServletContainer
            return new AsyncServletStreamServerImpl(
                    new AsyncServletStreamServerConfigurationImpl(
                            AndroidJettyServletContainer.INSTANCE,
                            networkAddressFactory.getStreamListenPort()
                    )
            );
        }
    }

    public UpnpServiceConfiguration getConfiguration() {
        return upnpService.getConfiguration();
    }

    public Registry getRegistry() {
        return upnpService.getRegistry();
    }

    public ControlPoint getControlPoint() {
        return upnpService.getControlPoint();
    }

    public class LocalBinder extends Binder {
        public BeyondUpnpService getService() {
            return BeyondUpnpService.this;
        }
    }
}
