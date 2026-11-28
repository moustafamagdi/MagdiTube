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
 private final CarLaunchRequest launch;private final Button openCar;private final TextView launchStatus;
 private final LinearLayout controls;
 private String arrangement="";
 private long epoch=-1,request=-1,owner=-1,seekEpoch,seekRequest,seekOwner;
 private double seekDuration;
 private double duration;
 private boolean dragging;
 PhoneController(Context context,boolean arabic){super(context);ar=arabic;setOrientation(VERTICAL);setPadding(dp(12),dp(6),dp(12),dp(6));
  state=new TextView(context);state.setTextSize(12);state.setIncludeFontPadding(false);state.setTextColor(0xff45dfcc);addView(state);
  title=new TextView(context);title.setTextSize(14);title.setIncludeFontPadding(false);title.setTextColor(Color.WHITE);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);addView(title);
  seek=new SeekBar(context);seek.setMax(1000);addView(seek,new LayoutParams(-1,dp(40)));
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int n,boolean user){}public void onStartTrackingTouch(SeekBar b){dragging=true;seekEpoch=epoch;seekRequest=request;seekOwner=owner;seekDuration=duration;}public void onStopTrackingTouch(SeekBar b){dragging=false;command("seek",seekDuration*b.getProgress()/1000,seekEpoch,seekRequest,seekOwner);}});
  controls=new LinearLayout(context);controls.setOrientation(VERTICAL);controls.setPadding(0,dp(6),0,0);addView(controls);
  String[] actions={"play","pause","stop","rewind","forward","previous","next","retry","fullscreen","fill","open"};
  String[] labels=ar?new String[]{"تشغيل","إيقاف مؤقت","إيقاف","−10 ث","+10 ث","السابق","التالي","إعادة المحاولة","ملء شاشة العربية","بدون حواف","فتح على العربية"}:new String[]{"Play","Pause","Stop","−10s","+10s","Previous","Next","Retry","Car fullscreen","Fill screen","Open on car"};
  for(int i=0;i<actions.length;i++){String action=actions[i];Button b=new Button(context);b.setText(labels[i]);b.setTextSize(12);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);styleButton(b,action);b.setContentDescription(action.equals("fullscreen")?(ar?"ملء شاشة العربية":"Toggle car fullscreen"):labels[i]);long[] gesture={-1,-1,-1};b.setOnTouchListener((v,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){gesture[0]=epoch;gesture[1]=request;gesture[2]=owner;}if(event.getActionMasked()==android.view.MotionEvent.ACTION_CANCEL)gesture[0]=-1;return false;});b.setOnClickListener(v->{command(action,action.equals("fill")&&CarController.state().optJSONObject("playback")!=null&&!CarController.state().optJSONObject("playback").optBoolean("fill")?1:0,gesture[0]<0?epoch:gesture[0],gesture[0]<0?request:gesture[1],gesture[0]<0?owner:gesture[2]);gesture[0]=-1;});buttons.put(action,b);}
  arrange(dp(360));
  openCar=buttons.get("open");
  launchStatus=new TextView(context);launchStatus.setTextColor(0xffb6c9d8);launchStatus.setTextSize(12);addView(launchStatus);
  launch=new CarLaunchRequest(context,TubeService.class,this::updateLaunch);openCar.setOnClickListener(v->launch.request());updateLaunch();update();
 }
 // Match button edges rather than text baselines. Longer labels may grow the entire row.
 private void styleButton(Button b,String action){
  b.setGravity(android.view.Gravity.CENTER);b.setMinimumHeight(dp(48));b.setMinHeight(dp(48));
  b.setPadding(dp(6),dp(8),dp(6),dp(8));b.setTextColor(Color.WHITE);b.setElevation(0);b.setStateListAnimator(null);
  android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
  shape.setColor(action.equals("open")?0xff285ca0:action.equals("play")||action.equals("pause")?0xff176d66:0xff1b2a3c);
  shape.setCornerRadius(dp(10));shape.setStroke(dp(1),action.equals("open")?0xffa3ccff:0xff40536b);
  b.setBackgroundTintList(null);b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33ffffff),shape,null));
 }
 private void arrange(int width){
  float scale=Math.max(1,getResources().getConfiguration().fontScale);
  int available=Math.max(dp(100),width-getPaddingLeft()-getPaddingRight());
  int transport=Math.max(1,Math.min(5,(int)((available+dp(6))/(dp(48)*scale+dp(6)))));
  transport=(int)Math.ceil(5.0/Math.ceil(5.0/transport));
  int other=Math.max(1,Math.min(3,(int)((available+dp(6))/(dp(72)*scale+dp(6)))));
  boolean wide=available>=dp((int)(720*scale));
  String key=transport+":"+other+":"+wide;if(key.equals(arrangement))return;arrangement=key;
  for(Button b:buttons.values())if(b.getParent()!=null)((android.view.ViewGroup)b.getParent()).removeView(b);controls.removeAllViews();
  List<Button> list=new ArrayList<>(buttons.values());
  addRows(list,0,5,transport);
  if(wide)addRows(list,5,11,6);else{addRows(list,5,8,other);addRows(list,8,11,other);}
 }
 private void addRows(List<Button> list,int start,int end,int columns){
  for(int offset=start;offset<end;offset+=columns){
   LinearLayout row=new LinearLayout(getContext());row.setBaselineAligned(false);row.setGravity(android.view.Gravity.CENTER_VERTICAL);
   LayoutParams rowParams=new LayoutParams(-1,-2);if(controls.getChildCount()>0)rowParams.topMargin=dp(6);controls.addView(row,rowParams);
   for(int i=offset;i<Math.min(end,offset+columns);i++){
    LayoutParams params=new LayoutParams(0,-1,1);if(i>offset)params.setMarginStart(dp(6));row.addView(list.get(i),params);
   }
  }
 }
 @Override protected void onMeasure(int widthSpec,int heightSpec){arrange(MeasureSpec.getSize(widthSpec));super.onMeasure(widthSpec,heightSpec);}
 void unavailable(){Toast.makeText(getContext(),ar?"مشغّل العربية غير جاهز. حاولنا فتحه؛ اختر الفيديو مجددًا بعد ظهوره.":"Car player is not ready. Opening was requested; select the video again when ready.",Toast.LENGTH_LONG).show();}
 void select(String url,long expected){if(VideoSelection.parse(url)==null)return;if(CarController.select(url,expected)){launch.cancel();launch.request();}else launch.request(()->CarController.select(url,CarController.epoch()));update();}
 private void command(String action,double value,long e,long r,long o){if("open".equals(action)){launch.request();return;}if("stop".equals(action))launch.cancel();if(!CarController.command(action,value,e,r,o)){if(!CarController.available())unavailable();else Toast.makeText(getContext(),ar?"الأمر غير متاح في المشغّل الحالي":"Command unavailable in the current player",Toast.LENGTH_SHORT).show();}update();}
 void update(){updateLaunch();JSONObject s=CarController.state(),p=s.optJSONObject("playback");epoch=s.optLong("epoch");request=s.optLong("request");owner=s.optLong("owner");boolean connected=s.optBoolean("connected"),video=p!=null&&p.optBoolean("hasVideo")&&p.optInt("ready")>0;
  String phase=s.optString("phase");String label;
  switch(phase){case "stale":label=ar?"جارٍ انتظار حالة مشغّل العربية…":"Waiting for car player status…";break;case "buffering":label=ar?"جارٍ التخزين المؤقت…":"Car connected • Buffering";break;case "ended":label=ar?"انتهى الفيديو":"Car connected • Ended";break;case "sending":label=ar?"جارٍ الإرسال للعربية…":"Sending to car…";break;case "loading":label=ar?"جارٍ التحميل على العربية…":"Loading on car…";break;case "playing":label=ar?"العربية متصلة • يعمل":"Car connected • Playing";break;case "paused":label=ar?"العربية متصلة • متوقف مؤقتًا":"Car connected • Paused";break;case "error":label=ar?"تعذر بدء الفيديو. افحص صفحة العربية وتسجيل الدخول ثم أعد المحاولة.":"Could not start video. Check car page/sign-in, then Retry.";break;default:label=connected?(ar?"مشغّل العربية جاهز":"Car player ready"):(ar?"وصّل Android Auto وافتح MM Tube على العربية":"Connect Android Auto and open MM Tube on the car");}
  buttons.get("fill").setText(p!=null&&p.optBoolean("fill")?(ar?"الصورة كاملة":"Full picture"):(ar?"بدون حواف":"Fill screen"));
  buttons.get("fill").setContentDescription(ar?"ملء الشاشة مع قص الأطراف أو عرض الصورة كاملة":"Fill screen (crop edges) or show full picture");
  double position=p==null?0:p.optDouble("time",0);duration=p==null?0:p.optDouble("duration",0);
  state.setText(label+(video?" • "+clock(position)+" / "+clock(duration):""));title.setText(p==null?"":p.optString("title"));title.setVisibility(title.length()==0?GONE:VISIBLE);seek.setVisibility(video?VISIBLE:GONE);
  seek.setEnabled(CarController.can("seek")&&duration>0);if(!dragging)seek.setProgress(duration>0?(int)(position*1000/duration):0);
  for(Map.Entry<String,Button> entry:buttons.entrySet()){String a=entry.getKey();boolean enabled=a.equals("open")?!launch.pending():a.equals("stop")?CarController.canStop():a.equals("retry")?connected&&s.optBoolean("retry"):CarController.can(a);entry.getValue().setEnabled(enabled);entry.getValue().setAlpha(enabled?1f:0.38f);}

 }
 void cancelLaunch(){launch.cancel();}
 private void updateLaunch(){openCar.setEnabled(!launch.pending());String text;
  switch(launch.status()){
   case "opening":text=ar?"جارٍ طلب فتح شاشة العربية…":"Requesting car screen…";break;
   case "opened":text=ar?"التطبيق يبلغ أن واجهته مفتوحة. لو الكاميرا ظاهرة، ارجع لشاشة Android Auto.":"The app reports its window open. If the camera is showing, return to Android Auto.";break;
   case "denied":text=ar?"Android Auto لم يسمح بالفتح. افتح MM Tube من شاشة العربية.":"Android Auto denied opening. Open MM Tube on the car.";break;
   case "timeout":case "unavailable":text=ar?"لم نتمكن من تأكيد الظهور. لو الكاميرا ظاهرة، اقفلها وارجع لـAndroid Auto ثم حاول تاني.":"Could not confirm visibility. If the camera is showing, close it, return to Android Auto and try again.";break;
   default:text="";
  }launchStatus.setText(text);launchStatus.setVisibility(text.isEmpty()?GONE:VISIBLE);
 }
 private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
 private String clock(double seconds){if(!Double.isFinite(seconds)||seconds<0)return "--:--";return String.format(Locale.ROOT,"%d:%02d",(long)seconds/60,(long)seconds%60);}
}
