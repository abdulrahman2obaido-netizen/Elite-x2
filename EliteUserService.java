package com.elitex.performance;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** This class runs under Shizuku's ADB-shell (or root) identity, NOT the app UID.
 * Every privileged action is on an explicit allowlist; there is no arbitrary shell interface. */
public final class EliteUserService extends IEliteService.Stub {
    private long prevTotal=-1,prevIdle=-1;
    public EliteUserService() {}

    @Override public String execute(String operation,String argument) {
        if(operation==null) return "ERROR: missing operation";
        String p=argument==null?"":argument;
        switch(operation) {
            case "game_performance":
                if(!safePackage(p))return "ERROR: invalid package";
                return run("/system/bin/cmd","game","mode","performance",p);
            case "game_balanced":
                if(!safePackage(p))return "ERROR: invalid package";
                return run("/system/bin/cmd","game","mode","standard",p);
            case "game_battery":
                if(!safePackage(p))return "ERROR: invalid package";
                return run("/system/bin/cmd","game","mode","battery",p);
            case "kill_background":
                return run("/system/bin/am","kill-all");
            case "trim_caches":
                return run("/system/bin/pm","trim-caches","2G");
            case "trim_caches_deep":
                return run("/system/bin/pm","trim-caches","512G");
            case "force_stop":
                if(!safePackage(p) || p.equals("com.elitex.performance") || p.startsWith("com.android.")
                        || p.startsWith("android.") || p.equals("com.google.android.gms"))return "ERROR: package blocked";
                return run("/system/bin/am","force-stop",p);
            case "fixed_performance_on":
                return run("/system/bin/cmd","power","set-fixed-performance-mode-enabled","true");
            case "fixed_performance_off":
                return run("/system/bin/cmd","power","set-fixed-performance-mode-enabled","false");
            default: return "ERROR: unsupported operation";
        }
    }
    private boolean safePackage(String pkg){return pkg.length()<200 && pkg.matches("[A-Za-z_][A-Za-z_0-9]*(\\.[A-Za-z_][A-Za-z_0-9]*)+");}
    private String run(String... command) {
        Process process=null;
        try {
            process=new ProcessBuilder(command).redirectErrorStream(true).start();
            boolean finished=process.waitFor(18,TimeUnit.SECONDS);
            if(!finished){process.destroyForcibly();return "ERROR: command timed out";}
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=process.getInputStream()){
                byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1 && out.size()<16384){out.write(buffer,0,n);}
            }
            String message=out.toString("UTF-8").trim();
            if(process.exitValue()!=0)return "ERROR (exit " + process.exitValue() + "): " + message;
            if(message.toLowerCase(Locale.US).contains("error") || message.toLowerCase(Locale.US).contains("not supported"))return "ERROR: " + message;
            return "OK"+(message.isEmpty()?"":" · "+message);
        }catch(Exception e){return "ERROR: "+e.getClass().getSimpleName()+" · "+e.getMessage();}
        finally {if(process!=null)process.destroy();}
    }
    @Override public synchronized String readMetrics() {
        return String.format(Locale.US,"%.1f|%.1f",sampleCpu(),sampleGpu());
    }
    private float sampleCpu(){
        try (BufferedReader in=new BufferedReader(new FileReader("/proc/stat"))){
            String line=in.readLine();if(line==null||!line.startsWith("cpu "))return -1;
            String[] words=line.trim().split("\\s+");long total=0;
            for(int i=1;i<words.length;i++)total+=Long.parseLong(words[i]);
            long idle=Long.parseLong(words[4])+Long.parseLong(words[5]);
            float result=-1;
            if(prevTotal>0 && total>prevTotal)result=100f*(1f-(float)(idle-prevIdle)/(total-prevTotal));
            prevTotal=total;prevIdle=idle;return result<0?-1:Math.min(100,result);
        }catch(Exception e){return -1;}
    }
    private float sampleGpu(){
        // GPU sysfs entries are vendor/kernel-specific and often inaccessible even to ADB shell.
        String busy=readFirstLine("/sys/class/kgsl/kgsl-3d0/gpubusy");
        if(busy!=null){
            try {String[] parts=busy.trim().split("\\s+");
                if(parts.length>=2){double b=Double.parseDouble(parts[0]),t=Double.parseDouble(parts[1]);
                    if(t>0 && b>=0 && b<=t)return (float)(b*100d/t);}
            }catch(Exception ignored){}
        }
        for(String path:new String[]{"/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/class/kgsl/kgsl-3d0/gpu_utilization"}) {
            String value=readFirstLine(path);
            if(value!=null) try{float p=Float.parseFloat(value.replace("%","").trim());if(p>=0&&p<=100)return p;}catch(Exception ignored){}
        }
        return -1;
    }
    private String readFirstLine(String path){
        File f=new File(path);if(!f.isFile()||!f.canRead())return null;
        try(BufferedReader r=new BufferedReader(new FileReader(f))){return r.readLine();}catch(Exception ignored){return null;}
    }
    public void destroy(){System.exit(0);}
}
