package com.android.rtmpvideo;

import android.os.Environment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, VideoGather.CameraOperateCallback,MediaPublisher.ConnectRtmpServerCb,SurfacePreview.PermissionNotify{
    private final static String TAG = "MainActivity";
    private Button btnStart;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private SurfacePreview mSurfacePreview;
    private MediaPublisher mediaPublisher;
    private boolean isStarted;
    private boolean isRtmpConnected = false;
    private static final String rtmpUrl = "rtmp://publish.lvmengya.com/lvshang/ls";
    private boolean hasPermission;
    private static final int TARGET_PERMISSION_REQUEST = 100;

    // 要申请的权限
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置全屏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        isStarted = false;
        hasPermission = false;
        btnStart = (Button) findViewById(R.id.btn_start);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);
        mSurfaceView.setKeepScreenOn(true);
        // 获得SurfaceView的SurfaceHolder
        mSurfaceHolder = mSurfaceView.getHolder();
        // 设置surface不需要自己的维护缓存区
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        // 为srfaceHolder添加一个回调监听器
        mSurfacePreview = new SurfacePreview(this,this);
        mSurfaceHolder.addCallback(mSurfacePreview);
        btnStart.setOnClickListener(this);

        String logPath = Environment
                .getExternalStorageDirectory() + "/watermark.bmp";
        mediaPublisher = MediaPublisher.newInstance(rtmpUrl,logPath);
        mediaPublisher.setRtmpConnectCb(this);
        mediaPublisher.initMediaPublish();

        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 检查该权限是否已经获取
            for (int i = 0; i < permissions.length; i++) {
                int result = ContextCompat.checkSelfPermission(this, permissions[i]);
                // 权限是否已经 授权 GRANTED---授权  DINIED---拒绝
                if (result != PackageManager.PERMISSION_GRANTED) {
                    hasPermission = false;
                    break;
                } else
                    hasPermission = true;
            }
            if(!hasPermission){
                // 如果没有授予权限，就去提示用户请求
                ActivityCompat.requestPermissions(this,
                        permissions, TARGET_PERMISSION_REQUEST);
            }
        }
    }

    private void codecToggle() {
        if (isStarted) {
            isStarted = false;
            if(isRtmpConnected){
                //停止编码 先要停止编码，然后停止采集
                mediaPublisher.stopEncoder();
                //停止音频采集
                mediaPublisher.stopAudioGather();
                //断开RTMP连接
                mediaPublisher.stopRtmpPublish();
            }
        } else {
            isStarted = true;
            //连接Rtmp流媒体服务器
            mediaPublisher.startRtmpPublish();
        }
        btnStart.setText(isStarted ? "停止" : "开始");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isStarted){
            isStarted = false;
            if(isRtmpConnected){
                //停止编码 先要停止编码，然后停止采集
                mediaPublisher.stopEncoder();
                //停止音频采集
                mediaPublisher.stopAudioGather();
                //断开RTMP连接
                mediaPublisher.stopRtmpPublish();
            }
        }
        //释放编码器
        if(mediaPublisher != null)
            mediaPublisher.release();
        mediaPublisher = null;
        VideoGather.getInstance().doStopCamera();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_start:
                codecToggle();
                break;
        }
    }

    @Override
    public void cameraHasOpened() {
        VideoGather.getInstance().doStartPreview(this, mSurfaceHolder);
    }

    @Override
    public void cameraHasPreview(int width,int height,int fps) {
        //初始化视频编码器
        mediaPublisher.initVideoEncoder(width,height,fps);
    }

    @Override
    public void onConnectRtmp(final int ret) {
        isRtmpConnected = ret == 0 ? false : true;
//        if(ret != 0){
//            //采集音频
//            mediaPublisher.startAudioGather();
//            //初始化音频编码器
//            mediaPublisher.initAudioEncoder();
//            //启动编码
//            mediaPublisher.startEncoder();
//        }
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                if(ret == 0){
                    Log.e(TAG, "===zhongjihao=====Rtmp连接失败====");
                    //更新UI
                    Toast.makeText(MainActivity.this,"RTMP流媒体服务器连接失败,请检测网络或服务器是否启动!",Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                && (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)) {
            if(requestCode == TARGET_PERMISSION_REQUEST){
                btnStart.setEnabled(true);
                hasPermission = true;
                // 打开摄像头
                VideoGather.getInstance().doOpenCamera(this);
            }
        }else{
            btnStart.setEnabled(false);
            hasPermission = false;
            Toast.makeText(this, getText(R.string.no_permission_tips), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public boolean hasPermission(){
        return  hasPermission;
    }
}
