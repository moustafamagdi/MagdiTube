package com.magdi.tube;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;

/** Large touch controls; commands still pass through the current car playback lease. */
final class CarSeekControls extends LinearLayout {
 interface Command {void run(String action,double value);}
 private final SeekBar seek;private final TextView time;private final Button back,forward;
 private long gestureEpoch,gestureRequest,gestureOwner;private double duration;private boolean dragging;private final Command command;
 CarSeekControls(Context c,Command command){super(c);this.command=command;setOrientation(VERTICAL);setPadding(dp(10),dp(3),dp(10),dp(3));setBackgroundColor(0xe6101b28);
  LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);back=button(c,"−10",c.getString(R.string.app_name)+" −10s");forward=button(c,"+10",c.getString(R.string.app_name)+" +10s");time=new TextView(c);time.setTextColor(Color.WHITE);time.setTextSize(13);time.setGravity(Gravity.CENTER);row.addView(back,new LayoutParams(dp(56),dp(48)));row.addView(time,new LayoutParams(0,dp(48),1));row.addView(forward,new LayoutParams(dp(56),dp(48)));addView(row);seek=new SeekBar(c);seek.setMax(1000);seek.setContentDescription(c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar")?"تقديم الفيديو":"Seek video");seek.setLayoutDirection(LAYOUT_DIRECTION_LTR);addView(seek,new LayoutParams(-1,dp(48)));
  back.setOnClickListener(v->command.run("rewind",0));forward.setOnClickListener(v->command.run("forward",0));seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){dragging=true;gestureEpoch=CarController.epoch();gestureRequest=CarController.request();gestureOwner=CarController.owner();}public void onProgressChanged(SeekBar s,int progress,boolean user){if(user)time.setText(clock(duration*progress/1000)+" / "+clock(duration));}public void onStopTrackingTouch(SeekBar s){dragging=false;if(s.isEnabled()&&gestureEpoch==CarController.epoch()&&gestureRequest==CarController.request()&&gestureOwner==CarController.owner())command.run("seek",duration*s.getProgress()/1000);}});
 }
 private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
 private Button button(Context c,String text,String label){Button b=new Button(c);b.setText(text);b.setTextColor(Color.WHITE);b.setTextSize(16);b.setContentDescription(label);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);return b;}
 static String clock(double seconds){long n=(long)Math.max(0,seconds);return n>=3600?String.format(java.util.Locale.ROOT,"%d:%02d:%02d",n/3600,n/60%60,n%60):String.format(java.util.Locale.ROOT,"%d:%02d",n/60,n%60);}
 void update(JSONObject state){double next=state==null?0:state.optDouble("duration",0);boolean enabled=state!=null&&state.optBoolean("seekable")&&Double.isFinite(next)&&next>0;seek.setEnabled(enabled);back.setEnabled(enabled);forward.setEnabled(enabled);if(!dragging){duration=enabled?next:0;double position=state==null?0:state.optDouble("time",0);seek.setProgress(duration>0?(int)(1000*position/duration):0);time.setText(enabled?clock(position)+" / "+clock(duration):"—");}}
}
