package com.magdi.tube;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.*;
public final class TubeView extends LinearLayout implements CarController.Player {
 private final WebView web;
 private PhoneController controller;
 private boolean phoneSafe,documentReady,controllerFullscreen;
 private long selectionEpoch,selectionRequest,phoneSequence;
 private String selectedId="",selectionToken="";
 private final Runnable controllerPoll=new Runnable(){public void run(){if(closed||!active||!phone)return;controller.update();web.evaluateJavascript("window.__magdiPhoneEpoch="+CarController.epoch(),null);timer.postDelayed(this,500);}};

 private final FrameLayout content;
 private final TextView status;
 private final ProgressBar progress;
 private final Button back;
 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
 private boolean active, closed, background, phoneSuppressed, sampleInFlight, visualStatePending;
 private static int nextInstance;
 private final int instance=++nextInstance;
 private long sampleGeneration,sampleDue,pictureWindowEnd,fullscreenWindowEnd,resumeCommandGeneration;
 private int pictureRefreshCount;
 private boolean interruption, pendingResume;
 private long detachedIdleSince;
 private FrameLayout fullscreen;
 private WebChromeClient.CustomViewCallback fullscreenCallback;
 private final java.util.Map<View,Integer> hiddenViews=new java.util.HashMap<>();
 private View searchPanel;
 private long lastVideoLog;
 private final Runnable refreshPicture=()->refreshVideoSurface();
 private void schedulePictureRefresh(){
  if(closed||!active)return;
  long now=android.os.SystemClock.uptimeMillis();if(now<pictureWindowEnd)return;pictureWindowEnd=now+1200;
  timer.post(refreshPicture);timer.postDelayed(refreshPicture,300);timer.postDelayed(refreshPicture,1100);
 }
 private final Runnable fullscreenReturn=()->{if(!closed&&active&&fullscreen!=null)tryResumePlayback();};
 private void scheduleFullscreenReturn(){if(closed||!active)return;schedulePictureRefresh();long now=android.os.SystemClock.uptimeMillis();if(now<fullscreenWindowEnd)return;fullscreenWindowEnd=now+1200;timer.postDelayed(fullscreenReturn,300);timer.postDelayed(fullscreenReturn,1100);}
 private void cancelPictureRefresh(){timer.removeCallbacks(refreshPicture);timer.removeCallbacks(fullscreenReturn);pictureWindowEnd=0;fullscreenWindowEnd=0;}
 private void invalidateSample(){sampleGeneration++;sampleInFlight=false;}
 @Override public void onWindowFocusChanged(boolean focused){
  if(!focused&&web!=null&&fullscreen!=null&&background)coverPlayback();
  super.onWindowFocusChanged(focused);
  if(focused&&web!=null){schedulePictureRefresh();if(fullscreen!=null)scheduleFullscreenReturn();}
 }
 @Override protected void onWindowVisibilityChanged(int visibility){
  if(visibility!=VISIBLE&&web!=null&&fullscreen!=null&&background)coverPlayback();
  super.onWindowVisibilityChanged(visibility);
  if(visibility==VISIBLE&&web!=null&&fullscreen!=null)scheduleFullscreenReturn();
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(web!=null)schedulePictureRefresh();}
 private void refreshVideoSurface(){
  if(closed||!active||accountPage||searchPanel!=null||!isAttachedToWindow()||!isShown())return;
  pictureRefreshCount++;web.onResume();
  View target=fullscreen!=null?fullscreen:web;
  target.requestFocus();target.requestLayout();target.postInvalidateOnAnimation();
  requestLayout();postInvalidateOnAnimation();
  web.evaluateJavascript("(function(){window.dispatchEvent(new Event('resize'));requestAnimationFrame(()=>requestAnimationFrame(()=>{window.__magdiPictureRefresh=(window.__magdiPictureRefresh||0)+1;}));var v=document.querySelector('video'),r=v?v.getBoundingClientRect():null;return JSON.stringify({hasVideo:!!v,paused:v?v.paused:null,time:v?Math.round(v.currentTime):0,ready:v?v.readyState:0,hidden:document.hidden,viewportWidth:innerWidth,viewportHeight:innerHeight,videoBoxWidth:r?Math.round(r.width):0,videoBoxHeight:r?Math.round(r.height):0,documentFullscreen:!!document.fullscreenElement});})()",value->{if(!closed)Diagnostics.event("Picture refresh native="+getWidth()+"x"+getHeight()+" web="+web.getWidth()+"x"+web.getHeight()+" custom="+(fullscreen!=null)+" page="+value);});
  if(!visualStatePending){visualStatePending=true;web.postVisualStateCallback(android.os.SystemClock.uptimeMillis(),new WebView.VisualStateCallback(){public void onComplete(long id){visualStatePending=false;if(!closed&&active){web.postInvalidateOnAnimation();if(fullscreen!=null)fullscreen.postInvalidateOnAnimation();}}});}
 }

 private final Runnable sample=new Runnable(){public void run(){
  if(closed||phone||!active||phoneSuppressed)return;
  long started=android.os.SystemClock.uptimeMillis();long late=sampleDue==0?0:Math.max(0,started-sampleDue);
  android.media.AudioManager am=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
  boolean call=am.getMode()!=android.media.AudioManager.MODE_NORMAL;
  if(call&&!interruption){interruption=true;coverPlayback();Diagnostics.event("Audio mode interruption started");}
  else if(!call&&interruption){interruption=false;tryResumePlayback();Diagnostics.event("Audio mode interruption ended");}
  if(!accountPage&&!sampleInFlight){
   sampleInFlight=true;final long request=++sampleGeneration;final long e=CarController.epoch(),r=CarController.request();
   boolean detailed=android.os.SystemClock.elapsedRealtime()-lastVideoLog>15000;
   web.evaluateJavascript("window.__magdiSample?window.__magdiSample("+detailed+","+MediaCardService.wantsArtwork(TubeView.this)+"):null",value->{
    if(closed||request!=sampleGeneration||!CarController.accepts(TubeView.this,e,r))return;sampleInFlight=false;
    try{String json=new org.json.JSONArray("["+value+"]").getString(0);org.json.JSONObject state=new org.json.JSONObject(json);
     org.json.JSONObject control=state.optJSONObject("controller");if(control!=null)CarController.report(TubeView.this,e,r,control);state.remove("controller");
     if(!state.optBoolean("resumeWanted"))pendingResume=false;
     MediaCardService.observe(TubeView.this,state.optBoolean("playing"),state.optBoolean("muted"),state.optInt("ready")>0,state.optLong("time")*1000,ThumbnailCache.Art.parse(state.optJSONObject("artwork")));
     state.remove("artwork"); // Never include thumbnail URLs or video IDs in diagnostics.
     if(state.optBoolean("playing")&&background)PlaybackService.keep(TubeView.this);else if(!pendingResume&&!interruption)PlaybackService.idle(TubeView.this);
     PlaybackCoordinator.observe(TubeView.this,state.optBoolean("playing")&&!state.optBoolean("muted")&&state.optInt("ready")>0);
     if(phoneSuppressed)return;
     if(!active&&getParent()==null&&!state.optBoolean("playing")&&!MediaCardService.retains(TubeView.this)){if(detachedIdleSince==0)detachedIdleSince=android.os.SystemClock.elapsedRealtime();if(android.os.SystemClock.elapsedRealtime()-detachedIdleSince>300000)CarPlaybackSession.stopDetached();}else detachedIdleSince=0;
     if(detailed){state.put("sampleDelayMs",late);state.put("callbackMs",android.os.SystemClock.uptimeMillis()-started);state.put("fullscreen",fullscreen!=null);state.put("webWidth",web.getWidth());state.put("webHeight",web.getHeight());state.put("viewWidth",getWidth());state.put("viewHeight",getHeight());state.put("pictureRefreshes",pictureRefreshCount);pictureRefreshCount=0;
      Runtime runtime=Runtime.getRuntime();state.put("appJavaHeapMiB",(runtime.totalMemory()-runtime.freeMemory())/1048576);
      Diagnostics.event("Playback source="+(phone?"phone":"car")+" instance="+instance+" visible="+active+" "+state);lastVideoLog=android.os.SystemClock.elapsedRealtime();}
    }catch(org.json.JSONException ignored){}
   });
  }
  sampleDue=android.os.SystemClock.uptimeMillis()+2000;timer.postDelayed(this,2000);
 }};
 private final boolean ar; private boolean phone, accountPage;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private Button button(String text,String description){Button b=new Button(getContext());b.setText(text);b.setTextColor(0xffe6edf5);b.setTextSize(14);b.setAllCaps(false);b.setContentDescription(description);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff1b2a3c));return b;}
 @SuppressLint("SetJavaScriptEnabled") public TubeView(Context c){
 super(c);background=c.getSharedPreferences("tube",0).getBoolean("backgroundAudio",true);phone=c instanceof android.app.Activity;setOrientation(VERTICAL);setBackgroundColor(0xff0b1220);
 Diagnostics.event(c instanceof android.app.Activity?"Phone view created":"Car view created");
 ar=c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
 LinearLayout bar=new LinearLayout(c);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),0,dp(8),0);
 ImageView logo=new ImageView(c);logo.setImageResource(R.drawable.ic_tube);logo.setContentDescription("Magdi Tube");bar.addView(logo,new LinearLayout.LayoutParams(dp(32),dp(32)));
 TextView title=new TextView(c);title.setText("Magdi Tube");title.setTextColor(Color.WHITE);title.setTextSize(16);title.setPadding(dp(8),0,0,0);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 back=button(ar?"رجوع":"Back",ar?"الصفحة السابقة":"Previous page");
 Button home=button(ar?"الرئيسية":"Home",ar?"رئيسية YouTube":"YouTube home");
 Button reload=button(ar?"تحديث":"Reload",ar?"إعادة تحميل الصفحة":"Reload page");
 for(Button b:new Button[]{back,home,reload}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.setMarginStart(dp(4));bar.addView(b,p);}
 
 addView(bar,new LinearLayout.LayoutParams(-1,dp(52)));
 progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff45dfcc));addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
 status=new TextView(c);status.setTextColor(0xffffcf8a);status.setPadding(dp(12),dp(6),dp(12),dp(6));setContentVisibility(status,GONE);addView(status);
 web=new WebView(c){@Override protected void onWindowVisibilityChanged(int visibility){
 // Preserve the existing media document during a temporary host cover (for example reverse camera).
 if(visibility!=View.VISIBLE&&background&&!closed)coverPlayback();
 super.onWindowVisibilityChanged(background&&!closed&&!accountPage&&!phoneSuppressed?View.VISIBLE:visibility);
 }};if(phone)phoneSafe=ControllerCookies.prepare(web);web.setBackgroundColor(Color.BLACK);content=new FrameLayout(c);content.addView(web,new FrameLayout.LayoutParams(-1,-1));addView(content,new LinearLayout.LayoutParams(-1,0,1));
 CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
 web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
 web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
 web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.getSettings().setMediaPlaybackRequiresUserGesture(phone);
 web.setWebChromeClient(new WebChromeClient(){
 public void onShowCustomView(View view,CustomViewCallback callback){if(phone){callback.onCustomViewHidden();return;}showFullscreen(view,callback);}
 public void onHideCustomView(){hideFullscreen();}
 public void onProgressChanged(WebView v,int value){progress.setProgress(value);setContentVisibility(progress,value==100?INVISIBLE:VISIBLE);}});
 web.setWebViewClient(new WebViewClient(){
 public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return phone?(phoneSafe?PhoneMediaPolicy.intercept(v,r):PhoneMediaPolicy.denied()):null;}
 public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
 Uri uri=r.getUrl();if(!phone&&r.isForMainFrame()&&!selectionToken.isEmpty()&&VideoSelection.parse(uri.toString())!=null&&!uri.toString().endsWith("#magdi_request="+selectionToken)){v.loadUrl(uri.buildUpon().fragment("magdi_request="+selectionToken).build().toString());return true;}if(phone&&r.isForMainFrame()&&VideoSelection.parse(uri.toString())!=null){controller.select(uri.toString(),CarController.epoch());return true;}String host=uri.getHost();boolean auth=GoogleAccountNavigation.allowed(uri);
 if(auth&&!phone){Toast.makeText(c,ar?"سجّل الدخول من Magdi Tube على الموبايل ثم اضغط تحديث":"Sign in using Magdi Tube on your phone, then Reload",Toast.LENGTH_LONG).show();Diagnostics.event("Phone sign-in required");return true;}
 boolean block=!allowed(uri)&&!(phone&&auth);
 if(block&&r.isForMainFrame()){String scheme=uri.getScheme();String target=(scheme==null?"none":scheme)+"://"+(host==null?"none":host);target=target.replaceAll("[^a-zA-Z0-9.:/_-]","?");if(target.length()>160)target=target.substring(0,160);Diagnostics.event("Blocked navigation target="+target);status.setText((ar?"رابط غير مدعوم: ":"Unsupported link: ")+target+(ar?" — جرّب اشتراكاتي أو صدّر اللوج":" — try Subscriptions or export log"));setContentVisibility(status,VISIBLE);CookiePersistence.request();}return block;
 } public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){invalidateSample();phoneSequence=0;accountPage=!allowed(Uri.parse(u));MediaCardService.clearArtwork(TubeView.this);if(accountPage)PlaybackService.release(TubeView.this);setContentVisibility(status,GONE);Diagnostics.event(accountPage?"Account page started":"Page load started");}
 public void onPageFinished(WebView v,String u){
  if(closed)return;back.setEnabled(v.canGoBack()||fullscreen!=null);CookiePersistence.request();
  if(phone){web.evaluateJavascript("window.__magdiPhoneEpoch="+CarController.epoch(),null);Diagnostics.event("Controller browsing loaded; car authentication not verified");return;}
  if(!active||"about:blank".equals(u)||!u.equals(v.getUrl()))return;
  if(!accountPage){documentReady=true;CarController.ready(TubeView.this);AdAssist.apply(v);
   try{for(String asset:new String[]{"playback_state.js","media_artwork.js","media_transport.js","car_controller.js","playback_sample.js"})v.evaluateJavascript(AdAssist.asset(c,asset),null);}catch(java.io.IOException e){Diagnostics.event("Playback tracking unavailable");}
   if(!selectionToken.isEmpty()&&u.endsWith("#magdi_request="+selectionToken)&&CarController.accepts(TubeView.this,selectionEpoch,selectionRequest)){nativeMute(false);v.evaluateJavascript("window.__magdiCar&&window.__magdiCar.start("+org.json.JSONObject.quote(selectedId)+","+org.json.JSONObject.quote(selectionToken)+")",null);}
   if(selectionToken.isEmpty())nativeMute(false);
   Diagnostics.event("Car page loaded; playback readiness remains observed");
  }
 }
 public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){Diagnostics.event("Main frame error code="+e.getErrorCode());status.setText(ar?"تعذر الاتصال. تحقق من الإنترنت واضغط تحديث.":"Unable to connect. Check your internet and tap Reload.");setContentVisibility(status,VISIBLE);}}
 });
 back.setEnabled(false);back.setOnClickListener(v->handleBack());
 home.setOnClickListener(v->web.loadUrl("https://m.youtube.com/"));reload.setOnClickListener(v->{AdAssist.configure(web);web.reload();});
 reload.setContentDescription(ar?"تحديث؛ اضغط مطولًا لإعادة تهيئة الشاشة وفتح الرئيسية":"Reload; hold to reset the screen and open Home");
 reload.setOnLongClickListener(v->{if(phone){Diagnostics.event("Phone view reset requested by user");((android.app.Activity)c).recreate();}else if(!CarPlaybackSession.reset(this))return false;Toast.makeText(c,ar?"تمت إعادة تهيئة الشاشة؛ افتح الفيديو من جديد":"Screen reset; open your video again",Toast.LENGTH_LONG).show();return true;});
 Button search=button(ar?"بحث":"Search",ar?"البحث في YouTube":"Search YouTube");
 bar.addView(search,new LinearLayout.LayoutParams(-2,dp(48)));search.setOnClickListener(v->showSearch());
 if(!phone){
 Button carAds=button(AdAssist.enabled(c)?(ar?"حجب: يعمل":"Block: ON"):(ar?"حجب: متوقف":"Block: OFF"),ar?"تشغيل أو إيقاف منع الإعلانات":"Toggle ad blocking");
 bar.addView(carAds,new LinearLayout.LayoutParams(-2,dp(48)));
 carAds.setOnClickListener(v->{AdAssist.toggle(c);carAds.setText(AdAssist.enabled(c)?(ar?"حجب: يعمل":"Block: ON"):(ar?"حجب: متوقف":"Block: OFF"));AdAssist.configure(web);AdAssist.apply(web);web.reload();});
 }
 if(phone){
 LinearLayout actions=new LinearLayout(c);actions.setGravity(Gravity.CENTER_VERTICAL);
 Button account=button(ar?"الحساب":"Account",ar?"تسجيل YouTube التجريبي":"Experimental YouTube sign-in");
 Button subscriptions=button(ar?"اشتراكاتي":"Subscriptions",ar?"قنواتي المشترَك بها":"Subscribed channels");
 Button ads=button(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ad blocker: ON"):(ar?"منع الإعلانات: متوقف":"Ad blocker: OFF"),ar?"منع الإعلانات التجريبي":"Experimental ad blocking");
 Button audio=button(ar?"صوت الخلفية":"Background audio",ar?"تشغيل أو إيقاف صوت الخلفية":"Toggle background audio");
 audio.setOnClickListener(v->{background=!background;c.getSharedPreferences("tube",0).edit().putBoolean("backgroundAudio",background).apply();if(!background)PlaybackService.release(this);Toast.makeText(c,background?(ar?"صوت الخلفية يعمل":"Background audio ON"):(ar?"صوت الخلفية متوقف":"Background audio OFF"),Toast.LENGTH_SHORT).show();});
 Button logs=button(ar?"جلسات اللوج":"Log sessions",ar?"نسخ أو تصدير جلسة اللوج":"Copy or export a log session");
 Button setup=button(ar?"إعداد Android Auto":"Android Auto setup",ar?"فحص وإعداد مصدر التثبيت":"Check and configure installation source");
 setup.setOnClickListener(v->c.startActivity(new android.content.Intent(c,InstallSetupActivity.class)));
 for(Button b:new Button[]{setup,account,subscriptions,ads,audio,logs})actions.addView(b,new LinearLayout.LayoutParams(-2,dp(48)));
 HorizontalScrollView scroller=new HorizontalScrollView(c);scroller.addView(actions);addView(scroller,1,new LinearLayout.LayoutParams(-1,dp(50)));
 account.setOnClickListener(v->new android.app.AlertDialog.Builder(c).setTitle(ar?"تسجيل دخول تجريبي":"Experimental sign-in").setMessage(ar?"تدخل بياناتك في صفحة Google. قد ترفض Google المتصفح المضمّن. لا نغيّر هوية المتصفح ولا نقرأ كلمات المرور. بعد النجاح افتح الاشتراكات، ثم تحديث على شاشة السيارة. لا تمسح بيانات التطبيق إن أردت الاحتفاظ بالجلسة.":"Enter your credentials only on Google's page. Google may reject this embedded browser. We do not change browser identity or read passwords. After success, check Subscriptions, then Reload on the car. Clearing app data removes the session.").setNegativeButton(ar?"إلغاء":"Cancel",null).setPositiveButton(ar?"متابعة":"Continue",(d,w)->{Diagnostics.event("Account flow opened on phone");web.loadUrl("https://m.youtube.com/signin");}).show());
 subscriptions.setOnClickListener(v->web.loadUrl("https://m.youtube.com/feed/subscriptions"));
 ads.setOnClickListener(v->{AdAssist.toggle(c);AdAssist.configure(web);ads.setText(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ad blocker: ON"):(ar?"منع الإعلانات: متوقف":"Ad blocker: OFF"));if(!accountPage){AdAssist.apply(web);web.reload();}Toast.makeText(c,ar?"حدّث صفحة السيارة لتطبيق التغيير. لا يضمن منع جميع الإعلانات.":"Reload the car page to apply. Experimental; complete blocking is not guaranteed.",Toast.LENGTH_LONG).show();});
 logs.setOnClickListener(v->Diagnostics.open((android.app.Activity)c));
 }
 if(phone){
  controller=new PhoneController(c,ar);addView(controller,2,new LinearLayout.LayoutParams(-1,-2));
  phoneSafe=phoneSafe&&PhoneMediaPolicy.install(web)&&androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.WEB_MESSAGE_LISTENER);
  if(phoneSafe)androidx.webkit.WebViewCompat.addWebMessageListener(web,"MagdiController",new java.util.HashSet<>(java.util.Arrays.asList("https://m.youtube.com","https://www.youtube.com","https://youtube.com")),(view,message,origin,main,reply)->{
   if(!main||closed||!active)return;try{String data=message.getData();if(data==null||data.length()>5000)return;org.json.JSONObject packet=new org.json.JSONObject(data);long seq=packet.optLong("sequence");if(seq<=phoneSequence)return;phoneSequence=seq;controller.select(packet.optString("url"),packet.optLong("epoch",-1));}catch(org.json.JSONException ignored){}
  });
  if(!phoneSafe){web.getSettings().setJavaScriptEnabled(false);status.setText(ar?"حدّث Android System WebView لتفعيل التصفح بدون تشغيل محلي.":"Update Android System WebView to enable controller browsing.");setContentVisibility(status,VISIBLE);}
 }else AdAssist.configure(web);
 if(!phone||phoneSafe)web.loadUrl("https://m.youtube.com/");
 }
 static boolean allowed(Uri u){String h=u.getHost();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&h!=null&&(h.equals("youtube.com")||h.endsWith(".youtube.com")||h.equals("youtu.be"));}

 void pause(){cancelPictureRefresh();active=false;timer.removeCallbacks(controllerPoll);timer.removeCallbacks(sample);invalidateSample();CookiePersistence.request();if(!phone){CarController.detach(this);documentReady=false;}web.onPause();Diagnostics.event(phone?"Phone controller covered":"Car player unavailable; playback stopped");}
 void resume(){if(closed)return;background=getContext().getSharedPreferences("tube",0).getBoolean("backgroundAudio",true);active=true;web.onResume();invalidateSample();sampleDue=0;timer.removeCallbacks(sample);
  if(phone){timer.removeCallbacks(controllerPoll);timer.post(controllerPoll);}else{CarController.attach(this);if(documentReady)CarController.ready(this);if(!documentReady||"about:blank".equals(web.getUrl()))web.loadUrl("https://m.youtube.com/");timer.post(sample);}
  Diagnostics.event("View resumed");schedulePictureRefresh();
 }
 public boolean isCarView(){return !phone;}
 boolean carAvailable(){return !phone&&active&&!closed&&CarController.accepts(this,CarController.epoch(),CarController.request());}
 private void nativeMute(boolean muted){if(androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MUTE_AUDIO))androidx.webkit.WebViewCompat.setAudioMuted(web,muted);}
 public void cancelController(){selectionToken="";selectedId="";invalidateSample();pendingResume=false;resumeCommandGeneration++;nativeMute(true);web.stopLoading();pauseMedia();hideFullscreen();hideControllerFullscreen();web.loadUrl("about:blank");MediaCardService.forget(this);PlaybackService.release(this);}
 public void selectController(VideoSelection video,long epoch,long request){
  cancelController();selectionEpoch=epoch;selectionRequest=request;selectedId=video.id;selectionToken=epoch+"_"+request;if(!ControllerCookies.toCar()){CarController.failed(this,epoch,request);return;}CookiePersistence.request();
  timer.postDelayed(()->{if(!CarController.accepts(this,epoch,request)||closed||!active)return;CarController.loading(this,epoch,request);web.loadUrl(video.url+"#magdi_request="+selectionToken);},150);
  timer.postDelayed(()->{if(!CarController.accepts(this,epoch,request))return;String phase=CarController.state().optString("phase");if(phase.equals("sending")||phase.equals("loading"))CarController.failed(this,epoch,request);},30000);
 }
 public void commandController(String action,double value,long epoch,long request){if(!carAvailable()||!CarController.accepts(this,epoch,request))return;if("fullscreen".equals(action)){toggleControllerFullscreen();return;}if("pause".equals(action))pendingResume=false;web.evaluateJavascript("window.__magdiCar&&window.__magdiCar.command("+org.json.JSONObject.quote(action)+","+value+","+org.json.JSONObject.quote(selectionToken)+")",null);}
 private void toggleControllerFullscreen(){
  if(fullscreen!=null){hideFullscreen();return;}if(controllerFullscreen){hideControllerFullscreen();return;}controllerFullscreen=true;hideChildren();web.setVisibility(VISIBLE);
  web.evaluateJavascript("(()=>{let s=document.createElement('style');s.id='magdi-car-fullscreen';s.textContent='html,body{overflow:hidden!important} #player-container-id,#player-container,#player,.html5-video-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;z-index:2147483646!important} video{width:100%!important;height:100%!important;object-fit:contain!important}';document.head.appendChild(s);window.dispatchEvent(new Event('resize'));})()",null);
  Button exit=button(ar?"خروج":"Exit","Exit car fullscreen");exit.setTag("controller-fullscreen-exit");content.addView(exit,new FrameLayout.LayoutParams(-2,dp(48),Gravity.TOP|Gravity.LEFT));exit.setOnClickListener(v->hideControllerFullscreen());
 }
 private void hideControllerFullscreen(){if(!controllerFullscreen)return;controllerFullscreen=false;View exit=content.findViewWithTag("controller-fullscreen-exit");if(exit!=null)content.removeView(exit);web.evaluateJavascript("document.getElementById('magdi-car-fullscreen')?.remove();window.dispatchEvent(new Event('resize'))",null);restoreChildren();}
 void suppressHiddenPhone(){
  if(!phone||active||closed||phoneSuppressed||accountPage)return;phoneSuppressed=true;cancelPictureRefresh();invalidateSample();timer.removeCallbacks(sample);pauseMedia();
  web.evaluateJavascript("if(window.__magdiPlayback)window.__magdiPlayback.suppress(true)",null);web.onPause();Diagnostics.event("Hidden phone playback suspended while car audio plays; instance="+instance);
 }
 private void releasePhoneSuppression(){if(!phoneSuppressed)return;phoneSuppressed=false;pendingResume=false;web.evaluateJavascript("if(window.__magdiPlayback)window.__magdiPlayback.suppress(false)",null);Diagnostics.event("Phone playback available again; automatic resume disabled; instance="+instance);}

 void pauseMedia(){resumeCommandGeneration++;PlaybackCoordinator.observe(this,false);pendingResume=false;web.evaluateJavascript("if(window.__magdiPlayback)window.__magdiPlayback.stop()",null);web.evaluateJavascript("document.querySelectorAll('video,audio').forEach(v=>v.pause())",null);MediaCardService.paused(this);PlaybackService.release(this);}
 void playFromMediaCard(){
  if(phone||!carAvailable()||closed||accountPage)return;
  android.media.AudioManager audio=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
  if(audio.getMode()!=android.media.AudioManager.MODE_NORMAL){Diagnostics.event("Media card play deferred: audio mode interruption");return;}
  if(!active&&!background){Diagnostics.event("Media card play ignored: background audio disabled");return;}
  releasePhoneSuppression();web.onResume();timer.removeCallbacks(sample);sampleDue=0;timer.post(sample);if(background)PlaybackService.keep(this);
  web.evaluateJavascript("(function(){var v=document.querySelector('video');if(!v)return 'no-video';v.muted=false;v.play().catch(()=>{});return 'play-requested';})()",value->{if(!closed)Diagnostics.event("Media card play request dispatched; playback result follows in observed state");});
 }

 void skipFromMediaCard(String id,boolean next){
  if(phone||!carAvailable()||closed||accountPage||phoneSuppressed||(!active&&!background)||!id.matches("[A-Za-z0-9_-]{11}"))return;
  android.media.AudioManager audio=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
  if(audio.getMode()!=android.media.AudioManager.MODE_NORMAL){Diagnostics.event("Media card skip deferred: audio mode interruption");return;}
  web.evaluateJavascript("window.__magdiTransport?window.__magdiTransport.skip('"+id+"',"+next+"):'unavailable'",value->{if(!closed)Diagnostics.event("Media card "+(next?"NEXT":"PREVIOUS")+" "+("\"requested\"".equals(value)?"request dispatched; playback result follows":"unavailable"));});
 }

 private void coverPlayback(){
  if(phone||!active||web==null||closed||accountPage||!background||phoneSuppressed)return;
  pendingResume=true;final long command=resumeCommandGeneration;
  web.evaluateJavascript("window.__magdiPlayback?window.__magdiPlayback.cover():false",value->{if(!closed&&!phoneSuppressed&&command==resumeCommandGeneration){pendingResume="true".equals(value);Diagnostics.event("Playback cover; resume intent="+pendingResume);}});
 }
 private void tryResumePlayback(){
  if(phone||!carAvailable()||closed||accountPage||!background||phoneSuppressed||!pendingResume)return;
  android.media.AudioManager audio=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
  if(audio.getMode()!=android.media.AudioManager.MODE_NORMAL||audio.isMusicActive()){Diagnostics.event("Resume deferred; audio is occupied");return;}
  pendingResume=false;
  web.evaluateJavascript("window.__magdiPlayback?window.__magdiPlayback.resume():'not-ready'",value->Diagnostics.event("Resume after interruption: "+value));
 }
 void prepareDetach(){pause();hideFullscreen();hideSearch();}
 boolean handleBack(){if(controllerFullscreen){hideControllerFullscreen();return true;}if(fullscreen!=null){hideFullscreen();return true;}if(searchPanel!=null){hideSearch();return true;}if(web.canGoBack()){web.goBack();return true;}return false;}
 private void setContentVisibility(View view,int visibility){if(hiddenViews.containsKey(view))hiddenViews.put(view,visibility);else view.setVisibility(visibility);}
 private void hideChildren(){for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child==content)continue;hiddenViews.put(child,child.getVisibility());child.setVisibility(GONE);}
  // Keep the WebView measured at the full content size while an overlay owns input.
  // GONE formerly removed it from layout, leaving a retained document with a stale viewport.
  web.setVisibility(INVISIBLE);
 }
 private void restoreChildren(){for(java.util.Map.Entry<View,Integer> e:hiddenViews.entrySet())e.getKey().setVisibility(e.getValue());hiddenViews.clear();web.setVisibility(VISIBLE);content.requestLayout();schedulePictureRefresh();}
 private void showFullscreen(View view,WebChromeClient.CustomViewCallback callback){
  if(fullscreen!=null){callback.onCustomViewHidden();return;}hideSearch();hideChildren();fullscreenCallback=callback;
  fullscreen=new FrameLayout(getContext()){
   @Override public void dispatchWindowVisibilityChanged(int visibility){
    if(visibility!=VISIBLE&&background&&!closed){coverPlayback();Diagnostics.event("Fullscreen temporarily covered");}
    // Keep the custom video subtree alive just as we keep the WebView alive.
    // The real host window still controls what is visible on the car display.
    super.dispatchWindowVisibilityChanged(background&&!closed?VISIBLE:visibility);
    if(visibility==VISIBLE&&!closed)scheduleFullscreenReturn();
   }
  };fullscreen.setBackgroundColor(Color.BLACK);fullscreen.addView(view,new FrameLayout.LayoutParams(-1,-1));
  ImageButton exit=new ImageButton(getContext());exit.setImageResource(R.drawable.ic_fullscreen_close);exit.setContentDescription(ar?"الخروج من ملء الشاشة":"Exit fullscreen");exit.setBackgroundResource(R.drawable.fullscreen_close_background);exit.setPadding(dp(14),dp(14),dp(14),dp(14));exit.setScaleType(ImageView.ScaleType.FIT_CENTER);exit.setElevation(0);exit.setStateListAnimator(null);
  // A 36 dp translucent circle inside a 48 dp touch target; keep the physical left edge in RTL too.
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER_VERTICAL|Gravity.LEFT);p.leftMargin=dp(2);fullscreen.addView(exit,p);exit.setOnClickListener(v->hideFullscreen());
  content.addView(fullscreen,new FrameLayout.LayoutParams(-1,-1));if(phone){android.view.WindowInsetsController controller=((android.app.Activity)getContext()).getWindow().getInsetsController();if(controller!=null){controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);controller.hide(android.view.WindowInsets.Type.systemBars());}}Diagnostics.event("Fullscreen entered");
 }
 private void hideFullscreen(){timer.removeCallbacks(fullscreenReturn);if(fullscreen==null)return;FrameLayout frame=fullscreen;fullscreen=null;content.removeView(frame);frame.removeAllViews();restoreChildren();if(phone){android.view.WindowInsetsController controller=((android.app.Activity)getContext()).getWindow().getInsetsController();if(controller!=null)controller.show(android.view.WindowInsets.Type.systemBars());}WebChromeClient.CustomViewCallback cb=fullscreenCallback;fullscreenCallback=null;if(cb!=null)cb.onCustomViewHidden();Diagnostics.event("Fullscreen exited");}
 private void hideSearch(){if(searchPanel==null)return;content.removeView(searchPanel);searchPanel=null;restoreChildren();}
 private Button largeActionButton(String text,String description,int minWidth){Button b=button(text,description);b.setTextSize(18);b.setMinWidth(dp(minWidth));b.setMinimumWidth(dp(minWidth));b.setPadding(dp(20),0,dp(20),0);return b;}
 private void showSearch(){
  hideFullscreen();if(searchPanel!=null)return;hideChildren();
  LinearLayout panel=new LinearLayout(getContext());panel.setBackgroundColor(0xff0b1220);panel.setOrientation(VERTICAL);panel.setPadding(dp(8),0,dp(8),0);searchPanel=panel;
  LinearLayout row=new LinearLayout(getContext());EditText input=new EditText(getContext());input.setTextColor(Color.WHITE);input.setSingleLine(true);input.setHint(ar?"ابحث في YouTube":"Search YouTube");input.setHintTextColor(0xffaaaaaa);input.setShowSoftInputOnFocus(phone);row.addView(input,new LinearLayout.LayoutParams(0,dp(56),1));
  Button go=largeActionButton(ar?"بحث":"Go",ar?"بحث":"Search",112),cancel=largeActionButton(ar?"إغلاق":"Close",ar?"إغلاق البحث":"Close search",96);for(Button b:new Button[]{go,cancel}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(56));p.setMarginStart(dp(4));row.addView(b,p);}panel.addView(row);
  Runnable submit=()->{String q=input.getText().toString().trim();if(q.isEmpty())return;hideSearch();web.loadUrl("https://m.youtube.com/results?search_query="+Uri.encode(q));Diagnostics.event("Search submitted; query not logged");};
  go.setOnClickListener(v->submit.run());cancel.setOnClickListener(v->hideSearch());input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);input.setOnEditorActionListener((v,id,event)->{if(id==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){submit.run();return true;}return false;});
  if(!phone){
   LinearLayout keys=new LinearLayout(getContext());keys.setOrientation(VERTICAL);panel.addView(keys,new LinearLayout.LayoutParams(-1,0,1));boolean[] arabic={ar};Runnable[] render=new Runnable[1];
   render[0]=()->{keys.removeAllViews();String[] rows=arabic[0]?new String[]{"١٢٣٤٥٦٧٨٩٠","ضصثقفغعهخحج","شسيبلاتنمكط","ئءؤرأإآىةوزظ"}:new String[]{"1234567890","qwertyuiop","asdfghjkl","zxcvbnm"};
    for(String chars:rows){LinearLayout line=new LinearLayout(getContext());for(int i=0;i<chars.length();i++){String letter=String.valueOf(chars.charAt(i));Button key=button(letter,letter);line.addView(key,new LinearLayout.LayoutParams(0,-1,1));key.setOnClickListener(v->input.getText().insert(Math.max(0,input.getSelectionStart()),letter));}keys.addView(line,new LinearLayout.LayoutParams(-1,0,1));}
    LinearLayout controls=new LinearLayout(getContext());Button lang=button(arabic[0]?"English":"عربي","Keyboard language"),space=button(ar?"مسافة":"Space","Space"),del=button("⌫","Delete");for(Button b:new Button[]{lang,space,del})controls.addView(b,new LinearLayout.LayoutParams(0,-1,1));keys.addView(controls,new LinearLayout.LayoutParams(-1,0,1));lang.setOnClickListener(v->{arabic[0]=!arabic[0];render[0].run();});space.setOnClickListener(v->input.getText().insert(Math.max(0,input.getSelectionStart())," "));del.setOnClickListener(v->{int pos=input.getSelectionStart();if(pos>0)input.getText().delete(pos-1,pos);});
   };render[0].run();
  }
  content.addView(panel,new FrameLayout.LayoutParams(-1,-1));input.requestFocus();
  if(phone)input.postDelayed(()->((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT),150);
 }
 void close(){if(!phone)CarController.detach(this);timer.removeCallbacks(controllerPoll);closed=true;active=false;PlaybackCoordinator.forget(this);invalidateSample();timer.removeCallbacks(sample);timer.removeCallbacks(refreshPicture);timer.removeCallbacks(fullscreenReturn);MediaCardService.forget(this);PlaybackService.release(this);hideFullscreen();hideSearch();Diagnostics.event("View destroyed");AdAssist.release(web);content.removeView(web);web.destroy();}
}
