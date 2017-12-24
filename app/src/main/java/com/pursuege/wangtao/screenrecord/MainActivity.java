package com.pursuege.wangtao.screenrecord;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private EditText etIpAddress, etPort, etTime;
    private MediaProjectionManager projectionManager;
    private int SCREEN_SHOT = 1;
    private MediaProjection mediaProject;
    private ImageReader imageReader;
    private int imgWidth = 1080;
    private int imgHeight = 1920;
    private int mDensity = 2;
    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.init(getApplication());
        //设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        etIpAddress = (EditText) findViewById(R.id.et_ip_main);
        etPort = (EditText) findViewById(R.id.et_port_main);
        etTime = (EditText) findViewById(R.id.et_port_per_second);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mDensity = metrics.densityDpi;
        imgWidth = ScreenUtils.getScreenWidth() / 2;
        imgHeight = ScreenUtils.getScreenHeight() / 2;
    }

    private String SERVER_ADDRESS;
    private int SERVER_PORT;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onclickStop(View view) {
        if (mediaProject == null) {
            return;
        }
        mediaProject.stop();
        Toast.makeText(this, "停止截屏", Toast.LENGTH_SHORT).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onclickStart(View view) {
        SERVER_ADDRESS = etIpAddress.getText().toString();
        SERVER_PORT = Integer.parseInt(etPort.getText().toString());
        if (TextUtils.isEmpty(SERVER_ADDRESS)) {
            Toast.makeText(this, "服务器ip错误!", Toast.LENGTH_SHORT).show();
            return;
        }
//        setTestData();
//        new Thread() {
//            @Override
//            public void run() {
//                if (socket == null) {
//                    try {
//                        socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }.start();
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(projectionManager.createScreenCaptureIntent(), SCREEN_SHOT);
    }

    private long lastTime=0;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SCREEN_SHOT) {
            mediaProject = projectionManager.getMediaProjection(resultCode, data);
            imageReader = ImageReader.newInstance(imgWidth, imgHeight, PixelFormat.RGBA_8888, 10);
            if (imageReader != null) {
                LogUtils.i("imageReader===success");
            }


            mediaProject.createVirtualDisplay("screencap", imgWidth, imgHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, imageReader.getSurface(), null, mHandler);

            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {

                    System.out.println("===每一帧时间间隔:"+(System.currentTimeMillis()-lastTime));
                    lastTime=System.currentTimeMillis();

                    Image image = imageReader.acquireLatestImage();
                    if (image == null) {
                        return;
                    }
                    int width = image.getWidth();

                    int height = image.getHeight();

                    final Image.Plane[] planes = image.getPlanes();

                    final ByteBuffer buffer = planes[0].getBuffer();

                    int pixelStride = planes[0].getPixelStride();

                    int rowStride = planes[0].getRowStride();

                    int rowPadding = rowStride - pixelStride * width;

                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);

                    bitmap.copyPixelsFromBuffer(buffer);

                    Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, width, height);
//                        saveBitmap(bitmap);
                    setSocketImage(bmp);

                    recyBitmap(bitmap);

                    image.close();

                }
            }, mHandler);
        }
    }

    Socket socket;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setSocketImage(final Bitmap image) {
        new Thread() {
            @Override
            public void run() {
                try {// 创建一个Socket对象，并指定服务端的IP及端口号
                    socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    final ByteArrayOutputStream imgByte = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 80, imgByte);
                    imgByte.flush();
                    byte[] data = imgByte.toByteArray();
                    dos.write(getFileTag(data).getBytes());
                    dos.write(data);
                    dos.flush();
                    dos.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }


    private void recyBitmap(Bitmap bmp) {
        if (bmp != null) {
            if (!bmp.isRecycled()) {
                bmp.recycle();
            }
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

        }
    };

    private void setTestData() {


        new Thread() {
            @Override
            public void run() {
                int index = 0;
                while (true) {
                    try {
                        index++;
                        InputStream in = getResources().getAssets().open(index % 2 == 0 ? "test.jpg" : "test2.jpg");
                        final Bitmap bmp = BitmapFactory.decodeStream(in);
                        final ByteArrayOutputStream out = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, out);
                        byte[] data = out.toByteArray();

                        Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                        OutputStream serverOut = socket.getOutputStream();
                        serverOut.write(getFileTag(data).getBytes());
                        serverOut.write(data, 0, data.length);
                        serverOut.flush();
                        serverOut.close();
                        socket.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        int time = Integer.parseInt(etTime.getText().toString());
                        sleep(time);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

    }

    public static String getFileTag(byte[] data) {
        String len = data.length + "";
        StringBuilder sb = new StringBuilder("99");
        for (int i = 0; i < 8 - len.length(); i++) {
            sb.append("0");
        }
        sb.append(len);
        System.out.println(sb.toString());
        return sb.toString();
    }

}
