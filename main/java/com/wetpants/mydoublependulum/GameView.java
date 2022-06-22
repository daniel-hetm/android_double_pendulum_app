package com.wetpants.mydoublependulum;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Context context;
    private SurfaceHolder holder;
    private Thread drawThread;
    private boolean surfaceReady;
    private boolean drawingActive;
    private boolean isTouched;

    private static final int MAX_FRAME_RATE = 60;
    private static final int MIN_FRAME_TIME = (int) (1000.0 / MAX_FRAME_RATE);
    private static final String LOG_TAG = "surface";

    private Pendulum pendulum;

    private float x = 0, y = 0, x1 = 0, x2 = 0, y1 = 0, y2 = 0, dx = 0, dy = 0;
    private long touchStart = 0, touchEnd = 0, dt = 1000000;
    private int touchedNode = 0;

    public GameView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    private void init() {
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);

        Display display = context.getDisplay();
        Point size = new Point();
        display.getRealSize(size);
        int width = size.x;
        int height = size.y;
        int[] colors = {Color.CYAN, Color.MAGENTA};
        pendulum = new Pendulum(width, height, colors);
    }

    private void tick() {
        if (isTouched) {
            pendulum.moveNode(touchedNode, x, y);
        } else {
            pendulum.update();
        }
    }

    private void render(Canvas canvas) {
        // canvas.drawARGB(50, 0, 0, 0);
        canvas.drawRGB(0, 0, 0);
        pendulum.draw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        canvas.drawLine(x1, y1, x2, y2, paint);
        canvas.save();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (width == 0 || height == 0)
            return;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        this.holder = holder;
        if (drawThread != null) {
            drawingActive = false;
            try {
                drawThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        surfaceReady = true;
        startDrawThread();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopDrawThread();
        holder.getSurface().release();
        this.holder = null;
        surfaceReady = false;
    }

    public void startDrawThread() {
        if (surfaceReady && drawThread == null) {
            drawThread = new Thread(this, "draw_thread");
            drawingActive = true;
            drawThread.start();
        }
    }

    public void stopDrawThread() {
        if (drawThread == null) {
            return;
        }
        drawingActive = false;
        while (true) {
            try {
                drawThread.join(5000);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        drawThread = null;
    }

    @Override
    public void run() {
        long frameStartTime;
        long frameTime;
        if (android.os.Build.BRAND.equalsIgnoreCase("google")
                && android.os.Build.MANUFACTURER.equalsIgnoreCase("asus")
                && android.os.Build.MODEL.equalsIgnoreCase("Nexus 7")) {
            Log.w(LOG_TAG, "Sleep 500ms (Device: Asus Nexus 7)");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
        while (drawingActive) {
            if (holder == null)
                return;
            frameStartTime = System.nanoTime();
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    synchronized (holder) {
                        tick();
                        render(canvas);
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            frameTime = (System.nanoTime() - frameStartTime) / 1000000;
            if (frameTime < MIN_FRAME_TIME) {

                try {
                    Thread.sleep(MIN_FRAME_TIME - frameTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                x2 = event.getX();
                y2 = event.getY();
                x = event.getX();
                y = event.getY();
                touchStart = System.currentTimeMillis();
                isTouched = true;
                touchedNode = pendulum.closestNode((int) x1, (int) y1);
                pendulum.lightUp(touchedNode);
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();
                dx = x2 - x1;
                dy = y2 - y1;
                touchEnd = System.currentTimeMillis();
                dt = (touchStart - touchEnd);
                isTouched = false;
                pendulum.lightNormal(touchedNode);
                break;

            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();
        }

        return true;
    }

    public void pause() {
        stopDrawThread();
    }

    public void resume() {
        startDrawThread();
    }
}
