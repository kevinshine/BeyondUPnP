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
import android.util.Log;

import org.fourthline.cling.support.model.TransportState;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaPlayerController {
    private static final String TAG = MediaPlayerController.class.getSimpleName();

    private TransportState mCurrentState = TransportState.STOPPED;
    private ExecutorService mMediaExecutorService = Executors.newSingleThreadExecutor();
    private Object mPlaybackLock = new Object();
    private volatile boolean isPaused = true;

    public MediaPlayerController(final Handler handler){
        //Create SeekBar sync thread,mSeekFuture will be set null when press stop.
        mMediaExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Create SeekBar sync thread.");
                while (true) {
                    synchronized (mPlaybackLock) {
                        try {
                            if (isPaused) mPlaybackLock.wait();

                            PlaybackCommand.getPositionInfo(handler);
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            Log.e(TAG, "MediaPlayer shutdown");
                        }
                    }
                }
            }
        });
    }

    public TransportState getCurrentState() {
        return mCurrentState;
    }

    public void setCurrentState(TransportState currentState) {
        if (this.mCurrentState != currentState) {
            this.mCurrentState = currentState;
        }
    }

    public void startUpdateSeekBar() throws InterruptedException, ExecutionException {
        Log.i(TAG, "Execute startUpdateSeekBar");
        if (this.mCurrentState == TransportState.PLAYING && isPaused) {
            synchronized (mPlaybackLock) {
                isPaused = false;
                Log.i(TAG, "Resume seekbar sync thread.");
                mPlaybackLock.notifyAll();
            }
        }
    }

    public void pauseUpdateSeekBar() throws InterruptedException {
        Log.i(TAG, "Execute pauseUpdateSeekBar");
        isPaused = true;
    }

    public boolean seekbarIsPaused(){
        return isPaused;
    }

    public void destroy() {
        mMediaExecutorService.shutdownNow();
    }
}
