package com.elitex.performance;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

public class GaugeView extends View {
    private final Paint p=new Paint(3);
    private float cpu=-1,gpu=-1;
    public GaugeView(Context c){super(c);setMinimumHeight(Ui.dp(c,190));}
    public void setValues(float cpu,float gpu){this.cpu=cpu;this.gpu=gpu;invalidate();}
    private void circle(Canvas canvas,float cx,float cy,float radius,float value,int color,String name) {
        float stroke=Ui.dp(getContext(),8);
        RectF r=new RectF(cx-radius,cy-radius,cx+radius,cy+radius);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(stroke);p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor(Ui.LINE);canvas.drawArc(r,132,276,false,p);
        if(value>=0){p.setColor(color);canvas.drawArc(r,132,276*Math.min(value,100)/100f,false,p);}
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);
        p.setColor(Ui.TXT);p.setTypeface(android.graphics.Typeface.create("sans-serif",1));p.setTextSize(Ui.dp(getContext(),23));
        canvas.drawText(value<0?"N/A":String.format(java.util.Locale.US,"%.0f%%",value),cx,cy+Ui.dp(getContext(),4),p);
        p.setColor(Ui.MUTED);p.setTextSize(Ui.dp(getContext(),12));p.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText(name,cx,cy+Ui.dp(getContext(),27),p);
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
        float w=getWidth(),h=getHeight();float r=Math.min(w/4-Ui.dp(getContext(),12),Ui.dp(getContext(),70));
        r=Math.max(r,Ui.dp(getContext(),38));
        circle(canvas,w*.26f,h*.48f,r,cpu,Ui.CYAN,"CPU LOAD");
        circle(canvas,w*.74f,h*.48f,r,gpu,Ui.PURPLE,"GPU LOAD");
    }
}
