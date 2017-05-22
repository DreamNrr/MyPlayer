package com.example.wzh.myplayer.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wzh.myplayer.R;
import com.example.wzh.myplayer.domain.MediaItem;
import com.example.wzh.myplayer.utils.Utils;
import com.example.wzh.myplayer.view.VideoView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SystemVideoPlayerActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int HIDE_MEDIACONTROLLER = 1;
    private static final int DEFUALT_SCREEN = 0;
    private static final int FULL_SCREEN = 1;
    private static final int SHOW_NET_SPEED = 2;
    private LinearLayout ll_buffering;
    private TextView tv_net_speed;
    private boolean isNetUri;

    private  float startY;
    private float startX;
    private float touchRang;
    private  int mVol;
    private VideoView vv;
    private Uri uri;
    private static final int PROGRESS = 0;
    private ArrayList<MediaItem> mediaItems;
    private LinearLayout ll_loading;
    private TextView tv_loading_net_speed;

    private LinearLayout llTop;
    private TextView tvName;
    private ImageView ivBattery;
    private TextView tvSystemTime;
    private Button btnVoice;
    private SeekBar seekbarVoice;
    private Button btnSwitchPlayer;
    private LinearLayout llBottom;
    private TextView tvCurrentTime;
    private SeekBar seekbarVideo;
    private TextView tvDuration;
    private Button btnExit;
    private Button btnPre;
    private Button btnStartPause;
    private Button btnNext;
    private Button btnSwitchScreen;
    private Utils utils;
    private MyBroadCastReceiver receiver;
    private int position;
    private GestureDetector detector;
    private boolean isFullScreen = false;
    private int screenHeight;
    private int screenWidth;
    private int videoWidth;
    private int videoHeight;
    private int currentVoice;
    private AudioManager am;
    private int maxVoice;
    private boolean isMute = false;
    private void findViews() {
        setContentView(R.layout.activity_system_video_player);
        llTop = (LinearLayout)findViewById( R.id.ll_top );
        tvName = (TextView)findViewById( R.id.tv_name );
        ivBattery = (ImageView)findViewById( R.id.iv_battery );
        tvSystemTime = (TextView)findViewById( R.id.tv_system_time );
        btnVoice = (Button)findViewById( R.id.btn_voice );
        seekbarVoice = (SeekBar)findViewById( R.id.seekbar_voice );
        btnSwitchPlayer = (Button)findViewById( R.id.btn_switch_player );
        llBottom = (LinearLayout)findViewById( R.id.ll_bottom );
        tvCurrentTime = (TextView)findViewById( R.id.tv_current_time );
        seekbarVideo = (SeekBar)findViewById( R.id.seekbar_video );
        tvDuration = (TextView)findViewById( R.id.tv_duration );
        btnExit = (Button)findViewById( R.id.btn_exit );
        btnPre = (Button)findViewById( R.id.btn_pre );
        btnStartPause = (Button)findViewById( R.id.btn_start_pause );
        btnNext = (Button)findViewById( R.id.btn_next );
        btnSwitchScreen = (Button)findViewById( R.id.btn_switch_screen );
        vv = (VideoView)findViewById(R.id.vv);
        ll_buffering = (LinearLayout) findViewById(R.id.ll_buffering);
        tv_net_speed = (TextView) findViewById(R.id.tv_net_speed);
        ll_loading = (LinearLayout)findViewById(R.id.ll_loading);
        tv_loading_net_speed = (TextView)findViewById(R.id.tv_loading_net_speed);


        btnVoice.setOnClickListener( this );
        btnSwitchPlayer.setOnClickListener( this );
        btnExit.setOnClickListener( this );
        btnPre.setOnClickListener( this );
        btnStartPause.setOnClickListener( this );
        btnNext.setOnClickListener( this );
        btnSwitchScreen.setOnClickListener( this );
        seekbarVoice.setMax(maxVoice);
        seekbarVoice.setProgress(currentVoice);
        handler.sendEmptyMessage(SHOW_NET_SPEED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initData();
        findViews();
        getData();

        //得到播放地址
        uri = getIntent().getData();
        setListener();
        setData();
        //设置播放地址
        vv.setVideoURI(uri);
    }

    private void setData() {
        if(mediaItems != null && mediaItems.size()>0) {
            MediaItem mediaItem = mediaItems.get(position);
            tvName.setText(mediaItem.getName());
            vv.setVideoPath(mediaItem.getData());
            isNetUri = utils.isNetUri(mediaItem.getData());
        }else if(uri != null){
            vv.setVideoURI(uri);
            tvName.setText(uri.toString());
            isNetUri = utils.isNetUri(uri.toString());
        }
        setButtonStatus();
    }

    private void initData() {
        utils = new Utils();
        //注册监听电量变化广播
        receiver = new MyBroadCastReceiver();
        IntentFilter intentFilter  = new IntentFilter();
        //监听电量变化
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(receiver,intentFilter);

        //手势识别器
        detector =  new GestureDetector(this,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public void onLongPress(MotionEvent e) {
                super.onLongPress(e);
                setStartOrPause();
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isShowMediaController) {
                    hideMediaController();
                    handler.removeMessages(HIDE_MEDIACONTROLLER);
                } else {
                    showMediaController();
                    handler.sendEmptyMessageDelayed(HIDE_MEDIACONTROLLER, 4000);
                }
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (isFullScreen) {
                    setVideoType(DEFUALT_SCREEN);
                } else {
                    setVideoType(FULL_SCREEN);
                }
                return super.onDoubleTap(e);
            }

        });
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenHeight = metrics.heightPixels;
        screenWidth = metrics.widthPixels;
        am = (AudioManager) getSystemService(AUDIO_SERVICE);
        currentVoice = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVoice = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN://手指按下屏幕
                //1.记录相关的值
                startY = event.getY();
                startX=event.getX();
                touchRang =Math.min(screenWidth, screenHeight);//screenHeight
                mVol = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                handler.removeMessages(HIDE_MEDIACONTROLLER);
                break;
            case MotionEvent.ACTION_MOVE://手指在屏幕上移动
                //2.来到结束的坐标
                float endY = event.getY();
                //3.计算偏移量
                float distanceY = startY - endY;
                if(startX>screenWidth/2) {
                    //要改变的声音 = (滑动的距离 / 总距离)*最大音量
                    float delta = (distanceY / touchRang) * maxVoice;
                    //最终声音 = 原来的声音 + 要改变的声音
                    float volume = Math.min(Math.max(mVol + delta, 0), maxVoice);
                    if (delta != 0) {
                        updateVoiceProgress((int) volume);
                    }
                } else {
                    //屏幕左半部分上滑，亮度变大，下滑，亮度变小
                    final double FLING_MIN_DISTANCE = 0.5;
                    final double FLING_MIN_VELOCITY = 0.5;
                    if (distanceY > FLING_MIN_DISTANCE && Math.abs(distanceY) > FLING_MIN_VELOCITY) {
                        setBrightness(20);
                    }
                    if (distanceY < FLING_MIN_DISTANCE
                            && Math.abs(distanceY) > FLING_MIN_VELOCITY) {
                        setBrightness(-20);
                    }
                }
                break;

            case MotionEvent.ACTION_UP://手指离开屏幕
                handler.sendEmptyMessageDelayed(HIDE_MEDIACONTROLLER, 5000);
                break;

        }

        return super.onTouchEvent(event);
    }
    public void setBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = lp.screenBrightness + brightness / 255.0f;
        if (lp.screenBrightness > 1) {
            lp.screenBrightness = 1;
        } else if (lp.screenBrightness < 0.2) {
            lp.screenBrightness = (float) 0.2;
        }
        getWindow().setAttributes(lp);
    }

    private  boolean isShowMediaController = false;

    private void  hideMediaController(){
        llBottom.setVisibility(View.INVISIBLE);
        llTop.setVisibility(View.GONE);
        isShowMediaController = false;
    }

    public void showMediaController(){
        llBottom.setVisibility(View.VISIBLE);
        llTop.setVisibility(View.VISIBLE);
        isShowMediaController = true;
    }

    public void getData() {
        uri = getIntent().getData();
        mediaItems  = (ArrayList<MediaItem>) getIntent().getSerializableExtra("videoList");
        position = getIntent().getIntExtra("position",0);
    }


    class MyBroadCastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            Log.e("TAG","level=="+level);
            setBatteryView(level);

        }
    }
    private void setBatteryView(int level) {
        if(level <=0){
            ivBattery.setImageResource(R.drawable.ic_battery_0);
        }else if(level <= 10){
            ivBattery.setImageResource(R.drawable.ic_battery_10);
        }else if(level <=20){
            ivBattery.setImageResource(R.drawable.ic_battery_20);
        }else if(level <=40){
            ivBattery.setImageResource(R.drawable.ic_battery_40);
        }else if(level <=60){
            ivBattery.setImageResource(R.drawable.ic_battery_60);
        }else if(level <=80){
            ivBattery.setImageResource(R.drawable.ic_battery_80);
        }else if(level <=100){
            ivBattery.setImageResource(R.drawable.ic_battery_100);
        }else {
            ivBattery.setImageResource(R.drawable.ic_battery_100);
        }
    }
    private int preCurrentPosition;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case SHOW_NET_SPEED:
                    if(isNetUri){
                        String netSpeed = utils.getNetSpeed(SystemVideoPlayerActivity.this);
                        tv_loading_net_speed.setText("正在加载中...."+netSpeed);
                        tv_net_speed.setText("正在缓冲...."+netSpeed);
                        sendEmptyMessageDelayed(SHOW_NET_SPEED,1000);
                    }
                    break;
                case PROGRESS:
                    int currentPosition = vv.getCurrentPosition();
                    seekbarVideo.setProgress(currentPosition);
                    tvCurrentTime.setText(utils.stringForTime(currentPosition));
                    tvSystemTime.setText(getSystemTime());
                    if(isNetUri) {
                        int bufferPercentage = vv.getBufferPercentage();
                        int totalBuffer = bufferPercentage * seekbarVideo.getMax();
                        int SecondaryProgress = totalBuffer / 100;
                        seekbarVideo.setSecondaryProgress(SecondaryProgress);
                    }else {
                        seekbarVideo.setSecondaryProgress(0);
                    }
                   if(isNetUri && vv.isPlaying()) {
                       int duration = currentPosition - preCurrentPosition;
                       if(duration <500) {
                           ll_buffering.setVisibility(View.VISIBLE);
                       }else {
                           ll_buffering.setVisibility(View.GONE);
                       }
                       preCurrentPosition = currentPosition;
                   }
                    sendEmptyMessageDelayed(PROGRESS,1000);
                    break;
                case HIDE_MEDIACONTROLLER:
                    hideMediaController();
                    break;
            }
        }
    };
    private void setListener() {
        vv.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoWidth = mp.getVideoWidth();
                videoHeight = mp.getVideoHeight();
                //得到视频的总时长
                int duration = vv.getDuration();
                seekbarVideo.setMax(duration);
                //设置文本总时间
                tvDuration.setText(utils.stringForTime(duration));
                //vv.seekTo(100);
                vv.start();//开始播放
                handler.sendEmptyMessage(PROGRESS);
                //隐藏加载效果
                ll_loading.setVisibility(View.GONE);
                hideMediaController();

                setVideoType(DEFUALT_SCREEN);
            }
        });

        vv.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });

        //设置监听播放完成
        vv.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                setNextVideo();
            }
        });
        seekbarVideo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    vv.seekTo(progress);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeMessages(HIDE_MEDIACONTROLLER);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.sendEmptyMessageDelayed(HIDE_MEDIACONTROLLER, 4000);
            }
        });

        seekbarVoice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    updateVoiceProgress(progress);
                }

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    private void updateVoiceProgress(int progress) {
        currentVoice = progress;
        //真正改变声音
        am.setStreamVolume(AudioManager.STREAM_MUSIC,currentVoice,0);
        //改变进度条
        seekbarVoice.setProgress(currentVoice);
        if(currentVoice <=0){
            isMute = true;
        }else {
            isMute = false;
        }

    }


    @Override
    public void onClick(View v) {
        if ( v == btnVoice ) {
            isMute = !isMute;
            updateVoice(isMute);
        } else if ( v == btnSwitchPlayer ) {
            // Handle clicks for btnSwitchPlayer
        } else if ( v == btnExit ) {
            finish();
            // Handle clicks for btnExit
        } else if ( v == btnPre ) {
            setPreVideo();
            // Handle clicks for btnPre
        } else if ( v == btnStartPause ) {
            setStartOrPause();
            // Handle clicks for btnStartPause
        } else if ( v == btnNext ) {
            setNextVideo();
            // Handle clicks for btnNext
        } else if ( v == btnSwitchScreen ) {
            if (isFullScreen) {
                //默认
                setVideoType(DEFUALT_SCREEN);
            } else {
                //全屏
                setVideoType(FULL_SCREEN);
            }
        }
        handler.removeMessages(HIDE_MEDIACONTROLLER);
        handler.sendEmptyMessageDelayed(HIDE_MEDIACONTROLLER, 4000);


    }

    private void updateVoice(boolean isMute) {
        if(isMute){
            //静音
            am.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
            seekbarVoice.setProgress(0);
        }else{
            //非静音
            am.setStreamVolume(AudioManager.STREAM_MUSIC,currentVoice,0);
            seekbarVoice.setProgress(currentVoice);
        }
    }

    private void setVideoType(int videoType) {
        switch (videoType) {
            case FULL_SCREEN:
                isFullScreen = true;
                btnSwitchScreen.setBackgroundResource(R.drawable.btn_switch_screen_default_selector);
                vv.setVideoSize(screenWidth, screenHeight);

                break;
            case DEFUALT_SCREEN:
                isFullScreen = false;
                btnSwitchScreen.setBackgroundResource(R.drawable.btn_switch_screen_full_selector);
                int mVideoWidth = videoWidth;
                int mVideoHeight = videoHeight;

                int width = screenWidth;
                int height = screenHeight;

                if (mVideoWidth * height < width * mVideoHeight) {
                    width = height * mVideoWidth / mVideoHeight;
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    height = width * mVideoHeight / mVideoWidth;
                }
                vv.setVideoSize(width, height);

                break;
        }
    }
    private void setStartOrPause() {
        if(vv.isPlaying()){
            vv.pause();
            btnStartPause.setBackgroundResource(R.drawable.btn_start_selector);
        }else {
            vv.start();
            btnStartPause.setBackgroundResource(R.drawable.btn_pause_selector);
        }
    }




    private void setButtonStatus() {
        if(mediaItems != null && mediaItems.size() >0){
            //有视频播放
            setEnable(true);

            if(position ==0){
                btnPre.setBackgroundResource(R.drawable.btn_pre_gray);
                btnPre.setEnabled(false);
            }

            if(position ==mediaItems.size()-1){
                btnNext.setBackgroundResource(R.drawable.btn_next_gray);
                btnNext.setEnabled(false);
            }

        }else if(uri != null){
            //上一个和下一个不可用点击
            setEnable(false);
        }
    }

    private void setEnable(boolean b) {
        if( b){
            //上一个和下一个都可以点击
            btnPre.setBackgroundResource(R.drawable.btn_pre_selector);
            btnNext.setBackgroundResource(R.drawable.btn_next_selector);
        }else {
            //上一个和下一个灰色，并且不可用点击
            btnPre.setBackgroundResource(R.drawable.btn_pre_gray);
            btnNext.setBackgroundResource(R.drawable.btn_next_gray);
        }
        btnPre.setEnabled(b);
        btnNext.setEnabled(b);
    }

    private void setPreVideo() {
        position--;
        if(position >= 0) {
            MediaItem mediaItem = mediaItems.get(position);
            vv.setVideoPath(mediaItem.getData());
            tvName.setText(mediaItem.getName());
            isNetUri = utils.isNetUri(mediaItem.getData());
            ll_loading.setVisibility(View.INVISIBLE);
            //设置按钮状态
            setButtonStatus();
        }
    }
    private void setNextVideo() {
        position++;
        if(position < mediaItems.size()){
            //还是在列表范围内容
            MediaItem mediaItem = mediaItems.get(position);
            vv.setVideoPath(mediaItem.getData());
            tvName.setText(mediaItem.getName());
            isNetUri = utils.isNetUri(mediaItem.getData());
            ll_loading.setVisibility(View.INVISIBLE);
            //设置按钮状态
            setButtonStatus();
        }else{
            Toast.makeText(this,"退出播放器",Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String getSystemTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return format.format(new Date());
    }
    @Override
    protected void onDestroy() {

        if(handler != null){
            //把所有消息移除
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        //取消注册
        if(receiver != null){
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            currentVoice--;
            updateVoiceProgress(currentVoice);
            handler.removeCallbacksAndMessages(null);
            handler.sendEmptyMessageDelayed(HIDE_MEDIACONTROLLER,4000);
            return true;
        }else if(keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            currentVoice++;
            updateVoiceProgress(currentVoice);
            handler.removeCallbacksAndMessages(null);
            handler.sendEmptyMessageDelayed(HIDE_MEDIACONTROLLER,4000);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
