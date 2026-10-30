package com.elitex.performance;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    public static final int BG=0xff080C16, CARD=0xff121829, RAISED=0xff1B2338,
        LINE=0xff293149, TXT=0xffF3F6FF, MUTED=0xff98A4C0, PURPLE=0xff9678FF,
        CYAN=0xff32DAE8, GREEN=0xff6DF4B0, AMBER=0xffFFC46D;
    private Ui() {}
    public static int dp(Context c, float n) {return (int)(n*c.getResources().getDisplayMetrics().density+0.5f);}
    public static GradientDrawable shape(Context c,int fill,int radius) {
        GradientDrawable d = new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(c,radius));return d;
    }
    public static GradientDrawable outline(Context c,int fill,int stroke,int radius) {
        GradientDrawable d=shape(c,fill,radius);d.setStroke(dp(c,1),stroke);return d;
    }
    public static GradientDrawable gradient(Context c,int start,int end,int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{start,end});
        d.setCornerRadius(dp(c,radius)); return d;
    }
    public static TextView text(Context c,String value,float sp,int color,boolean bold) {
        TextView t=new TextView(c);t.setText(value);t.setTextSize(sp);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        t.setGravity(Gravity.CENTER_VERTICAL);t.setIncludeFontPadding(true);return t;
    }
    public static TextView label(Context c,String value) {return text(c,value,12,MUTED,false);}
    public static LinearLayout col(Context c) {LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    public static LinearLayout row(Context c) {LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.HORIZONTAL);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    public static LinearLayout card(Context c) {
        LinearLayout v=col(c);v.setPadding(dp(c,17),dp(c,17),dp(c,17),dp(c,17));
        v.setBackground(outline(c,CARD,LINE,22));return v;
    }
    public static LinearLayout.LayoutParams lp(Context c,int w,int h,int top) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));lp.topMargin=dp(c,top);return lp;
    }
    public static TextView button(Context c,String title,boolean primary) {
        TextView t=text(c,title,14,primary?0xff090E22:TXT,true);
        t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,52));t.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));
        t.setBackground(primary ? gradient(c,CYAN,0xff8BA7FF,16) : outline(c,RAISED,LINE,16));
        t.setClickable(true);t.setFocusable(true);return t;
    }
    public static TextView chip(Context c,String title,int color) {
        TextView t=text(c,title,11,color,true);t.setPadding(dp(c,10),dp(c,6),dp(c,10),dp(c,6));
        t.setBackground(outline(c,RAISED,LINE,20));return t;
    }
    public static View gap(Context c,int dp) {View v=new View(c);v.setLayoutParams(new LinearLayout.LayoutParams(1,Ui.dp(c,dp)));return v;}
}
