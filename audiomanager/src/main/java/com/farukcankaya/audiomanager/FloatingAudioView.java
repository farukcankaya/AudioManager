package com.farukcankaya.audiomanager;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.farukcankaya.audiomanager.cons.Type;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Faruk Cankaya on 30/03/17.
 */

public class FloatingAudioView extends LinearLayout implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    public static final String TAG = "AudioView";
    private FloatingActionButton floatingActionButtonRecord;
    private CardView cardViewRecord;
    private TextView textViewRecordText;

    private PlayerListener playerListener;
    private ProcessListener processListener;
    private AudioManager audioManager;


    final int defaultFabColor;
    final String defaultStartRecordText;

    private int mFabColor;
    private Drawable mRecordDrawable;
    private Drawable mStopDrawable;
    private String mStartRecordText;
    private int mTextColor;
    private float mTextDimen;

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

    public FloatingAudioView(Context context) {
        this(context, null);
    }

    public FloatingAudioView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.favFloatingAudioViewStyle);
    }

    public FloatingAudioView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.fc_audio_manager_floating_player_layout,
                this, true);
        floatingActionButtonRecord = (FloatingActionButton) findViewById(com.farukcankaya.audiomanager.R.id.fc_audio_manager_new_player_record_fab);
        cardViewRecord = (CardView) findViewById(com.farukcankaya.audiomanager.R.id.fc_audio_manager_new_player_record_cv);
        textViewRecordText = (TextView) findViewById(R.id.fc_audio_manager_new_player_record_text);

        final Resources res = getResources();
        defaultFabColor = ContextCompat.getColor(context, R.color.default_fab_color);
        defaultStartRecordText = res.getString(R.string.default_record_text);

        //Retrieve styles attributes
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FloatingAudioView, defStyleAttr, 0);
        mFabColor = a.getColor(R.styleable.FloatingAudioView_fab_color, defaultFabColor);
        mRecordDrawable = a.getDrawable(R.styleable.FloatingAudioView_fab_record_icon);
        mStopDrawable = a.getDrawable(R.styleable.FloatingAudioView_fab_stop_icon);
        mStartRecordText = a.getString(R.styleable.FloatingAudioView_fab_record_text);
        mTextColor = a.getColor(R.styleable.FloatingAudioView_fab_record_text_color, Color.DKGRAY);
        mTextDimen = a.getDimension(R.styleable.FloatingAudioView_fab_record_text_size, 0f);

        FontUtil.setCustomFont(textViewRecordText, context, attrs);
        a.recycle();

        if (mStartRecordText == null) {
            mStartRecordText = defaultStartRecordText;
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Record Button
        floatingActionButtonRecord.setBackgroundTintList(ColorStateList.valueOf(mFabColor));
        if (mRecordDrawable != null) {
            floatingActionButtonRecord.setImageDrawable(mRecordDrawable);
        }
        floatingActionButtonRecord.setOnClickListener(this);
        cardViewRecord.setOnClickListener(this);

        // Recording Text
        textViewRecordText.setText(mStartRecordText);
        textViewRecordText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextDimen);
        textViewRecordText.setTextColor(mTextColor);
    }

    public TextView getRecordTextView() {
        return textViewRecordText;
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
                if (FloatingAudioView.this.processListener != null) {
                    FloatingAudioView.this.processListener.onSuccess(type, millis);
                    FloatingAudioView.this.processListener = null;
                }
                Log.i(TAG, "onReady " + millis);
                if (type == Type.RECORD) {
                    goToState(State.IDLE_RECORD);
                } else if (type == Type.PLAY) {
                    duration = millis;
                    progress = 0;
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
                    playerListener.stop(FloatingAudioView.this.filePath, duration);
                }
                progress = 0;
                FloatingAudioView.this.duration = duration;
                goToState(State.IDLE_PLAY);
                post(new Runnable() {
                    @Override
                    public void run() {
                        textViewRecordText.setText(getDurationText(duration));
                    }
                });
            }

            @Override
            public void onCanceled(Type type) {
                goToState(State.IDLE_RECORD);
            }
        });
    }

    private void goToState(int state) {
        this.state = state;
        switch (state) {
            case State.IDLE_PLAY:
                post(new Runnable() {
                    @Override
                    public void run() {
                        changeRecordIcon(mRecordDrawable);
                        textViewRecordText.setText(mStartRecordText);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
            case State.RECORDING:
                post(new Runnable() {
                    @Override
                    public void run() {
                        changeRecordIcon(mStopDrawable);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
            case State.PLAYING:
                post(new Runnable() {
                    @Override
                    public void run() {
                        invalidate();
                        requestLayout();
                    }
                });
                break;
            default:
                post(new Runnable() {
                    @Override
                    public void run() {
                        changeRecordIcon(mRecordDrawable);
                        textViewRecordText.setText(mStartRecordText);
                        invalidate();
                        requestLayout();
                    }
                });
                break;
        }
    }

    private void changeRecordIcon(Drawable drawable) {
        if (drawable != null) {
            floatingActionButtonRecord.setImageDrawable(drawable);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.fc_audio_manager_new_player_record_fab ||
                view.getId() == R.id.fc_audio_manager_new_player_record_cv) {
            if (playerListener != null) {
                playerListener.record();
            }

            if (audioManager != null) {
                audioManager.record();
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
                textViewRecordText.setText(getDurationText(millis));
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
