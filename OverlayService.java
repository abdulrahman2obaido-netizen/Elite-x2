package com.elitex.performance;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.concurrent.atomic.AtomicBoolean;

/** Floating foreground overlay that does not steal focus from the game. */
public class OverlayService extends Service {
    public static volatile boolean active=false;
    public static final String STOP="com.elitex.performance.STOP_OVERLAY";
    private WindowManager wm;private WindowManager.LayoutParams params;private LinearLayout panel;
    private TextView cpu,gpu,ram;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AtomicBoolean reading=new AtomicBoolean(false);
    private boolean closing=false;
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        if(!active){
            createNotification();try{createOverlay();}catch(Exception e){stopSelf();return START_NOT_STICKY;}
            active=true;handler.post(refresh);
        }
        return START_NOT_STICKY;
    }
    private void createNotification(){
        NotificationManager mgr=getSystemService(NotificationManager.class);
        mgr.createNotificationChannel(new NotificationChannel("elite_overlay","ELITE X Overlay",NotificationManager.IMPORTANCE_LOW));
        Intent back=new Intent(this,MainActivity.class);back.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(this,0,back,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Intent stop=new Intent(this,OverlayService.class).setAction(STOP);
        PendingIntent si=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"elite_overlay")
                .setSmallIcon(R.drawable.ic_elite).setContentTitle("ELITE X · Metrics overlay")
                .setContentText("CPU / GPU metrics — click to return")
                .setOngoing(true).setContentIntent(pi)
                .addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,"Stop overlay",si).build()).build();
        startForeground(4001,n);
    }
    private TextView info(String content,int color){
        TextView t=Ui.text(this,content,12,color,true);t.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,4));return t;
    }
    private void createOverlay(){
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        params=new WindowManager.LayoutParams(Ui.dp(this,205),WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.x=Ui.dp(this,14);params.y=Ui.dp(this,130);
        panel=Ui.col(this);panel.setPadding(Ui.dp(this,14),Ui.dp(this,10),Ui.dp(this,14),Ui.dp(this,12));
        panel.setBackground(Ui.outline(this,0xF20F1423,Ui.PURPLE,18));
        LinearLayout bar=Ui.row(this);
        TextView title=Ui.text(this,"◈   ELITE X  •  LIVE",11,Ui.CYAN,true);
        bar.addView(title,new LinearLayout.LayoutParams(0,Ui.dp(this,34),1));
        TextView close=Ui.text(this,"✕",18,Ui.MUTED,true);close.setGravity(Gravity.CENTER);bar.addView(close,Ui.lp(this,32,32,0));
        close.setOnClickListener(v->stopSelf());
        panel.addView(bar);
        View line=new View(this);line.setBackgroundColor(Ui.LINE);panel.addView(line,Ui.lp(this,-1,1,3));
        cpu=info("CPU    —",Ui.CYAN);gpu=info("GPU    —",Ui.PURPLE);ram=info("RAM    —",Ui.MUTED);
        panel.addView(cpu);panel.addView(gpu);panel.addView(ram);
        title.setOnTouchListener(new View.OnTouchListener(){
            int initX,initY;float touchX,touchY;
            @Override public boolean onTouch(View v,MotionEvent e){
                if(e.getAction()==MotionEvent.ACTION_DOWN){initX=params.x;initY=params.y;touchX=e.getRawX();touchY=e.getRawY();return true;}
                if(e.getAction()==MotionEvent.ACTION_MOVE){params.x=initX+(int)(e.getRawX()-touchX);params.y=initY+(int)(e.getRawY()-touchY);
                    try{wm.updateViewLayout(panel,params);}catch(Exception ignored){}return true;}
                return e.getAction()==MotionEvent.ACTION_UP;
            }
        });
        wm.addView(panel,params);
    }
    private final Runnable refresh=new Runnable(){
        @Override public void run(){
            if(closing)return;
            ram.setText("RAM    "+DeviceStats.memory(OverlayService.this));
            if(reading.compareAndSet(false,true)){
                ShizukuBridge.get(OverlayService.this).metrics(value->{
                    reading.set(false);
                    if(closing)return;
                    try{String[] parts=value.split("\\|");
                        float c=Float.parseFloat(parts[0]);float g=Float.parseFloat(parts[1]);
                        cpu.setText("CPU    "+(c<0?"N/A":String.format(java.util.Locale.US,"%.0f%%",c)));
                        gpu.setText("GPU    "+(g<0?"N/A":String.format(java.util.Locale.US,"%.0f%%",g)));
                    }catch(Exception ignored){}
                });
            }
            handler.postDelayed(this,2000);
        }
    };
    @Override public void onDestroy(){
        closing=true;active=false;handler.removeCallbacksAndMessages(null);
        if(wm!=null&&panel!=null)try{wm.removeView(panel);}catch(Exception ignored){}
        super.onDestroy();
    }
}
