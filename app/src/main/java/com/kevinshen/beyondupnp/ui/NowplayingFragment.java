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
package com.kevinshen.beyondupnp.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.kevinshen.beyondupnp.Intents;
import com.kevinshen.beyondupnp.R;
import com.kevinshen.beyondupnp.core.MediaPlayerController;
import com.kevinshen.beyondupnp.core.PlaybackCommand;
import com.kevinshen.beyondupnp.core.SystemManager;

import org.fourthline.cling.model.ModelUtil;
import org.fourthline.cling.support.model.MediaInfo;
import org.fourthline.cling.support.model.PositionInfo;
import org.fourthline.cling.support.model.TransportState;

import java.util.concurrent.ExecutionException;

public class NowplayingFragment extends Fragment {
    private static final String TAG = NowplayingFragment.class.getSimpleName();

    public static final int PLAY_ACTION = 0xa1;
    public static final int PAUSE_ACTION = 0xa2;
    public static final int STOP_ACTION = 0xa3;
    public static final int GET_MEDIA_INFO_ACTION = 0xa4;
    public static final int GET_POSITION_INFO_ACTION = 0xa5;
    public static final int RESUME_SEEKBAR_ACTION = 0xa6;
    public static final int GET_VOLUME_ACTION = 0xa7;
    public static final int SET_VOLUME_ACTION = 0xa8;

    private MediaPlayerController mMediaPlayerController;
    private ImageButton mPlayPauseButton;
    private TextView mRelTimeText;
    private TextView mTrackDurationText;
    private SeekBar mSeekBar;
    private int mTrackDurationSeconds = 0;
    private TextView mTitleTextView;

    private BroadcastReceiver mTransportStateBroadcastReceiver;
    /**
     * Use this factory method to create a new instance.
     *
     * @return A new instance of fragment NowplayingFragment.
     */
    public static NowplayingFragment newInstance() {
        NowplayingFragment fragment = new NowplayingFragment();
        return fragment;
    }

    public NowplayingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMediaPlayerController = new MediaPlayerController(mHandler);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_nowplaying, container, false);

        mTitleTextView = (TextView) view.findViewById(R.id.track_info_title);
        mRelTimeText = (TextView) view.findViewById(R.id.audio_player_current_time);
        mTrackDurationText = (TextView) view.findViewById(R.id.audio_player_total_time);

        mPlayPauseButton = (ImageButton) view.findViewById(R.id.audio_player_play);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransportState state = mMediaPlayerController.getCurrentState();
                if (state == TransportState.PLAYING){
                    PlaybackCommand.pause();
                }else if (state == TransportState.PAUSED_PLAYBACK || state == TransportState.STOPPED){
                    PlaybackCommand.play();
                    PlaybackCommand.getPositionInfo(mHandler);
                }
            }
        });

        mSeekBar = (SeekBar)view.findViewById(R.id.audio_player_seekbar);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i(TAG,"Start Seek");
                try {
                    mMediaPlayerController.pauseUpdateSeekBar();
                } catch (InterruptedException e) {
                    Log.e(TAG,"Interrupt update seekbar thread.");
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float percent = (float)seekBar.getProgress()/seekBar.getMax();
                int seekToSecond = (int)(mTrackDurationSeconds*percent);
                Log.i(TAG,"Seek to second:" + ModelUtil.toTimeString(seekToSecond));
                //Send command and receive result.
                PlaybackCommand.seek(ModelUtil.toTimeString(seekToSecond),mHandler);
            }
        });

        return view;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case PLAY_ACTION:
                        Log.i(TAG,"Execute PLAY_ACTION");
                        mMediaPlayerController.setCurrentState(TransportState.PLAYING);
                        mPlayPauseButton.setImageResource(R.drawable.beyond_holo_light_pause);
                        //Start syncing
                        mMediaPlayerController.startUpdateSeekBar();
                        break;
                    case PAUSE_ACTION:
                        Log.i(TAG,"Execute PAUSE_ACTION");
                        mMediaPlayerController.setCurrentState(TransportState.PAUSED_PLAYBACK);
                        mPlayPauseButton.setImageResource(R.drawable.beyond_holo_light_play);
                        mMediaPlayerController.pauseUpdateSeekBar();
                        break;
                    case STOP_ACTION:
                        Log.i(TAG,"Execute STOP_ACTION");
                        mMediaPlayerController.setCurrentState(TransportState.STOPPED);
                        mPlayPauseButton.setImageResource(R.drawable.beyond_holo_light_play);
                        mMediaPlayerController.pauseUpdateSeekBar();
                        break;
                    case GET_MEDIA_INFO_ACTION:
                        MediaInfo mediaInfo = (MediaInfo)msg.obj;
                        if (mediaInfo != null){
                            Log.i(TAG,"Execute GET_MEDIA_INFO_ACTION:" + mediaInfo);
                        }
                        break;
                    case GET_POSITION_INFO_ACTION:
                        PositionInfo positionInfo = (PositionInfo)msg.obj;
                        if (positionInfo != null){
                            //Set rel time and duration time.
                            String relTime = positionInfo.getRelTime();
                            String trackDuration = positionInfo.getTrackDuration();
                            mRelTimeText.setText(relTime);
                            mTrackDurationText.setText(trackDuration);

                            int elapsedSeconds = (int)positionInfo.getTrackElapsedSeconds();
                            int durationSeconds = (int)positionInfo.getTrackDurationSeconds();
                            mSeekBar.setProgress(elapsedSeconds);
                            mSeekBar.setMax(durationSeconds);

                            Log.d(TAG,"elapsedSeconds:" + elapsedSeconds);
                            Log.d(TAG,"durationSeconds:" + durationSeconds);

                            //Record the current track's duration seconds
                            mTrackDurationSeconds = durationSeconds;
                        }
                        break;
                    case RESUME_SEEKBAR_ACTION:
                        mMediaPlayerController.startUpdateSeekBar();
                        break;
                    case GET_VOLUME_ACTION:
                        //Get the current volume from arg1.
                        SystemManager.getInstance().setDeviceVolume(msg.arg1);
                        break;
                    case SET_VOLUME_ACTION:
                        SystemManager.getInstance().setDeviceVolume(msg.arg1);
                        PlaybackCommand.setVolume(msg.arg1);
                        break;
                }
            } catch (InterruptedException e) {
                Log.e(TAG,"SetCurrentStatus InterruptedException:" + e.getMessage());
            } catch (ExecutionException e) {
                Log.e(TAG, "SetCurrentStatus ExecutionException:" + e.getMessage());
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //Register play status broadcast
        mTransportStateBroadcastReceiver = new TransportStateBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intents.ACTION_PLAYING);
        filter.addAction(Intents.ACTION_PAUSED_PLAYBACK);
        filter.addAction(Intents.ACTION_STOPPED);
        filter.addAction(Intents.ACTION_CHANGE_DEVICE);
        filter.addAction(Intents.ACTION_SET_VOLUME);
        filter.addAction(Intents.ACTION_UPDATE_LAST_CHANGE);
        getActivity().registerReceiver(mTransportStateBroadcastReceiver,filter);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().unregisterReceiver(mTransportStateBroadcastReceiver);
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);

        mMediaPlayerController.destroy();
        mMediaPlayerController = null;

        super.onDestroy();
    }

    private class TransportStateBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG,"Receive playback intent:" + intent.getAction());
            if (Intents.ACTION_PLAYING.equals(intent.getAction())){
                mHandler.sendEmptyMessage(PLAY_ACTION);
            }else if (Intents.ACTION_PAUSED_PLAYBACK.equals(intent.getAction())){
                mHandler.sendEmptyMessage(PAUSE_ACTION);
            }else if (Intents.ACTION_STOPPED.equals(intent.getAction())){
                mHandler.sendEmptyMessage(STOP_ACTION);
            }else if (Intents.ACTION_CHANGE_DEVICE.equals(intent.getAction())){
                //Update UI to sync with current device.
                PlaybackCommand.getTransportInfo(mHandler);
                PlaybackCommand.getVolume(mHandler);
            }else if (Intents.ACTION_SET_VOLUME.equals(intent.getAction())){
                Message msg = Message.obtain(mHandler,SET_VOLUME_ACTION,intent.getIntExtra("currentVolume",0),0);
                msg.sendToTarget();
            }else if (Intents.ACTION_UPDATE_LAST_CHANGE.equals(intent.getAction())){
                mTitleTextView.setText(intent.getStringExtra("title") + " - " + intent.getStringExtra("creator"));
            }
        }
    }
}
