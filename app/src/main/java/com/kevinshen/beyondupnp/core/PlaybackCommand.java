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
package com.kevinshen.beyondupnp.core;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.kevinshen.beyondupnp.ui.NowplayingFragment;

import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.support.avtransport.callback.GetMediaInfo;
import org.fourthline.cling.support.avtransport.callback.GetPositionInfo;
import org.fourthline.cling.support.avtransport.callback.GetTransportInfo;
import org.fourthline.cling.support.avtransport.callback.Pause;
import org.fourthline.cling.support.avtransport.callback.Play;
import org.fourthline.cling.support.avtransport.callback.Seek;
import org.fourthline.cling.support.avtransport.callback.SetAVTransportURI;
import org.fourthline.cling.support.avtransport.callback.Stop;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportInfo;
import org.fourthline.cling.support.model.TransportState;
import org.fourthline.cling.support.renderingcontrol.callback.GetVolume;
import org.fourthline.cling.support.renderingcontrol.callback.SetVolume;

/**
 * Execute UPnP command,send callback to handler,refresh UI
 */
public class PlaybackCommand {
    private static final String TAG = PlaybackCommand.class.getSimpleName();

    public static void playNewItem(final String uri, final String metadata) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        final Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            final ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new Stop(avtService) {
                @Override
                public void success(ActionInvocation invocation) {
                    cp.execute(new SetAVTransportURI(avtService, uri, metadata) {
                        @Override
                        public void success(ActionInvocation invocation) {
                            //Second,Set Play command.
                            cp.execute(new Play(avtService) {
                                @Override
                                public void success(ActionInvocation invocation) {
                                    Log.i(TAG, "PlayNewItem success:" + uri);
                                }

                                @Override
                                public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
                                    Log.e(TAG, "playNewItem failed");
                                }
                            });
                        }

                        @Override
                        public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                        }
                    });
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                }
            });

        }
    }

    public static void play() {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new Play(avtService) {
                @Override
                public void success(ActionInvocation invocation) {
                    Log.i(TAG, "Play success.");
                }

                @Override
                public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
                    Log.e(TAG, "Play failed");
                }
            });
        }
    }

    public static void pause() {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new Pause(avtService) {
                @Override
                public void success(ActionInvocation invocation) {
                    Log.i(TAG, "Pause success.");
                }

                @Override
                public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
                    Log.e(TAG, "Pause failed");
                }
            });
        }
    }

    public static void stop() {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new Stop(avtService) {
                @Override
                public void success(ActionInvocation invocation) {
                    Log.i(TAG, "Stop success.");
                }

                @Override
                public void failure(ActionInvocation arg0, UpnpResponse arg1, String arg2) {
                    Log.e(TAG, "Stop failed");
                }
            });
        }
    }

    /**
     * Seek
     * seek完成后通过handler重新启动position同步线程
     *
     * @param relativeTimeTarget 要seek到的值,该值为已播放的相对时间如：01:15:03
     * @param handler
     */
    public static void seek(String relativeTimeTarget, final Handler handler) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new Seek(avtService, relativeTimeTarget) {
                @Override
                public void success(ActionInvocation invocation) {
                    Log.i(TAG, "Seek success.");
                    //Delay 1 second to synchronize remote device rel_time and SeekBar progress value.
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            handler.sendEmptyMessage(NowplayingFragment.RESUME_SEEKBAR_ACTION);
                        }
                    }, 1000);
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.e(TAG, "Seek failed");
                }
            });
        }
    }

    public static void getMediaInfo(final Handler handler) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new GetMediaInfo(avtService) {

                @Override
                public void received(ActionInvocation invocation, MediaInfo mediaInfo) {
                    Message msg = Message.obtain(handler, NowplayingFragment.GET_MEDIA_INFO_ACTION);
                    msg.obj = mediaInfo;
                    msg.sendToTarget();
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.e(TAG, "GetMediaInfo failed");
                }
            });
        }
    }

    public static void getPositionInfo(final Handler handler) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new GetPositionInfo(avtService) {
                @Override
                public void received(ActionInvocation invocation, PositionInfo positionInfo) {
                    Message msg = Message.obtain(handler, NowplayingFragment.GET_POSITION_INFO_ACTION);
                    msg.obj = positionInfo;
                    msg.sendToTarget();
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.e(TAG, "GetPositionInfo failed");
                }
            });
        }
    }

    public static void getTransportInfo(final Handler handler) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service avtService = device.findService(SystemManager.AV_TRANSPORT_SERVICE);
        if (avtService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new GetTransportInfo(avtService) {

                @Override
                public void received(ActionInvocation invocation, TransportInfo transportInfo) {
                    TransportState ts = transportInfo.getCurrentTransportState();
                    Log.i(TAG, "TransportState:" + ts.getValue());

                    if (TransportState.PLAYING == ts) {
                        handler.sendEmptyMessage(NowplayingFragment.PLAY_ACTION);
                    } else if (TransportState.PAUSED_PLAYBACK == ts) {
                        handler.sendEmptyMessage(NowplayingFragment.PAUSE_ACTION);
                    } else if (TransportState.STOPPED == ts) {
                        handler.sendEmptyMessage(NowplayingFragment.STOP_ACTION);
                    }
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.e(TAG, "GetTransportInfo failed");
                }
            });
        }
    }

    public static void getVolume(final Handler handler) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service rcService = device.findService(SystemManager.RENDERING_CONTROL_SERVICE);
        if (rcService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new GetVolume(rcService) {

                @Override
                public void received(ActionInvocation actionInvocation, int currentVolume) {
                    //Send currentVolume to handler.
                    Log.i(TAG, "GetVolume:" + currentVolume);
                    Message msg = Message.obtain(handler, NowplayingFragment.GET_VOLUME_ACTION, currentVolume, 0);
                    msg.sendToTarget();
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.e(TAG, "GetVolume failed");
                }
            });
        }
    }

    public static void setVolume(int newVolume) {
        Device device = SystemManager.getInstance().getSelectedDevice();
        //Check selected device
        if (device == null) return;

        Service rcService = device.findService(SystemManager.RENDERING_CONTROL_SERVICE);
        if (rcService != null) {
            ControlPoint cp = SystemManager.getInstance().getControlPoint();
            cp.execute(new SetVolume(rcService, newVolume) {

                @Override
                public void success(ActionInvocation invocation) {
                    Log.i(TAG, "SetVolume success.");
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                    Log.e(TAG, "SetVolume failure.");
                }
            });
        }
    }

}
