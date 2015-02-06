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
package com.kevinshen.beyondupnp;

import android.app.Application;

import com.kevinshen.beyondupnp.core.server.JettyResourceServer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BeyondApplication extends Application {
    private static BeyondApplication sBeyondApplication = null;
    private ExecutorService mThreadPool = Executors.newCachedThreadPool();
    private JettyResourceServer mJettyResourceServer;

    @Override
    public void onCreate() {
        super.onCreate();

        sBeyondApplication = this;

        mJettyResourceServer = new JettyResourceServer();
        mThreadPool.execute(mJettyResourceServer);
    }

    synchronized public static BeyondApplication getApplication() {
        return sBeyondApplication;
    }

    synchronized public void stopServer() {
        mJettyResourceServer.stopIfRunning();
    }
}
