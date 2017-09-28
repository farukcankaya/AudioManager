package com.farukcankaya.audiomanager;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.farukcankaya.audiomanager.cons.Type;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Faruk Cankaya on 11/9/16.
 */

public class NewAudioView extends LinearLayout implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    public static final String TAG = "AudioView";
    private ImageView imageViewRecord, imageViewPlay, imageViewStop, imageViewDelete;
    private TextView textViewDuration, textViewRecordText;
    private ProgressBar progressBarRecording;
    private SeekBar seekBarPlaying;

    private PlayerListener playerListener;
    private ProcessListener processListener;
    private AudioManager audioManager;

    interface State {
        int IDLE_RECORD = 0;
        int IDLE_PLAY = 1;
        int RECORDING = 2;
        int PLAYING = 3;
    }

    private int state = State.IDLE_RECORD;
    private long progress = 0;
    private long duration = 0;
    private String filePath = null;

    public NewAudioView(Context context) {
        super(context);
    }

    public NewAudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public NewAudioView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs) {
        setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.fc_audio_manager_new_player_layout, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Play Button
        imageViewPlay = (ImageView) findViewById(R.id.fc_audio_manager_new_player_play);
        imageViewPlay.setOnClickListener(this);

        // Stop Button
        imageViewStop = (ImageView) findViewById(R.id.fc_audio_manager_new_player_stop);
        imageViewStop.setOnClickListener(this);

        // Record Button
        imageViewRecord = (ImageView) findViewById(R.id.fc_audio_manager_new_player_record);
        imageViewRecord.setOnClickListener(this);

        // Delete Button
        imageViewDelete = (ImageView) findViewById(R.id.fc_audio_manager_new_player_delete);
        imageViewDelete.setOnClickListener(this);

        // Recording Text
        textViewRecordText = (TextView) findViewById(R.id.fc_audio_manager_new_player_record_text);

        // Duration Text
        textViewDuration = (TextView) findViewById(R.id.fc_audio_manager_new_player_duration);

        // Recording Progress
        progressBarRecording = (ProgressBar) findViewById(R.id.fc_audio_manager_new_player_recording_progress);

        // Playing Progress
        seekBarPlaying = (SeekBar) findViewById(R.id.fc_audio_manager_new_player_playing_progress);
        seekBarPlaying.setOnSeekBarChangeListener(this);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.state = this.state;
        ss.duration = this.progress;
        ss.filePath = this.filePath;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.state = ss.state;
        this.progress = ss.duration;
        this.filePath = ss.filePath;
        goToState(this.state);
    }

    public void setPlayerListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    public void bind(long duration) {
        this.duration = duration;
        goToState(State.IDLE_PLAY);
    }


    public void bind(String filePath) {
        bind(filePath, null);
    }

    public void bind(String filePath, ProcessListener processListener) {
        this.processListener = processListener;
        this.filePath = filePath;
        if (this.audioManager != null) {
            this.audioManager.destroy();
            this.audioManager = null;
        }

        this.audioManager = new AudioManager(getContext(), this.filePath, new AudioListener() {
            @Override
            public void onReady(Type type, long millis) {
                if (NewAudioView.this.processListener != null) {
                    NewAudioView.this.processListener.onSuccess(type, millis);
                    NewAudioView.this.processListener = null;
                }
                Log.i(TAG, "onReady " + millis);
                if (type == Type.RECORD) {
                    goToState(State.IDLE_RECORD);
                } else if (type == Type.PLAY) {
                    duration = millis;
                    progress = 0;
                    seekBarPlaying.incrementProgressBy(1);
                    seekBarPlaying.setMax((int) Math.ceil(duration / 1000));
                    seekBarPlaying.setProgress(0);

                    goToState(State.IDLE_PLAY);
                    changeDuration(millis);
                }
            }

            @Override
            public void onStarted(Type type) {
                Log.i(TAG, "onStarted");
                if (type == Type.RECORD) {
                    goToState(State.RECORDING);
                } else if (type == Type.PLAY) {
                    goToState(State.PLAYING);
                }
            }

            @Override
            public void onProgress(Type type, long millis) {
                Log.i(TAG, "onProgress " + millis);
                changeDuration(millis);
            }

            @Override
            public void onStopped(Type type, final long duration) {
                if (playerListener != null) {
                    playerListener.stop(NewAudioView.this.filePath, duration);
                }
                progress = 0;
                NewAudioView.this.duration = duration;
                goToState(State.IDLE_PLAY);
                post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDuration.setText(getDurationText(duration));
                        seekBarPlaying.setProgress(0);
                        seekBarPlaying.setMax((int) Math.ceil(duration / 1000));
                    }
                });
            }

            @Override
            public void onCanceled(Type type) {
                goToState(State.IDLE_RECORD);
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw) {
            imageViewPlay.getLayoutParams().width = h;
            imageViewStop.getLayoutParams().width = h;
            imageViewRecord.getLayoutParams().width = h;
            imageViewPlay.getLayoutParams().width = h;
            imageViewDelete.getLayoutParams().width = h;
        }
    }

    private void goToState(int state) {
        this.state = state;
        switch (state) {
            case State.IDLE_PLAY:
                post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDuration.setText(getDurationText(duration));
                        imageViewPlay.setVisibility(VISIBLE);
                        imageViewStop.setVisibility(GONE);
                        imageViewRecord.setVisibility(GONE);
                        textViewRecordText.setVisibility(GONE);
                        textViewDuration.setVisibility(VISIBLE);
                        progressBarRecording.setVisibility(GONE);
                        seekBarPlaying.setVisibility(VISIBLE);
                        seekBarPlaying.setEnabled(false);
                        imageViewDelete.setVisibility(VISIBLE);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
            case State.RECORDING:
                post(new Runnable() {
                    @Override
                    public void run() {
                        imageViewPlay.setVisibility(GONE);
                        imageViewStop.setVisibility(VISIBLE);
                        imageViewRecord.setVisibility(GONE);
                        textViewRecordText.setVisibility(GONE);
                        textViewDuration.setVisibility(VISIBLE);
                        progressBarRecording.setVisibility(VISIBLE);
                        seekBarPlaying.setVisibility(GONE);
                        imageViewDelete.setVisibility(VISIBLE);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
            case State.PLAYING:
                post(new Runnable() {
                    @Override
                    public void run() {
                        imageViewPlay.setVisibility(GONE);
                        imageViewStop.setVisibility(VISIBLE);
                        imageViewRecord.setVisibility(GONE);
                        textViewRecordText.setVisibility(GONE);
                        textViewDuration.setVisibility(VISIBLE);
                        progressBarRecording.setVisibility(GONE);
                        seekBarPlaying.setVisibility(VISIBLE);
                        seekBarPlaying.setEnabled(true);
                        imageViewDelete.setVisibility(VISIBLE);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
            default:
                post(new Runnable() {
                    @Override
                    public void run() {
                        textViewDuration.setText(getDurationText(0));
                        imageViewPlay.setVisibility(GONE);
                        imageViewStop.setVisibility(GONE);
                        imageViewRecord.setVisibility(VISIBLE);
                        textViewRecordText.setVisibility(VISIBLE);
                        textViewDuration.setVisibility(GONE);
                        progressBarRecording.setVisibility(GONE);
                        seekBarPlaying.setVisibility(GONE);
                        imageViewDelete.setVisibility(GONE);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fc_audio_manager_new_player_record) {
            if (playerListener != null) {
                playerListener.record();
            }

            if (audioManager != null) {
                audioManager.record();
            }
        } else if (view.getId() == R.id.fc_audio_manager_new_player_stop) {
            if (audioManager != null) {
                audioManager.stop();
            }
        } else if (view.getId() == R.id.fc_audio_manager_new_player_play) {
            if (playerListener != null) {
                playerListener.play();
            }
            if (audioManager != null) {
                audioManager.play();
            }
        } else if (view.getId() == R.id.fc_audio_manager_new_player_delete) {
            if (playerListener != null) {
                playerListener.cancel(this.filePath);
            }
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        progress = i * 1000;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (audioManager != null) {
            audioManager.seekTo(progress);
        }
    }

    private void changeDuration(final long millis) {
        this.progress = millis;
        post(new Runnable() {
            @Override
            public void run() {
                textViewDuration.setText(getDurationText(millis));
                seekBarPlaying.setProgress((int) Math.ceil(millis / 1000));
                invalidate();
                requestLayout();
            }
        });
    }

    /**
     * Save instance state
     */
    static class SavedState extends BaseSavedState {
        int state;
        long duration;
        String filePath;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            state = in.readInt();
            duration = in.readLong();
            filePath = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(state);
            out.writeLong(duration);
            out.writeString(filePath);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        if (this.audioManager != null) {
            this.audioManager.stopPlaying();
            this.audioManager.destroy();
        }
        super.onDetachedFromWindow();
    }

    public AudioManager getAudioManager() {
        return audioManager;
    }

    private String getDurationText(long millis) {
        java.text.DateFormat formatter = new SimpleDateFormat("mm:ss");
        Date time = new Date(millis);
        String timeStr = formatter.format(time);
        return timeStr;
    }

    public interface PlayerListener {
        void record();

        void stop(String filePath, long duration);

        void play();

        void cancel(String filePath);
    }

    public interface ProcessListener {
        void onSuccess(Type type, long duration);

        void onError();
    }
}
