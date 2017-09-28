package com.farukcankaya.audiomanager;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.farukcankaya.audiomanager.cons.Type;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Faruk Cankaya on 11/9/16.
 */

public class AudioView extends LinearLayout implements View.OnClickListener {
    public static final String TAG = "AudioView";
    private ImageView recordImageView, recordingImageView, playImageView, stopImageView, cancelImageView;
    private TextView durationTextView;

    private PlayerListener playerListener;
    private AudioManager audioManager;

    private int width, height;

    /**
     * 0 = IDLE
     * 1 = RECORDING
     * 2 = RECORDED
     * 3 = PLAYING
     */
    private int state = 0;
    private long duration = 0;
    private String filePath = null;

    public AudioView(Context context) {
        super(context);
    }

    public AudioView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AudioView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs) {
        setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        setOrientation(LinearLayout.HORIZONTAL);

        LayoutInflater.from(context).inflate(R.layout.layout, this, true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        recordImageView = (ImageView) findViewById(R.id.rw_record);
        recordImageView.setOnClickListener(this);

        recordingImageView = (ImageView) findViewById(R.id.rw_recording);
        recordingImageView.setOnClickListener(this);

        playImageView = (ImageView) findViewById(R.id.rw_play);
        playImageView.setOnClickListener(this);

        stopImageView = (ImageView) findViewById(R.id.rw_stop);
        stopImageView.setOnClickListener(this);

        cancelImageView = (ImageView) findViewById(R.id.rw_cancel);
        cancelImageView.setOnClickListener(this);

        durationTextView = (TextView) findViewById(R.id.rw_duration);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.state = this.state;
        ss.duration = this.duration;
        ss.filePath = this.filePath;
        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.state = ss.state;
        this.duration = ss.duration;
        this.filePath = ss.filePath;
        goToState(this.state);
    }

    public void setPlayerListener(PlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    public void bind(String filePath) {
        this.filePath = filePath;
        if (this.audioManager != null) {
            this.audioManager.destroy();
            this.audioManager = null;
        }

        this.audioManager = new AudioManager(getContext(), this.filePath, new AudioListener() {
            @Override
            public void onReady(Type type, long millis) {
                Log.i(TAG, "onReady " + millis);
                duration = millis;
                if (type == Type.RECORD) {
                    state = 0;
                    goToState(0);
                } else if (type == Type.PLAY) {
                    state = 2;
                    goToState(2);
                    changeDuration(millis);
                }
            }

            @Override
            public void onStarted(Type type) {
                Log.i(TAG, "onStarted");
                if (type == Type.RECORD) {
                    state = 1;
                    goToState(1);
                } else if (type == Type.PLAY) {
                    state = 3;
                    goToState(3);
                }
            }

            @Override
            public void onProgress(Type type, long millis) {
                Log.i(TAG, "onProgress " + millis);
                duration = millis;
                changeDuration(millis);
            }

            @Override
            public void onStopped(Type type, long duration) {
                state = 2;
                goToState(2);
            }

            @Override
            public void onCanceled(Type type) {
                state = 0;
                goToState(0);
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        if (w != oldw) {
            recordImageView.getLayoutParams().width = h;
            recordingImageView.getLayoutParams().width = h;
            playImageView.getLayoutParams().width = h;
            durationTextView.getLayoutParams().width = h;
            stopImageView.getLayoutParams().width = h;
            cancelImageView.getLayoutParams().width = h;
        }
    }

    private void goToState(int state) {
        switch (state) {
            case 1:
                post(playOrRecord);
                break;
            case 2:
                post(stop);
                break;
            case 3:
                post(playOrRecord);
                break;
            default:
                post(idle);
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.rw_record) {
            if (playerListener != null) {
                playerListener.record();
            }

            if (audioManager != null) {
                audioManager.record();
            }

            state = 1;
            goToState(state);
        } else if (view.getId() == R.id.rw_stop) {
            if (playerListener != null) {
                playerListener.stop(this.filePath);
            }
            if (audioManager != null) {
                audioManager.stop();
            }
            state = 2;
            goToState(state);
        } else if (view.getId() == R.id.rw_play) {
            if (playerListener != null) {
                playerListener.play();
            }
            if (audioManager != null) {
                audioManager.play();
            }
            state = 3;
            goToState(state);
        } else if (view.getId() == R.id.rw_cancel) {
            if (audioManager != null) {
                audioManager.cancel();
            }

            if (playerListener != null) {
                playerListener.cancel();
            }
            state = 0;
            goToState(state);
        }
    }

    @Override
    protected void dispatchSaveInstanceState(SparseArray<Parcelable> container) {
        // Makes sure that the state of the child views in the side
        // spinner are not saved since we handle the state in the
        // onSaveInstanceState.
        super.dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        // Makes sure that the state of the child views in the side
        // spinner are not restored since we handle the state in the
        // onSaveInstanceState.
        super.dispatchThawSelfOnly(container);
    }

    /**
     *
     */
    private Runnable idle = new Runnable() {
        @Override
        public void run() {
            recordingImageView.clearAnimation();

            duration = 0;
            durationTextView.setText("00:00");

            recordImageView.setVisibility(VISIBLE);
            recordingImageView.setVisibility(GONE);
            playImageView.setVisibility(GONE);
            durationTextView.setVisibility(GONE);
            stopImageView.setVisibility(GONE);
            cancelImageView.setVisibility(GONE);
            invalidate();
            requestLayout();
        }
    };

    /**
     *
     */
    private Runnable playOrRecord = new Runnable() {
        @Override
        public void run() {
            Animation pulse = AnimationUtils.loadAnimation(getContext(), R.anim.pulse);
            recordingImageView.startAnimation(pulse);

            duration = 0;
            durationTextView.setText("00:00");

            recordImageView.setVisibility(GONE);
            recordingImageView.setVisibility(VISIBLE);
            playImageView.setVisibility(GONE);
            durationTextView.setVisibility(VISIBLE);
            stopImageView.setVisibility(VISIBLE);
            cancelImageView.setVisibility(VISIBLE);
            invalidate();
            requestLayout();

            // TODO animate with postDelayed(this, 16);
        }
    };

    /**
     *
     */
    private Runnable stop = new Runnable() {
        @Override
        public void run() {
            recordingImageView.clearAnimation();

            recordImageView.setVisibility(GONE);
            recordingImageView.clearAnimation();
            recordingImageView.setVisibility(GONE);
            playImageView.setVisibility(VISIBLE);
            durationTextView.setVisibility(VISIBLE);
            stopImageView.setVisibility(GONE);
            cancelImageView.setVisibility(VISIBLE);
            invalidate();
            requestLayout();
        }
    };

    private void changeDuration(final long millis) {
        post(new Runnable() {
            @Override
            public void run() {
                java.text.DateFormat formatter = new SimpleDateFormat("mm:ss");
                Date time = new Date(millis);
                String timeStr = formatter.format(time);

                durationTextView.setText(timeStr);
                invalidate();
                requestLayout();
            }
        });
    }

    public interface PlayerListener {
        void record();

        void stop(String filePath);

        void play();

        void cancel();
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
            this.audioManager.destroy();
            this.audioManager.stopPlaying();
        }
        super.onDetachedFromWindow();
    }
}
