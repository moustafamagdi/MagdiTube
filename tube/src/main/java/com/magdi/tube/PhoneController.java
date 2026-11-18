package com.magdi.tube;

import android.content.Context;
import android.graphics.Color;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

/** Native controls remain available even if the browsing page fails or needs sign-in. */
final class PhoneController extends LinearLayout {
 private final TextView state,title;
 private final SeekBar seek;
 private final Map<String,Button> buttons=new LinkedHashMap<>();
 private final boolean ar;
 private long epoch=-1,request=-1,owner=-1,seekEpoch,seekRequest,seekOwner;
 private double seekDuration;
 private double duration;
 private boolean dragging;
 PhoneController(Context context,boolean arabic){super(context);ar=arabic;setOrientation(VERTICAL);setPadding(dp(12),dp(6),dp(12),dp(6));
  state=new TextView(context);state.setTextColor(0xff45dfcc);addView(state);
  title=new TextView(context);title.setTextColor(Color.WHITE);title.setMaxLines(2);addView(title);
  seek=new SeekBar(context);seek.setMax(1000);addView(seek,new LayoutParams(-1,dp(40)));
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){}public void onStartTrackingTouch(SeekBar b){dragging=true;seekEpoch=epoch;seekRequest=request;seekOwner=owner;seekDuration=duration;}public void onStopTrackingTouch(SeekBar b){dragging=false;command("seek",seekDuration*b.getProgress()/1000,seekEpoch,seekRequest,seekOwner);}});
  HorizontalScrollView scroller=new HorizontalScrollView(context);LinearLayout row=new LinearLayout(context);scroller.addView(row);addView(scroller);
  String[] actions={"play","pause","stop","previous","rewind","forward","next","retry","fullscreen"};
  String[] labels=ar?new String[]{"تشغيل","إيقاف مؤقت","إيقاف","السابق","−10 ث","+10 ث","التالي","إعادة المحاولة","ملء شاشة العربية"}:new String[]{"Play","Pause","Stop","Previous","−10s","+10s","Next","Retry","Car fullscreen"};
  for(int i=0;i<actions.length;i++){String action=actions[i];Button b=new Button(context);b.setText(labels[i]);b.setTextSize(12);b.setAllCaps(false);long[] gesture={-1,-1,-1};b.setOnTouchListener((v,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){gesture[0]=epoch;gesture[1]=request;gesture[2]=owner;}if(event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)gesture[0]=-1;return false;});b.setOnClickListener(v->{command(action,0,gesture[0]<0?epoch:gesture[0],gesture[0]<0?request:gesture[1],gesture[0]<0?owner:gesture[2]);gesture[0]=-1;});row.addView(b,new LayoutParams(-2,dp(48)));buttons.put(action,b);}
  update();
 }
 void unavailable(){Toast.makeText(getContext(),ar?"وصّل Android Auto وافتح MM Tube على شاشة العربية. الاختيار لم يُحفظ.":"Connect Android Auto and open MM Tube on the car. Selection was not saved.",Toast.LENGTH_LONG).show();}
 void select(String url,long expected){if(!CarController.select(url,expected))unavailable();update();}
 private void command(String action,double value,long e,long r,long o){if(!CarController.command(action,value,e,r,o)){if(!CarController.available())unavailable();else Toast.makeText(getContext(),ar?"الأمر غير متاح في المشغّل الحالي":"Command unavailable in the current player",Toast.LENGTH_SHORT).show();}update();}
 void update(){JSONObject s=CarController.state(),p=s.optJSONObject("playback");epoch=s.optLong("epoch");request=s.optLong("request");owner=s.optLong("owner");boolean connected=s.optBoolean("connected"),video=p!=null&&p.optBoolean("hasVideo")&&p.optInt("ready")>0;
  String phase=s.optString("phase");String label;
  switch(phase){case "stale":label=ar?"جارٍ انتظار حالة مشغّل العربية…":"Waiting for car player status…";break;case "buffering":label=ar?"جارٍ التخزين المؤقت…":"Car connected • Buffering";break;case "ended":label=ar?"انتهى الفيديو":"Car connected • Ended";break;case "sending":label=ar?"جارٍ الإرسال للعربية…":"Sending to car…";break;case "loading":label=ar?"جارٍ التحميل على العربية…":"Loading on car…";break;case "playing":label=ar?"العربية متصلة • يعمل":"Car connected • Playing";break;case "paused":label=ar?"العربية متصلة • متوقف مؤقتًا":"Car connected • Paused";break;case "error":label=ar?"تعذر بدء الفيديو. افحص صفحة العربية وتسجيل الدخول ثم أعد المحاولة.":"Could not start video. Check car page/sign-in, then Retry.";break;default:label=connected?(ar?"مشغّل العربية جاهز":"Car player ready"):(ar?"وصّل Android Auto وافتح MM Tube على العربية":"Connect Android Auto and open MM Tube on the car");}
  double position=p==null?0:p.optDouble("time",0);duration=p==null?0:p.optDouble("duration",0);
  state.setText(label+(video?" • "+clock(position)+" / "+clock(duration):""));title.setText(p==null?"":p.optString("title"));
  seek.setEnabled(CarController.can("seek")&&duration>0);if(!dragging)seek.setProgress(duration>0?(int)(position*1000/duration):0);
  for(Map.Entry<String,Button> entry:buttons.entrySet()){String a=entry.getKey();boolean enabled=a.equals("stop")?connected:a.equals("retry")?connected&&s.optBoolean("retry"):CarController.can(a);entry.getValue().setEnabled(enabled);entry.getValue().setAlpha(enabled?1f:0.38f);}

 }
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
 private String clock(double seconds){if(!Double.isFinite(seconds)||seconds<0)return "--:--";return String.format(Locale.ROOT,"%d:%02d",(long)seconds/60,(long)seconds%60);}
}
