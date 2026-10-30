package com.elitex.performance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.InputType;
import androidx.documentfile.provider.DocumentFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/** SAF based file manager: operates only on folders granted by user. */
public final class GameFileManager {
    private static final int REQ_TREE=517;
    private final MainActivity a;
    private DocumentFile root;
    private final ArrayDeque<DocumentFile> parents=new ArrayDeque<>();
    private DocumentFile current;
    private LinearLayout target;
    GameFileManager(MainActivity activity){a=activity;
        String saved=a.prefs().getString("file_tree","");
        if(!saved.isEmpty())try{root=DocumentFile.fromTreeUri(a,Uri.parse(saved));current=root;}catch(Exception ignored){}
    }
    private void message(String s){a.info(s);}
    private void select(){
        Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        a.startActivityForResult(pick,REQ_TREE);
    }
    public boolean result(int code,int result,Intent data){
        if(code!=REQ_TREE)return false;
        if(result==Activity.RESULT_OK && data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            try{
                a.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                root=DocumentFile.fromTreeUri(a,uri);current=root;parents.clear();
                a.prefs().edit().putString("file_tree",uri.toString()).apply();
            }catch(Exception e){message("تعذر فتح المجلد: "+e.getMessage());}
        }
        show();return true;
    }
    public void render(LinearLayout container){target=container;show();}
    private void show(){
        if(target==null)return;
        target.removeAllViews();
        target.addView(Ui.text(a,"FILE EXPLORER",23,Ui.TXT,true));
        target.addView(Ui.text(a,"مدير ملفات الألعاب · SAF",13,Ui.MUTED,false),Ui.lp(a,-1,-2,5));
        target.addView(Ui.gap(a,16));
        TextView pick=Ui.button(a,"+  اختيار مجلد بإذن Android",true);target.addView(pick);pick.setOnClickListener(v->select());
        target.addView(Ui.gap(a,12));
        TextView backup=Ui.button(a,"↶  النسخ الاحتياطية / استعادة",false);target.addView(backup);
        backup.setOnClickListener(v->showBackups());
        target.addView(Ui.gap(a,14));
        LinearLayout card=Ui.card(a);target.addView(card,Ui.lp(a,-1,-2,0));
        if(current==null || !current.exists()){
            card.addView(Ui.text(a,"لا يوجد مجلد مختار",16,Ui.TXT,true));
            card.addView(Ui.text(a,"اختر مجلدًا مسموحًا من نافذة Android لعرض الملفات وتحرير النصوص. بعض مجلدات Android/data و OBB محجوبة من النظام.",12,Ui.MUTED,false),Ui.lp(a,-1,-2,10));
            return;
        }
        card.addView(Ui.text(a,"◉  "+current.getName(),16,Ui.CYAN,true));
        card.addView(Ui.text(a,"يمكن تعديل INI / CFG / JSON / TXT فقط · أقصى حجم 256 KB",12,Ui.MUTED,false),Ui.lp(a,-1,-2,8));
        if(!parents.isEmpty()){
            TextView back=Ui.button(a,"←  المجلد السابق",false);card.addView(back,Ui.lp(a,-1,-2,10));
            back.setOnClickListener(v->{current=parents.pop();show();});
        }
        DocumentFile[] entries;
        try{entries=current.listFiles();}catch(Exception e){card.addView(Ui.label(a,"تعذر قراءة المجلد: "+e.getMessage()));return;}
        Arrays.sort(entries,Comparator.comparing((DocumentFile d)->!d.isDirectory()).thenComparing(d->d.getName()==null?"":d.getName(),String.CASE_INSENSITIVE_ORDER));
        if(entries.length==0)card.addView(Ui.label(a,"المجلد فارغ"),Ui.lp(a,-1,-2,12));
        for(int i=0;i<Math.min(entries.length,150);i++){
            DocumentFile f=entries[i];String name=f.getName()==null?"بدون اسم":f.getName();
            LinearLayout row=Ui.row(a);row.setPadding(0,Ui.dp(a,10),0,Ui.dp(a,10));
            TextView icon=Ui.text(a,f.isDirectory()?"▣":"≡",24,f.isDirectory()?Ui.PURPLE:Ui.CYAN,true);
            row.addView(icon,Ui.lp(a,38,42,0));
            LinearLayout meta=Ui.col(a);row.addView(meta,new LinearLayout.LayoutParams(0,-2,1));
            TextView fname=Ui.text(a,name,14,Ui.TXT,true);fname.setSingleLine(true);fname.setEllipsize(android.text.TextUtils.TruncateAt.END);meta.addView(fname);
            meta.addView(Ui.label(a,f.isDirectory()?"FOLDER":formatSize(f.length())));
            card.addView(row);
            row.setOnClickListener(v->{if(f.isDirectory()){parents.push(current);current=f;show();}else openFile(f);});
            android.view.View line=new android.view.View(a);line.setBackgroundColor(Ui.LINE);card.addView(line,Ui.lp(a,-1,1,0));
        }
        if(entries.length>150)card.addView(Ui.label(a,"أول 150 عنصر فقط"));
    }
    private static String formatSize(long size){return String.format(Locale.US,"%.1f KB",size/1024d);}
    private boolean isText(DocumentFile f){String n=f.getName();if(n==null)return false;n=n.toLowerCase(Locale.US);
        return n.endsWith(".ini")||n.endsWith(".cfg")||n.endsWith(".json")||n.endsWith(".txt")||n.endsWith(".properties")||n.endsWith(".xml");}
    private void openFile(DocumentFile f){
        if(!f.isFile()||!isText(f)){message("التعديل متاح لملفات الإعدادات النصية المدعومة فقط");return;}
        if(f.length()>262144){message("حجم الملف أكبر من 256 KB؛ لا يمكن فتحه في المحرر");return;}
        try {
            String original=read(f.getUri());
            EditText edit=new EditText(a);edit.setText(original);edit.setTextSize(12);edit.setTypeface(Typeface.MONOSPACE);
            edit.setGravity(Gravity.TOP|Gravity.START);edit.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            edit.setMinLines(10);edit.setPadding(Ui.dp(a,14),Ui.dp(a,12),Ui.dp(a,14),Ui.dp(a,12));
            ScrollView scroll=new ScrollView(a);scroll.addView(edit);scroll.setMinimumHeight(Ui.dp(a,280));
            AlertDialog dialog=new AlertDialog.Builder(a).setTitle("EDIT · "+f.getName())
                .setView(scroll)
                .setPositiveButton("حفظ + Backup",(d,w)->{
                    try{
                        String contents=edit.getText().toString();
                        if(contents.getBytes(StandardCharsets.UTF_8).length>262144){message("التعديل يتجاوز 256 KB");return;}
                        backup(f.getUri(),f.getName(),original);
                        write(f.getUri(),contents);
                        message("تم الحفظ مع نسخة احتياطية ✓");
                    }catch(Exception e){message("فشل الحفظ: "+e.getMessage());}
                })
                .setNeutralButton("UE preset",null)
                .setNegativeButton("إلغاء",null).create();
            dialog.setOnShowListener(v->{
                android.widget.Button b=dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                b.setVisibility(f.getName()!=null&&f.getName().toLowerCase(Locale.US).endsWith(".ini")?android.view.View.VISIBLE:android.view.View.GONE);
                b.setOnClickListener(x->new AlertDialog.Builder(a).setTitle("Unreal Engine · إعدادات تجريبية")
                    .setMessage("إضافة مقطع SystemSettings أسفل الملف؛ قد لا تتعرف عليه بعض الألعاب. راجع النص قبل الحفظ.")
                    .setItems(new String[]{"Low latency / Lower graphics","Balanced quality"},(dia,which)->{
                        String snippet="\n\n[SystemSettings]\nr.MotionBlurQuality=0\nr.DepthOfFieldQuality=0\nr.ShadowQuality="+(which==0?"0":"2")+
                            "\nr.PostProcessAAQuality="+(which==0?"1":"3")+"\nr.ScreenPercentage="+(which==0?"85":"100")+"\n";
                        edit.append(snippet);message("أضيف المقطع للمحرر — اضغط حفظ إن أردت تطبيقه");
                    }).show());
            });
            dialog.show();
        }catch(Exception e){message("لا يمكن قراءة الملف: "+e.getMessage());}
    }
    private String read(Uri uri)throws Exception{
        try(InputStream stream=a.getContentResolver().openInputStream(uri)){
            if(stream==null)throw new Exception("لا يوجد stream");
            java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;
            while((n=stream.read(buf))!=-1){if(out.size()+n>262144)throw new Exception("الملف كبير");out.write(buf,0,n);}
            return out.toString("UTF-8");
        }
    }
    private void write(Uri uri,String text)throws Exception{
        try(OutputStream stream=a.getContentResolver().openOutputStream(uri,"wt")){
            if(stream==null)throw new Exception("ملف للقراءة فقط");stream.write(text.getBytes(StandardCharsets.UTF_8));stream.flush();
        }
    }
    private File backupDir(){File folder=new File(a.getFilesDir(),"backups");folder.mkdirs();return folder;}
    private void backup(Uri uri,String name,String text)throws Exception{
        File folder=backupDir();String id=String.valueOf(System.currentTimeMillis());
        File data=new File(folder,id+".bak");
        try(FileOutputStream output=new FileOutputStream(data)){output.write(text.getBytes(StandardCharsets.UTF_8));}
        File meta=new File(folder,id+".uri");
        try(FileOutputStream output=new FileOutputStream(meta)){
            output.write((uri.toString()+"\n"+(name==null?"config":name)).getBytes(StandardCharsets.UTF_8));
        }
    }
    private void showBackups(){
        File[] list=backupDir().listFiles((dir,name)->name.endsWith(".uri"));
        if(list==null||list.length==0){message("لا يوجد نسخ احتياطية بعد");return;}
        Arrays.sort(list,(x,y)->Long.compare(y.lastModified(),x.lastModified()));
        final String[] labels=new String[Math.min(list.length,60)];
        for(int i=0;i<labels.length;i++){
            try{String[] meta=new String(java.nio.file.Files.readAllBytes(list[i].toPath()),StandardCharsets.UTF_8).split("\n",2);
                labels[i]=(meta.length>1?meta[1]:"file")+"  ·  "+new SimpleDateFormat("dd/MM HH:mm",Locale.US).format(new Date(list[i].lastModified()));
            }catch(Exception e){labels[i]="backup "+i;}
        }
        new AlertDialog.Builder(a).setTitle("Backup Vault · Restore").setItems(labels,(d,index)->{
            File meta=list[index];File data=new File(meta.getParentFile(),meta.getName().replace(".uri",".bak"));
            new AlertDialog.Builder(a).setTitle("تأكيد الاستعادة")
                .setMessage("سيُحفظ Backup جديد من الملف الحالي ثم يُستبدل بمحتوى النسخة المحددة، إذا كان الملف لا يزال متاحًا.")
                .setNegativeButton("إلغاء",null).setPositiveButton("استعادة",(di,w)->{
                    try{
                        String[] info=new String(java.nio.file.Files.readAllBytes(meta.toPath()),StandardCharsets.UTF_8).split("\n",2);
                        Uri uri=Uri.parse(info[0]);String original=read(uri);
                        String version=new String(java.nio.file.Files.readAllBytes(data.toPath()),StandardCharsets.UTF_8);
                        backup(uri,info.length>1?info[1]:"config",original);
                        write(uri,version);message("تمت الاستعادة ✓");
                    }catch(Exception e){message("تعذرت الاستعادة: "+e.getMessage());}
                }).show();
        }).setNegativeButton("إغلاق",null).show();
    }
}
