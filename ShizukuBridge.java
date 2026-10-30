package com.elitex.performance;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CopyOnWriteArrayList;
import rikka.shizuku.Shizuku;

/** Async facade. All Binder command/metric calls run off main thread. */
public final class ShizukuBridge {
    public interface Result {void accept(String text);}
    private static volatile ShizukuBridge instance;
    public static ShizukuBridge get(Context c){
        if(instance==null)synchronized(ShizukuBridge.class){if(instance==null)instance=new ShizukuBridge(c.getApplicationContext());}
        return instance;
    }
    private final Context context;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final CopyOnWriteArrayList<Runnable> observers=new CopyOnWriteArrayList<>();
    private volatile IEliteService service;
    private volatile boolean binding;
    private final Shizuku.UserServiceArgs args;
    private ShizukuBridge(Context c){
        context=c;
        args=new Shizuku.UserServiceArgs(new ComponentName(c,EliteUserService.class))
                .processNameSuffix("elite_x_shell").tag("elite_x_privileged").version(1).daemon(false);
        try{
            Shizuku.addBinderReceivedListenerSticky(this::bindIfReady);
            Shizuku.addBinderDeadListener(()->{service=null;binding=false;notifyObservers();});
            Shizuku.addRequestPermissionResultListener((request,grant)->{
                if(grant==PackageManager.PERMISSION_GRANTED)bindIfReady();notifyObservers();
            });
        }catch(Throwable ignored){}
    }
    private void notifyObservers(){handler.post(()->{for(Runnable r:observers){try{r.run();}catch(Exception ignored){}}});}
    public void observe(Runnable r){observers.addIfAbsent(r);}
    public void remove(Runnable r){observers.remove(r);}
    public boolean isRunning(){try{return Shizuku.pingBinder();}catch(Throwable e){return false;}}
    public boolean permitted(){try{return isRunning()&&Shizuku.checkSelfPermission()==PackageManager.PERMISSION_GRANTED;}catch(Throwable e){return false;}}
    public boolean connected(){return service!=null;}
    public String status(){return !isRunning()?"SHIZUKU OFFLINE":!permitted()?"PERMISSION NEEDED":connected()?"SHIZUKU CONNECTED":"CONNECTING…";}
    public void request(){
        if(!isRunning())return;
        try{if(permitted())bindIfReady();else Shizuku.requestPermission(2201);}catch(Throwable ignored){}
        notifyObservers();
    }
    public synchronized void bindIfReady(){
        if(!permitted()||service!=null||binding)return;
        binding=true;
        try{Shizuku.bindUserService(args,connection);}catch(Throwable e){binding=false;notifyObservers();}
    }
    private final ServiceConnection connection=new ServiceConnection(){
        @Override public void onServiceConnected(ComponentName name,IBinder binder){service=IEliteService.Stub.asInterface(binder);binding=false;notifyObservers();}
        @Override public void onServiceDisconnected(ComponentName name){service=null;binding=false;notifyObservers();}
    };
    public void command(String op,String arg,Result done){
        executor.execute(()->{
            String output;
            IEliteService s=service;
            if(s==null)output="ERROR: Shizuku غير متصل. فعّل الصلاحية أولاً.";
            else try{output=s.execute(op,arg==null?"":arg);}catch(Exception e){output="ERROR: "+e.getMessage();}
            String finalText=output;handler.post(()->done.accept(finalText));
        });
    }
    public void metrics(Result done){
        executor.execute(()->{
            String result="-1|-1";
            IEliteService s=service;
            if(s!=null)try{result=s.readMetrics();}catch(Exception ignored){}
            String value=result;handler.post(()->done.accept(value));
        });
    }
}
