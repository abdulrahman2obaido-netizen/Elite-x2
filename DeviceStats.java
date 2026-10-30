package com.elitex.performance;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import java.io.File;

public final class DeviceStats {
    public static String memory(Context c){
        ActivityManager am=(ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        if(am==null)return "N/A";
        ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();am.getMemoryInfo(m);
        double used=(m.totalMem-m.availMem)/(1024d*1024*1024);double total=m.totalMem/(1024d*1024*1024);
        return String.format(java.util.Locale.US,"%.1f / %.1f GB",used,total);
    }
    public static String batteryTemp(Context c){
        Intent i=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(i==null)return "N/A";int t=i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,Integer.MIN_VALUE);
        return t==Integer.MIN_VALUE?"N/A":String.format(java.util.Locale.US,"%.1f °C",t/10f);
    }
    public static String thermal(Context c){
        if(Build.VERSION.SDK_INT<29)return "غير مدعوم";
        PowerManager p=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        if(p==null)return "N/A";
        switch(p.getCurrentThermalStatus()){
            case PowerManager.THERMAL_STATUS_NONE:return "NORMAL";
            case PowerManager.THERMAL_STATUS_LIGHT:return "LIGHT";
            case PowerManager.THERMAL_STATUS_MODERATE:return "MODERATE";
            case PowerManager.THERMAL_STATUS_SEVERE:return "SEVERE";
            case PowerManager.THERMAL_STATUS_CRITICAL:return "CRITICAL";
            case PowerManager.THERMAL_STATUS_EMERGENCY:return "EMERGENCY";
            case PowerManager.THERMAL_STATUS_SHUTDOWN:return "SHUTDOWN";
            default:return "N/A";
        }
    }
    public static String chipset(){
        if(Build.VERSION.SDK_INT>=31 && Build.SOC_MODEL!=null&&!Build.SOC_MODEL.isEmpty())return Build.SOC_MODEL;
        return Build.HARDWARE;
    }
    public static long removeOwnCache(Context c){return deleteChildren(c.getCacheDir())+deleteChildren(c.getExternalCacheDir());}
    private static long deleteChildren(File folder){
        if(folder==null || !folder.exists() || !folder.isDirectory())return 0;
        File[] files=folder.listFiles();if(files==null)return 0;long n=0;
        for(File f:files)n+=erase(f);
        return n;
    }
    private static long erase(File f){
        long bytes=0;
        if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File child:children)bytes+=erase(child);}
        else bytes=f.length();
        return f.delete()?bytes:0;
    }
    public static String bytes(long b){return String.format(java.util.Locale.US,"%.2f MB",b/(1024d*1024));}
}
