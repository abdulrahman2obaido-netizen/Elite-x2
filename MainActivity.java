package com.elitex.performance;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.FrameLayout;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private LinearLayout screen,body,bottom;
    private ScrollView scroll;
    private TextView status,cpuCaption,gpuCaption,ramCaption,tempCaption,thermCaption;
    private GaugeView gauge;
    private int tab=0;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AtomicBoolean reading=new AtomicBoolean(false);
    private ShizukuBridge shizuku;
    private GameFileManager files;
    private boolean overlayPending=false;
    private final Runnable updateStatus=()->{if(status!=null){status.setText(shizuku.status());status.setTextColor(shizuku.connected()?Ui.GREEN:Ui.AMBER);}};

    public android.content.SharedPreferences prefs(){return getSharedPreferences("elite_prefs",MODE_PRIVATE);}
    public void info(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private String gamePackage(){return prefs().getString("game_package","");}
    private String gameName(){return prefs().getString("game_name","No game selected");}

    @Override protected void onCreate(Bundle saved){super.onCreate(saved);
        getWindow().setStatusBarColor(Ui.BG);getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
        shizuku=ShizukuBridge.get(this);shizuku.observe(updateStatus);files=new GameFileManager(this);
        buildFrame();showTab(0);handler.post(refresh);
    }
    private void buildFrame(){
        screen=Ui.col(this);screen.setBackgroundColor(Ui.BG);setContentView(screen);
        screen.setOnApplyWindowInsetsListener((v,insets)->{
            int top,bottom;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
                top=bars.top;bottom=bars.bottom;
            }else {top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}
            screen.setPadding(0,top,0,bottom);
            return insets;
        });
        LinearLayout header=Ui.row(this);header.setPadding(Ui.dp(this,22),Ui.dp(this,19),Ui.dp(this,20),Ui.dp(this,16));
        LinearLayout brand=Ui.col(this);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView logo=Ui.text(this,"◈  ELITE X",24,Ui.TXT,true);logo.setLetterSpacing(.08f);brand.addView(logo);
        brand.addView(Ui.text(this,"SNAPDRAGON  •  GAME CONTROL",10,Ui.MUTED,true),Ui.lp(this,-1,-2,4));
        LinearLayout shizukuBox=Ui.col(this);shizukuBox.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,8));
        shizukuBox.setBackground(Ui.outline(this,Ui.CARD,Ui.LINE,14));header.addView(shizukuBox);
        status=Ui.text(this,"CHECKING…",10,Ui.AMBER,true);shizukuBox.addView(status);
        TextView hint=Ui.text(this,"●  SHIZUKU",9,Ui.MUTED,true);shizukuBox.addView(hint);
        shizukuBox.setOnClickListener(v->manageShizuku());screen.addView(header);
        View line=new View(this);line.setBackgroundColor(Ui.LINE);screen.addView(line,Ui.lp(this,-1,1,0));
        scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        body=Ui.col(this);body.setPadding(Ui.dp(this,18),Ui.dp(this,22),Ui.dp(this,18),Ui.dp(this,32));
        scroll.addView(body);screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        bottom=Ui.row(this);bottom.setBackground(Ui.outline(this,0xff101626,Ui.LINE,24));
        bottom.setPadding(Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4),Ui.dp(this,8));
        LinearLayout.LayoutParams nav=Ui.lp(this,-1,-2,0);nav.leftMargin=Ui.dp(this,9);nav.rightMargin=Ui.dp(this,9);nav.bottomMargin=Ui.dp(this,8);
        screen.addView(bottom,nav);updateStatus.run();
    }
    private void nav(){
        bottom.removeAllViews();String[] icons={"◈","⚡","▣","≡","⚙"};
        String[] names={"HOME","BOOST","GAMES","FILES","TOOLS"};
        for(int i=0;i<5;i++){
            final int item=i;
            LinearLayout col=Ui.col(this);col.setGravity(Gravity.CENTER);col.setPadding(0,Ui.dp(this,9),0,Ui.dp(this,9));
            col.setBackground(Ui.shape(this,i==tab?0xff242B49:Color.TRANSPARENT,15));
            TextView glyph=Ui.text(this,icons[i],20,i==tab?Ui.CYAN:Ui.MUTED,true);glyph.setGravity(Gravity.CENTER);col.addView(glyph);
            TextView label=Ui.text(this,names[i],9,i==tab?Ui.TXT:Ui.MUTED,true);label.setGravity(Gravity.CENTER);col.addView(label);
            bottom.addView(col,new LinearLayout.LayoutParams(0,-2,1));col.setOnClickListener(v->showTab(item));
        }
    }
    private void showTab(int index){
        tab=index;body.removeAllViews();gauge=null;nav();scroll.scrollTo(0,0);
        switch(index){case 0:home();break;case 1:boost();break;case 2:games();break;case 3:files.render(body);break;case 4:tools();break;}
    }
    private void addHeader(String eyebrow,String title,String subtitle){
        TextView e=Ui.text(this,eyebrow,11,Ui.CYAN,true);e.setLetterSpacing(.18f);body.addView(e);
        body.addView(Ui.text(this,title,28,Ui.TXT,true),Ui.lp(this,-1,-2,7));
        body.addView(Ui.text(this,subtitle,13,Ui.MUTED,false),Ui.lp(this,-1,-2,8));body.addView(Ui.gap(this,20));
    }
    private void home(){
        LinearLayout hero=Ui.col(this);hero.setPadding(Ui.dp(this,21),Ui.dp(this,22),Ui.dp(this,21),Ui.dp(this,24));
        hero.setBackground(Ui.gradient(this,0xff28245C,0xff11283B,26));body.addView(hero);
        hero.addView(Ui.chip(this,"◉  ENGINE READY · V1.0",Ui.GREEN));
        hero.addView(Ui.text(this,"YOUR GAME.\nYOUR CONTROL.",30,Ui.TXT,true),Ui.lp(this,-1,-2,14));
        hero.addView(Ui.text(this,"System-level gaming tools using Shizuku. Performance depends on your device and game support.",13,0xffD0D9FF,false),Ui.lp(this,-1,-2,9));
        hero.addView(Ui.text(this,"SOC  ·  "+DeviceStats.chipset(),11,Ui.CYAN,true),Ui.lp(this,-1,-2,14));
        TextView start=Ui.button(this,"⚡  ACTIVATE GAME MODE",true);hero.addView(start,Ui.lp(this,-1,-2,18));start.setOnClickListener(v->applyGameMode("game_performance"));
        body.addView(Ui.gap(this,18));
        LinearLayout chart=Ui.card(this);body.addView(chart);
        LinearLayout title=Ui.row(this);chart.addView(title);
        TextView section=Ui.text(this,"LIVE HARDWARE",16,Ui.TXT,true);title.addView(section,new LinearLayout.LayoutParams(0,-2,1));
        title.addView(Ui.chip(this,"●  LIVE",Ui.GREEN));
        gauge=new GaugeView(this);chart.addView(gauge,Ui.lp(this,-1,185,6));
        cpuCaption=Ui.label(this,"CPU load: sampling requires Shizuku / compatible procfs");chart.addView(cpuCaption);
        gpuCaption=Ui.label(this,"GPU load: available only if exposed by GPU driver");chart.addView(gpuCaption,Ui.lp(this,-1,-2,4));
        body.addView(Ui.gap(this,15));
        LinearLayout tiles=Ui.row(this);body.addView(tiles);LinearLayout left=Ui.col(this),right=Ui.col(this);
        LinearLayout.LayoutParams lpL=new LinearLayout.LayoutParams(0,-2,1);lpL.rightMargin=Ui.dp(this,7);
        LinearLayout.LayoutParams lpR=new LinearLayout.LayoutParams(0,-2,1);lpR.leftMargin=Ui.dp(this,7);
        tiles.addView(left,lpL);tiles.addView(right,lpR);
        ramCaption=miniMetric(left,"SYSTEM MEMORY",DeviceStats.memory(this),Ui.PURPLE);
        tempCaption=miniMetric(right,"BATTERY TEMP",DeviceStats.batteryTemp(this),Ui.AMBER);
        body.addView(Ui.gap(this,14));
        LinearLayout thermalCard=Ui.card(this);body.addView(thermalCard);
        thermalCard.addView(Ui.text(this,"THERMAL MANAGEMENT",12,Ui.MUTED,true));
        thermCaption=Ui.text(this,DeviceStats.thermal(this),22,Ui.GREEN,true);
        thermalCard.addView(thermCaption,Ui.lp(this,-1,-2,6));
        thermalCard.addView(Ui.text(this,"حالة تحذير الحرارة من Android، وليست حرارة أنوية CPU.",11,Ui.MUTED,false),Ui.lp(this,-1,-2,3));
        body.addView(Ui.gap(this,19));
        sectionTitle("QUICK ACCESS","أدوات بلمسة واحدة");
        quick("◉","Gaming HUD Overlay","نافذة فوق الألعاب",()->toggleOverlay());
        quick("⚡","Boost Center","وضع اللعبة وتنظيف الخلفية",()->showTab(1));
        quick("▣","Game File Manager","تعديل الملفات مع Backup",()->showTab(3));
    }
    private TextView miniMetric(LinearLayout parent,String label,String value,int color){
        LinearLayout card=Ui.card(this);parent.addView(card);
        card.addView(Ui.text(this,label,10,Ui.MUTED,true));
        TextView metric=Ui.text(this,value,16,color,true);card.addView(metric,Ui.lp(this,-1,-2,11));return metric;
    }
    private void sectionTitle(String title,String subtitle){
        body.addView(Ui.text(this,title,17,Ui.TXT,true));
        body.addView(Ui.text(this,subtitle,12,Ui.MUTED,false),Ui.lp(this,-1,-2,5));body.addView(Ui.gap(this,14));
    }
    private void quick(String symbol,String title,String subtitle,Runnable action){
        LinearLayout card=Ui.card(this);LinearLayout row=Ui.row(this);card.addView(row);
        TextView icon=Ui.text(this,symbol,25,Ui.CYAN,true);icon.setGravity(Gravity.CENTER);
        icon.setBackground(Ui.shape(this,0xff222B4B,15));row.addView(icon,Ui.lp(this,53,53,0));
        LinearLayout text=Ui.col(this);LinearLayout.LayoutParams t=new LinearLayout.LayoutParams(0,-2,1);t.leftMargin=Ui.dp(this,14);row.addView(text,t);
        text.addView(Ui.text(this,title,15,Ui.TXT,true));text.addView(Ui.text(this,subtitle,12,Ui.MUTED,false),Ui.lp(this,-1,-2,4));
        row.addView(Ui.text(this,"›",27,Ui.MUTED,false));body.addView(card,Ui.lp(this,-1,-2,0));body.addView(Ui.gap(this,10));
        card.setOnClickListener(v->action.run());
    }
    private void boost(){
        addHeader("ENGINE / TUNING","Performance Lab","تحسين الأداء باستخدام إعدادات Android المدعومة");
        LinearLayout current=Ui.card(this);body.addView(current);
        current.addView(Ui.text(this,"TARGET GAME",11,Ui.MUTED,true));
        current.addView(Ui.text(this,gameName(),20,Ui.CYAN,true),Ui.lp(this,-1,-2,8));
        current.addView(Ui.label(this,gamePackage().isEmpty()?"اختر لعبة لتفعيل Game Mode":gamePackage()));
        TextView choose=Ui.button(this,"◈  SELECT GAME",false);current.addView(choose,Ui.lp(this,-1,-2,14));choose.setOnClickListener(v->pickGame());
        body.addView(Ui.gap(this,20));sectionTitle("GAME MODE PROFILES","يتطلب Shizuku ودعم اللعبة من النظام");
        mode("⚡","PERFORMANCE","اطلب وضع الأداء من Android GameManager","game_performance",Ui.CYAN);
        mode("◉","BALANCED","استعادة وضع Standard الافتراضي","game_balanced",Ui.PURPLE);
        mode("◐","BATTERY SAVER","تقليل الاستهلاك حين يدعم الجهاز الوضع","game_battery",Ui.GREEN);
        body.addView(Ui.gap(this,17));sectionTitle("MEMORY & STORAGE","عمليات محدودة لتجنب حذف بيانات الألعاب");
        quick("✧","Smart Cache Cleanup","تنظيف كاش التطبيق + طلب حذف كاش قابل للاستعادة",this::askCacheClean);
        quick("×","Close Background Processes","Android am kill-all (اختياري)",this::askBackgroundKill);
        LinearLayout caveat=Ui.card(this);body.addView(caveat);
        caveat.addView(Ui.text(this,"ABOUT PERFORMANCE",13,Ui.AMBER,true));
        caveat.addView(Ui.text(this,"إغلاق التطبيقات وتنظيف الكاش لا يرفع FPS بشكل مضمون. تنفيذ Game Mode قد يفشل على ألعاب أو أجهزة لا تدعم هذا الوضع، وسيظهر الخطأ الفعلي.",12,Ui.MUTED,false),Ui.lp(this,-1,-2,9));
    }
    private void mode(String icon,String title,String description,String op,int color){
        LinearLayout c=Ui.card(this);body.addView(c,Ui.lp(this,-1,-2,0));
        LinearLayout row=Ui.row(this);c.addView(row);
        TextView i=Ui.text(this,icon,27,color,true);row.addView(i,Ui.lp(this,40,-2,0));
        LinearLayout texts=Ui.col(this);row.addView(texts,new LinearLayout.LayoutParams(0,-2,1));
        texts.addView(Ui.text(this,title,16,Ui.TXT,true));
        texts.addView(Ui.text(this,description,12,Ui.MUTED,false),Ui.lp(this,-1,-2,4));
        TextView go=Ui.button(this,"APPLY",false);c.addView(go,Ui.lp(this,-1,-2,14));
        go.setOnClickListener(v->applyGameMode(op));body.addView(Ui.gap(this,10));
    }
    private void games(){
        addHeader("LAUNCHER / PROFILES","Game Library","اختر لعبة مثبتة، غيّر وضعها، وشغّلها");
        LinearLayout target=Ui.card(this);body.addView(target);
        target.addView(Ui.chip(this,"SELECTED TARGET",Ui.GREEN));
        target.addView(Ui.text(this,gameName(),24,Ui.TXT,true),Ui.lp(this,-1,-2,16));
        target.addView(Ui.text(this,gamePackage().isEmpty()?"NO GAME SELECTED":gamePackage(),12,Ui.MUTED,false),Ui.lp(this,-1,-2,7));
        TextView select=Ui.button(this,"+  CHOOSE INSTALLED GAME",true);target.addView(select,Ui.lp(this,-1,-2,20));select.setOnClickListener(v->pickGame());
        TextView launch=Ui.button(this,"▶  LAUNCH SELECTED GAME",false);target.addView(launch,Ui.lp(this,-1,-2,10));launch.setOnClickListener(v->launchGame());
        TextView fast=Ui.button(this,"⚡  BOOST & LAUNCH",false);target.addView(fast,Ui.lp(this,-1,-2,10));fast.setOnClickListener(v->boostLaunch());
        body.addView(Ui.gap(this,17));
        sectionTitle("GAME ACTIONS","هذه الإجراءات تتطلب موافقتك");
        quick("⚡","Enable Performance","تفعيل وضع اللعبة المختارة",()->applyGameMode("game_performance"));
        quick("◉","Return to Standard","إعادة الوضع العادي",()->applyGameMode("game_balanced"));
        quick("×","Force-stop selected game","قد تفقد البيانات غير المحفوظة",this::forceStopGame);
        body.addView(Ui.gap(this,8));
        LinearLayout help=Ui.card(this);body.addView(help);
        help.addView(Ui.text(this,"PROFILE INFO",12,Ui.CYAN,true));
        help.addView(Ui.text(this,"التبديل يستخدم cmd game mode. بعض الألعاب لا تدعم Performance/Battery Mode. لا يتم تغيير ملفات اللعبة في هذه الصفحة.",12,Ui.MUTED,false),Ui.lp(this,-1,-2,8));
    }
    private void tools(){
        addHeader("CONTROL / DIAGNOSTICS","Advanced Tools","أدوات النظام ومراقبة الأداء والصلاحيات");
        quick("◉","Shizuku Connectivity",shizuku.status(),this::manageShizuku);
        quick("▣","Gaming HUD Overlay",OverlayService.active?"ACTIVE · tap to stop":"INACTIVE · tap to start",this::toggleOverlay);
        quick("◌","Network TCP Latency","قياس اختبار اتصال، ليس Ping سيرفر اللعبة",this::testNetwork);
        quick("⇧","Export Device Report","مشاركة ملخص مواصفات الجهاز والحالة",this::exportReport);
        quick("⚡","Fixed Performance Mode","للاختبارات فقط وليس زيادة تردد قصوى",this::fixedPerformanceDialog);
        quick("✧","Clear This App Cache","يُحذف كاش ELITE X فقط",this::clearOwnCache);
        LinearLayout detail=Ui.card(this);body.addView(detail,Ui.lp(this,-1,-2,8));
        detail.addView(Ui.text(this,"SYSTEM INFORMATION",15,Ui.TXT,true));
        detail.addView(Ui.text(this,"Device  ·  "+Build.MANUFACTURER+" "+Build.MODEL,12,Ui.MUTED,false),Ui.lp(this,-1,-2,12));
        detail.addView(Ui.text(this,"SoC  ·  "+DeviceStats.chipset(),12,Ui.MUTED,false),Ui.lp(this,-1,-2,7));
        detail.addView(Ui.text(this,"Android  ·  "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")",12,Ui.MUTED,false),Ui.lp(this,-1,-2,7));
        detail.addView(Ui.text(this,"Display refresh  ·  "+String.format(Locale.US,"%.0f Hz",getWindowManager().getDefaultDisplay().getRefreshRate()),12,Ui.MUTED,false),Ui.lp(this,-1,-2,7));
        detail.addView(Ui.text(this,"Thermal state  ·  "+DeviceStats.thermal(this),12,Ui.MUTED,false),Ui.lp(this,-1,-2,7));
        detail.addView(Ui.text(this,"Android scoped storage & OEM policies may limit features. Nothing here promises an FPS or ping improvement.",11,Ui.AMBER,false),Ui.lp(this,-1,-2,16));
    }
    private void manageShizuku(){
        if(!shizuku.isRunning()){
            new AlertDialog.Builder(this).setTitle("Shizuku غير مُشغّل")
                .setMessage("ثبّت تطبيق Shizuku وشغّل خدمته عبر Wireless Debugging أو ADB، وبعدها ارجع إلى ELITE X وامنحه الإذن.")
                .setPositiveButton("صفحة Shizuku",(d,w)->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://shizuku.rikka.app/download/"))))
                .setNegativeButton("إغلاق",null).show();
        }else if(!shizuku.permitted())shizuku.request();
        else{shizuku.bindIfReady();info(shizuku.status());}
    }
    private void applyGameMode(String operation){
        if(gamePackage().isEmpty()){info("اختر لعبة من قائمة الألعاب أولاً");pickGame();return;}
        if(!shizuku.connected()){manageShizuku();return;}
        runCommand(operation,gamePackage(),"Game Mode · "+gameName());
    }
    private void runCommand(String operation,String arg,String title){
        shizuku.command(operation,arg,result->{
            String msg=result.startsWith("OK")?"تم تنفيذ الطلب من Android. لا يعني بالضرورة تغير FPS.\n\n"+result:result;
            new AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("حسنًا",null).show();
        });
    }
    private void confirm(String title,String description,Runnable yes){
        new AlertDialog.Builder(this).setTitle(title).setMessage(description)
            .setNegativeButton("إلغاء",null).setPositiveButton("متابعة",(d,w)->yes.run()).show();
    }
    private void askBackgroundKill(){
        confirm("Close Background Processes","قد يتوقف الصوت أو المزامنة أو التحميلات. سيستخدم Android am kill-all، وقد يعيد النظام تشغيل بعض الخدمات.",
            ()->{if(shizuku.connected())runCommand("kill_background","","Background cleanup");else manageShizuku();});
    }
    private void askCacheClean(){
        new AlertDialog.Builder(this).setTitle("Smart Cache Clean")
            .setMessage("Standard يطلب تفريغ الكاش حتى تتوفر مساحة 2GB وقد لا يحرر شيئًا. Deep يطلب إزالة كل الكاش القابل للإزالة ويؤدي إلى إعادة تحميله لاحقًا. لا يمسح بيانات الألعاب.")
            .setItems(new String[]{"Standard clean · 2GB", "Deep clean · disposable cache", "ELITE X local cache only"},(dialog,which)->{
                confirm("تأكيد تنظيف Cache", "قد تصبح بعض التطبيقات أبطأ في التشغيل الأول بعد تنظيف الكاش. هل تريد المتابعة؟",()->{
                    long own=DeviceStats.removeOwnCache(this);
                    if(which==2){info("ELITE X: "+DeviceStats.bytes(own));return;}
                    if(shizuku.connected())shizuku.command(which==0?"trim_caches":"trim_caches_deep","",value->
                        new AlertDialog.Builder(this).setTitle("Cache Result")
                            .setMessage("ELITE X cache: "+DeviceStats.bytes(own)+"\nSystem request: "+value+
                                "\nلا يعرض Android عدد البايتات المحذوفة من النظام في هذا الأمر.")
                            .setPositiveButton("OK",null).show());
                    else new AlertDialog.Builder(this).setTitle("Local cache cleared")
                        .setMessage("تم تنظيف "+DeviceStats.bytes(own)+" من كاش التطبيق. فعّل Shizuku لطلب تنظيف كاش النظام.")
                        .setPositiveButton("OK",null).show();
                });
            }).setNegativeButton("إلغاء",null).show();
    }
    private void clearOwnCache(){
        confirm("Clear app cache","حذف ملفات كاش ELITE X فقط دون المساس بحفظ الألعاب.",()->{
            long n=DeviceStats.removeOwnCache(this);info("تم تحرير "+DeviceStats.bytes(n));
        });
    }
    private void forceStopGame(){
        if(gamePackage().isEmpty()){pickGame();return;}
        try{
            android.content.pm.ApplicationInfo app=getPackageManager().getApplicationInfo(gamePackage(),0);
            if((app.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM)!=0 ||
               (app.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0){
                info("حماية النظام: لا يمكن Force-stop تطبيق نظام من داخل ELITE X");return;
            }
        }catch(Exception e){info("تعذر التحقق من التطبيق");return;}
        confirm("Force Stop · "+gameName(),"ستُغلق اللعبة فورًا، وقد تخسر تقدمًا غير محفوظ. لن يتم إغلاق أي تطبيق آخر.",()->{
            if(shizuku.connected())runCommand("force_stop",gamePackage(),"Force-stop result");else manageShizuku();
        });
    }
    private void boostLaunch(){
        if(gamePackage().isEmpty()){pickGame();return;}
        if(!shizuku.connected()){manageShizuku();return;}
        shizuku.command("game_performance",gamePackage(),response->{
            if(response.startsWith("OK")){info("تم تنفيذ Game Mode · تشغيل اللعبة");launchGame();}
            else new AlertDialog.Builder(this).setTitle("Game Mode unavailable")
                .setMessage(response+"\n\nهل تريد تشغيل اللعبة بدون هذا الوضع؟")
                .setPositiveButton("تشغيل",(d,w)->launchGame()).setNegativeButton("إلغاء",null).show();
        });
    }
    private void launchGame(){
        String pkg=gamePackage();if(pkg.isEmpty()){pickGame();return;}
        Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null){info("تعذر تشغيل اللعبة");return;}
        try{startActivity(i);}catch(Exception e){info("لا يمكن تشغيل اللعبة: "+e.getMessage());}
    }
    private static final class GameChoice {String label,pkg;GameChoice(String n,String p){label=n;pkg=p;}}
    private void pickGame(){
        info("جاري قراءة قائمة التطبيقات القابلة للتشغيل…");
        new Thread(()->{
            Intent find=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            ArrayList<GameChoice> games=new ArrayList<>();
            try{
                for(ResolveInfo ri:getPackageManager().queryIntentActivities(find,0)){
                    String pkg=ri.activityInfo.packageName;
                    if(!pkg.equals(getPackageName()))games.add(new GameChoice(String.valueOf(ri.loadLabel(getPackageManager())),pkg));
                }
            }catch(Exception ignored){}
            games.sort(Comparator.comparing(o->o.label.toLowerCase(Locale.ROOT)));
            runOnUiThread(()->presentGamePicker(games));
        }).start();
    }
    private void presentGamePicker(ArrayList<GameChoice> games){
        if(games.isEmpty()){info("لا توجد تطبيقات مرئية. تحقق من إعدادات Android.");return;}
        LinearLayout wrapper=Ui.col(this);wrapper.setPadding(Ui.dp(this,18),Ui.dp(this,8),Ui.dp(this,18),0);
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("ابحث عن لعبة / تطبيق");wrapper.addView(search);
        ListView list=new ListView(this);wrapper.addView(list,Ui.lp(this,-1,375,7));
        ArrayList<GameChoice> filtered=new ArrayList<>(games);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,new ArrayList<>());
        list.setAdapter(adapter);
        Runnable render=()->{
            adapter.clear();for(GameChoice g:filtered)adapter.add(g.label+"\n"+g.pkg);adapter.notifyDataSetChanged();
        };render.run();
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                String q=s.toString().toLowerCase(Locale.ROOT);filtered.clear();
                for(GameChoice g:games)if(g.label.toLowerCase(Locale.ROOT).contains(q)||g.pkg.toLowerCase(Locale.ROOT).contains(q))filtered.add(g);
                render.run();
            }
            public void afterTextChanged(Editable e){}
        });
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("SELECT GAME").setView(wrapper).setNegativeButton("إلغاء",null).create();
        list.setOnItemClickListener((parent,view,index,id)->{
            GameChoice chosen=filtered.get(index);
            prefs().edit().putString("game_package",chosen.pkg).putString("game_name",chosen.label).apply();dialog.dismiss();showTab(tab);info("الهدف: "+chosen.label);
        });
        dialog.show();
    }
    private void toggleOverlay(){
        if(OverlayService.active){stopService(new Intent(this,OverlayService.class));info("تم إيقاف Overlay");handler.postDelayed(()->showTab(tab),200);return;}
        if(!Settings.canDrawOverlays(this)){
            overlayPending=true;
            new AlertDialog.Builder(this).setTitle("إذن العرض فوق التطبيقات")
                .setMessage("يلزم السماح لـ ELITE X بعرض نافذة عائمة فوق الألعاب. افتح إعدادات Android ومنح الإذن.")
                .setPositiveButton("فتح الإعدادات",(d,w)->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))))
                .setNegativeButton("إلغاء",(d,w)->overlayPending=false).show();return;
        }
        startOverlayNow();
    }
    private void startOverlayNow(){
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},101);return;
        }
        try{startForegroundService(new Intent(this,OverlayService.class));info("Overlay started. Open your game.");}
        catch(Exception e){info("فشل تشغيل Overlay: "+e.getMessage());}
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==101){try{startForegroundService(new Intent(this,OverlayService.class));}catch(Exception e){info(e.getMessage());}}
    }
    private void exportReport(){
        String report="ELITE X · DEVICE REPORT\n"+Build.MANUFACTURER+" "+Build.MODEL+
            "\nSoC: "+DeviceStats.chipset()+"\nAndroid: "+Build.VERSION.RELEASE+
            "\nRAM: "+DeviceStats.memory(this)+"\nBattery temp: "+DeviceStats.batteryTemp(this)+
            "\nThermal: "+DeviceStats.thermal(this)+"\nShizuku: "+shizuku.status()+
            "\nTarget game: "+gameName()+" ("+gamePackage()+")";
        Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,report);
        startActivity(Intent.createChooser(share,"Export ELITE X report"));
    }
    private void fixedPerformanceDialog(){
        if(!shizuku.connected()){manageShizuku();return;}
        new AlertDialog.Builder(this).setTitle("Fixed Performance · Benchmark")
            .setMessage("هذا وضع لاختبارات الأداء المتكررة، وليس Turbo أو Overclock. قد يرفع الحرارة، ولا يضمن أفضل أداء. أوقفه بعد الاختبار.")
            .setNeutralButton("إيقاف",(d,w)->runCommand("fixed_performance_off","","Fixed Performance OFF"))
            .setPositiveButton("تفعيل للاختبار",(d,w)->runCommand("fixed_performance_on","","Fixed Performance ON"))
            .setNegativeButton("إلغاء",null).show();
    }
    private void testNetwork(){
        info("Network test started…");
        new Thread(()->{
            String result;
            long start=System.nanoTime();
            try(Socket s=new Socket()){
                s.connect(new InetSocketAddress("1.1.1.1",443),1800);
                double ms=(System.nanoTime()-start)/1_000_000d;
                result=String.format(Locale.US,"TCP handshake connection: %.0f ms\nEndpoint: 1.1.1.1:443\n\nهذه النتيجة ليست Ping سيرفر اللعبة، ولا تضمن تحسن الشبكة.",ms);
            }catch(Exception e){result="Network test unavailable: "+e.getClass().getSimpleName();}
            String finalResult=result;runOnUiThread(()->new AlertDialog.Builder(this).setTitle("NETWORK DIAGNOSTIC")
                .setMessage(finalResult).setPositiveButton("حسنًا",null).show());
        }).start();
    }
    private final Runnable refresh=new Runnable(){
        @Override public void run(){
            if(tab==0){
                if(ramCaption!=null)ramCaption.setText(DeviceStats.memory(MainActivity.this));
                if(tempCaption!=null)tempCaption.setText(DeviceStats.batteryTemp(MainActivity.this));
                if(thermCaption!=null){String t=DeviceStats.thermal(MainActivity.this);thermCaption.setText(t);
                    thermCaption.setTextColor(t.equals("SEVERE")||t.equals("CRITICAL")?Ui.AMBER:Ui.GREEN);}
                if(gauge!=null&&reading.compareAndSet(false,true))shizuku.metrics(result->{
                    reading.set(false);if(gauge==null)return;
                    try{String[] vals=result.split("\\|");float cpu=Float.parseFloat(vals[0]),gpu=Float.parseFloat(vals[1]);
                        gauge.setValues(cpu,gpu);
                        if(cpuCaption!=null)cpuCaption.setText("CPU · "+(cpu<0?"N/A · insufficient access":String.format(Locale.US,"%.0f%% system load",cpu)));
                        if(gpuCaption!=null)gpuCaption.setText("GPU · "+(gpu<0?"N/A · driver data unavailable":String.format(Locale.US,"%.0f%% utilization",gpu)));
                    }catch(Exception ignored){}
                });
            }
            handler.postDelayed(this,2400);
        }
    };
    @Override protected void onResume(){super.onResume();
        handler.postDelayed(()->{
            if(overlayPending&&Settings.canDrawOverlays(this)){overlayPending=false;startOverlayNow();}
            shizuku.bindIfReady();updateStatus.run();
        },350);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(files.result(request,result,data))showTab(3);
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);shizuku.remove(updateStatus);super.onDestroy();}
}
