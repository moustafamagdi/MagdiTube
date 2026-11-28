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
 private boolean phoneSafe,documentReady,controllerFullscreen,controllerFullscreenRequested;
 private long controllerFullscreenGeneration;
 private final StartFullscreen startFullscreen=new StartFullscreen();
 private long selectionEpoch,selectionRequest,phoneSequence;
 private String selectedId="",selectionToken="",controllerObservation="";
 private final Runnable controllerPoll=new Runnable(){public void run(){if(closed||!active||!phone)return;controller.update();web.evaluateJavascript("window.__magdiPhoneEpoch="+CarController.epoch(),null);timer.postDelayed(this,500);}};

 private final FrameLayout content;
 private final TextView status;
 private final ProgressBar progress;
 private final Button back;
 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper());
 private final FullscreenControls fullscreenControls=new FullscreenControls(timer);
 @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){
  if(fullscreenControls!=null)fullscreenControls.beforeTouch(event);
  try{return super.dispatchTouchEvent(event);}finally{if(fullscreenControls!=null)fullscreenControls.afterTouch(event);}
 }
 @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){if(fullscreenControls!=null&&event.getAction()==android.view.KeyEvent.ACTION_DOWN)fullscreenControls.onKey();return super.dispatchKeyEvent(event);}
 private boolean active, covered, closed, background, phoneSuppressed, sampleInFlight, visualStatePending;
 private static int nextInstance;
 private final int instance=++nextInstance;
 private long sampleStarted,sampleGeneration,sampleDue,pictureWindowEnd,fullscreenWindowEnd,resumeCommandGeneration;
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
  if(closed||!active||covered)return;
  long now=android.os.SystemClock.uptimeMillis();if(now<pictureWindowEnd)return;pictureWindowEnd=now+1200;
  timer.post(refreshPicture);timer.postDelayed(refreshPicture,300);timer.postDelayed(refreshPicture,1100);
 }
 private long fullscreenResumeGeneration,fullscreenOwner;
 private final Runnable fullscreenReturn=()->{if(!closed&&active&&!covered&&fullscreenResumeGeneration==resumeCommandGeneration&&fullscreenOwner==CarController.owner())tryResumePlayback();};
 private void scheduleFullscreenReturn(){if(closed||!active||covered)return;schedulePictureRefresh();long now=android.os.SystemClock.uptimeMillis();if(now<fullscreenWindowEnd)return;fullscreenWindowEnd=now+1200;fullscreenResumeGeneration=resumeCommandGeneration;fullscreenOwner=CarController.owner();timer.postDelayed(fullscreenReturn,300);timer.postDelayed(fullscreenReturn,1100);}
 private void cancelPictureRefresh(){timer.removeCallbacks(refreshPicture);timer.removeCallbacks(fullscreenReturn);pictureWindowEnd=0;fullscreenWindowEnd=0;}
 private void invalidateSample(){sampleGeneration++;sampleInFlight=false;}
 private boolean acceptSample(long generation,long epoch,long request){
  if(closed||generation!=sampleGeneration)return false;
  // Release the outstanding slot even if the connection was not ready when sampled.
  sampleInFlight=false;return CarController.accepts(this,epoch,request);
 }
 private void controllerObservation(String result){
  if(result.equals(controllerObservation))return;controllerObservation=result;
  Diagnostics.event("Controller observation="+result); // No content, identifiers or tokens.
 }
 @Override public void onWindowFocusChanged(boolean focused){
  if(!focused&&web!=null&&keepCarMediaVisible())coverPlayback();
  super.onWindowFocusChanged(focused);
  if(!phone)CarUiDiagnostics.event(focused?"focus-gained":"focus-lost");
  if(focused&&web!=null){schedulePictureRefresh();if(fullscreen!=null)scheduleFullscreenReturn();}
 }
 @Override protected void onWindowVisibilityChanged(int visibility){
  if(visibility!=VISIBLE&&web!=null&&keepCarMediaVisible())coverPlayback();
  super.onWindowVisibilityChanged(visibility);
  if(!phone)CarUiDiagnostics.event(visibility==VISIBLE?"window-visible":"window-hidden");
  if(visibility==VISIBLE&&web!=null&&fullscreen!=null)scheduleFullscreenReturn();
 }
 @Override protected void onAttachedToWindow(){super.onAttachedToWindow();if(web!=null)schedulePictureRefresh();}
 private void refreshVideoSurface(){
  if(closed||!active||covered||accountPage||searchPanel!=null||!isAttachedToWindow()||!isShown())return;
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
  if(sampleInFlight&&started-sampleStarted>=6000){invalidateSample();controllerObservation("sample-timeout");}
  if(!accountPage&&!sampleInFlight){
   sampleInFlight=true;sampleStarted=started;final long request=++sampleGeneration;final long e=CarController.epoch(),r=CarController.request();
   boolean detailed=android.os.SystemClock.elapsedRealtime()-lastVideoLog>15000;
   web.evaluateJavascript("window.__magdiSample?window.__magdiSample("+detailed+","+MediaCardService.wantsArtwork(TubeView.this)+"):null",value->{
    if(!acceptSample(request,e,r))return;
    try{String json=new org.json.JSONArray("["+value+"]").getString(0);org.json.JSONObject state=new org.json.JSONObject(json);
     org.json.JSONObject control=state.optJSONObject("controller");String result=control==null?"helper-unavailable":CarController.report(TubeView.this,e,r,control);controllerObservation(result);
     if(!result.equals("accepted")&&!result.equals("current-loading"))return;
     timer.removeCallbacks(expireObservation);timer.postDelayed(expireObservation,CarController.FRESH_MS);
     state.remove("controller");
     updateFitControl(control.optBoolean("fill"));
     if(result.equals("current-loading")||control.optBoolean("error")||!control.optBoolean("hasVideo")||control.optInt("ready")==0){clearPlaybackConsumers();return;}
     if(startFullscreen.shouldStart(StartFullscreen.enabled(getContext()),active&&!covered&&!phoneSuppressed&&searchPanel==null,control)&&fullscreen==null&&!controllerFullscreenRequested)toggleControllerFullscreen();
     if(!state.optBoolean("resumeWanted"))pendingResume=false;
     MediaCardService.observe(TubeView.this,state.optBoolean("playing"),state.optBoolean("muted"),state.optInt("ready")>0,state.optLong("time")*1000,ThumbnailCache.Art.parse(state.optJSONObject("artwork")));
     state.remove("artwork"); // Never include thumbnail URLs or video IDs in diagnostics.
     if(state.optBoolean("playing")&&background)PlaybackService.keep(TubeView.this);else if(!pendingResume&&!interruption)PlaybackService.idle(TubeView.this);
     PlaybackCoordinator.observe(TubeView.this,state.optBoolean("playing")&&!state.optBoolean("muted")&&state.optInt("ready")>0);
     if(phoneSuppressed)return;
     if((covered||!active)&&getParent()==null&&!state.optBoolean("playing")&&!MediaCardService.retains(TubeView.this)){if(detachedIdleSince==0)detachedIdleSince=android.os.SystemClock.elapsedRealtime();if(android.os.SystemClock.elapsedRealtime()-detachedIdleSince>300000)CarPlaybackSession.stopDetached();}else detachedIdleSince=0;
     if(detailed){state.put("sampleDelayMs",late);state.put("callbackMs",android.os.SystemClock.uptimeMillis()-started);state.put("fullscreen",fullscreen!=null||controllerFullscreen);state.put("webWidth",web.getWidth());state.put("webHeight",web.getHeight());state.put("viewWidth",getWidth());state.put("viewHeight",getHeight());state.put("pictureRefreshes",pictureRefreshCount);pictureRefreshCount=0;
      Runtime runtime=Runtime.getRuntime();state.put("appJavaHeapMiB",(runtime.totalMemory()-runtime.freeMemory())/1048576);
      Diagnostics.event("Playback source="+(phone?"phone":"car")+" instance="+instance+" visible="+(active&&!covered)+" "+state);lastVideoLog=android.os.SystemClock.elapsedRealtime();}
    }catch(org.json.JSONException ignored){controllerObservation("sample-unavailable");}
   });
  }
  sampleDue=android.os.SystemClock.uptimeMillis()+2000;timer.postDelayed(this,2000);
 }};
 private final boolean ar; private boolean phone, accountPage;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private Button button(String text,String description){Button b=new Button(getContext());b.setText(text);b.setTextColor(0xffe6edf5);b.setTextSize(14);b.setAllCaps(false);b.setContentDescription(description);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff1b2a3c));return b;}
 @SuppressLint("SetJavaScriptEnabled") public TubeView(Context c){
 super(c);background=BackgroundAudio.enabled(c);phone=c instanceof android.app.Activity;setOrientation(VERTICAL);setBackgroundColor(0xff0b1220);
 Diagnostics.event(c instanceof android.app.Activity?"Phone view created":"Car view created");
 ar=c.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
 LinearLayout bar=new LinearLayout(c);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(8),0,dp(8),0);
 ImageView logo=new ImageView(c);logo.setImageResource(R.drawable.ic_tube);logo.setContentDescription(c.getString(R.string.app_name));bar.addView(logo,new LinearLayout.LayoutParams(dp(32),dp(32)));
 TextView title=new TextView(c);title.setText(R.string.app_name);title.setTextColor(Color.WHITE);title.setTextSize(16);title.setPadding(dp(8),0,0,0);bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
 back=button(ar?"رجوع":"Back",ar?"الصفحة السابقة":"Previous page");
 Button home=button(ar?"الرئيسية":"Home",ar?"رئيسية YouTube":"YouTube home");
 Button reload=button(ar?"تحديث":"Reload",ar?"إعادة تحميل الصفحة":"Reload page");
 for(Button b:new Button[]{back,home,reload}){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(48));p.setMarginStart(dp(4));bar.addView(b,p);}
 
 addView(bar,new LinearLayout.LayoutParams(-1,dp(52)));
 progress=new ProgressBar(c,null,android.R.attr.progressBarStyleHorizontal);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff45dfcc));addView(progress,new LinearLayout.LayoutParams(-1,dp(2)));
 status=new TextView(c);status.setTextColor(0xffffcf8a);status.setPadding(dp(12),dp(6),dp(12),dp(6));setContentVisibility(status,GONE);addView(status);
 web=new WebView(c){@Override public void onVisibilityAggregated(boolean visible){
 // Modern WebView also receives aggregated visibility, independently of window visibility.
 boolean retain=keepCarMediaVisible();
 if(!visible&&retain)coverPlayback();
 super.onVisibilityAggregated(visible||retain);
 }@Override protected void onWindowVisibilityChanged(int visibility){
 // Preserve the existing media document during a temporary host cover (for example reverse camera).
 boolean retain=keepCarMediaVisible();
 if(visibility!=View.VISIBLE&&retain)coverPlayback();
 super.onWindowVisibilityChanged(retain?View.VISIBLE:visibility);
 }};if(phone)phoneSafe=ControllerCookies.prepare(web);web.setBackgroundColor(Color.BLACK);content=new FrameLayout(c);content.addView(web,new FrameLayout.LayoutParams(-1,-1));addView(content,new LinearLayout.LayoutParams(-1,0,1));
 CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
 web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
 // The legacy car host renders in its own density, which can differ from the
 // phone display cached by Chromium. Initial scale is physical percent, not dp.
 if(!phone)web.setInitialScale(Math.max(1,Math.round(c.getResources().getDisplayMetrics().density*100)));
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
 Uri uri=r.getUrl();if(phone&&r.isForMainFrame()&&VideoSelection.parse(uri.toString())!=null){controller.select(uri.toString(),CarController.epoch());return true;}String host=uri.getHost();boolean auth=GoogleAccountNavigation.allowed(uri);
 if(auth&&!phone){Toast.makeText(c,ar?"سجّل الدخول من MM Tube على الموبايل ثم اضغط تحديث":"Sign in using MM Tube on your phone, then Reload",Toast.LENGTH_LONG).show();Diagnostics.event("Phone sign-in required");return true;}
 boolean block=!allowed(uri)&&!(phone&&auth);
 if(block&&r.isForMainFrame()){String scheme=uri.getScheme();String target=(scheme==null?"none":scheme)+"://"+(host==null?"none":host);target=target.replaceAll("[^a-zA-Z0-9.:/_-]","?");if(target.length()>160)target=target.substring(0,160);Diagnostics.event("Blocked navigation target="+target);status.setText((ar?"رابط غير مدعوم: ":"Unsupported link: ")+target+(ar?" — جرّب اشتراكاتي أو صدّر اللوج":" — try Subscriptions or export log"));setContentVisibility(status,VISIBLE);CookiePersistence.request();}return block;
 } public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){invalidateSample();if(!phone){hideControllerFullscreen();CarController.navigating(TubeView.this);clearPlaybackConsumers();}phoneSequence=0;accountPage=!allowed(Uri.parse(u));MediaCardService.clearArtwork(TubeView.this);if(accountPage)PlaybackService.release(TubeView.this);setContentVisibility(status,GONE);Diagnostics.event(accountPage?"Account page started":"Page load started");}
 public void onPageFinished(WebView v,String u){
  if(closed)return;back.setEnabled(v.canGoBack()||fullscreen!=null);CookiePersistence.request();
  if(phone){web.evaluateJavascript("window.__magdiPhoneEpoch="+CarController.epoch(),null);Diagnostics.event("Controller browsing loaded; car authentication not verified");return;}
  if(!active||"about:blank".equals(u)||!u.equals(v.getUrl()))return;
  if(!accountPage){documentReady=true;CarController.ready(TubeView.this);AdAssist.apply(v);
   try{for(String asset:new String[]{"video_fit.js","playback_state.js","media_artwork.js","media_transport.js","car_controller.js","car_audio.js","playback_sample.js"})v.evaluateJavascript(AdAssist.asset(c,asset),null);}catch(java.io.IOException e){Diagnostics.event("Playback tracking unavailable");}
   VideoSelection loaded=VideoSelection.parse(u);
   if(!selectionToken.isEmpty()&&loaded!=null&&loaded.id.equals(selectedId)&&CarController.accepts(TubeView.this,selectionEpoch,selectionRequest)){nativeMute(false);v.evaluateJavascript("window.__magdiCar&&window.__magdiCar.start("+org.json.JSONObject.quote(selectedId)+","+org.json.JSONObject.quote(selectionToken)+")",null);}
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
 Button carAds=button(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ads block: ON"):(ar?"منع الإعلانات: متوقف":"Ads block: OFF"),ar?"تشغيل أو إيقاف منع الإعلانات":"Toggle ad blocking");
 bar.addView(carAds,new LinearLayout.LayoutParams(-2,dp(48)));
 carAds.setOnClickListener(v->{AdAssist.toggle(c);carAds.setText(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ads block: ON"):(ar?"منع الإعلانات: متوقف":"Ads block: OFF"));AdAssist.configure(web);AdAssist.apply(web);web.reload();});
 }
 if(phone){
 reload.setVisibility(GONE);
 Button more=button(ar?"المزيد":"More",ar?"الحساب والمساعدة والإعدادات":"Account, help and settings");bar.addView(more,new LinearLayout.LayoutParams(-2,dp(48)));
 Button account=button(ar?"الحساب":"Account",ar?"تسجيل YouTube التجريبي":"Experimental YouTube sign-in");
 Button ads=button(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ads block: ON"):(ar?"منع الإعلانات: متوقف":"Ads block: OFF"),ar?"منع الإعلانات التجريبي":"Experimental ad blocking");
 Button audio=button(ar?"صوت الخلفية":"Background audio",ar?"تشغيل أو إيقاف صوت الخلفية":"Toggle background audio");
 audio.setOnClickListener(v->{background=!BackgroundAudio.enabled(c);BackgroundAudio.setEnabled(c,background);if(!background)PlaybackService.release(this);Toast.makeText(c,background?(ar?"صوت الخلفية يعمل":"Background audio ON"):(ar?"صوت الخلفية متوقف":"Background audio OFF"),Toast.LENGTH_SHORT).show();});
 Button logs=button(ar?"الدعم والتشخيص":"Support & diagnostics",ar?"إرسال تقرير دعم":"Send a support report");
 Button setup=button(ar?"التثبيت والتحديث عبر MM Auto":"Installation & updates — MM Auto",ar?"فتح مساعد التثبيت":"Open installation help");
 setup.setOnClickListener(v->c.startActivity(new android.content.Intent(c,InstallSetupActivity.class)));
 more.setOnClickListener(v->{PopupMenu menu=new PopupMenu(c,more);Button[] options={account,logs};for(int i=0;i<options.length;i++)menu.getMenu().add(0,i,i,options[i].getText());menu.getMenu().add(0,4,4,ar?"خيارات متقدمة":"Advanced options");menu.getMenu().add(0,5,5,ar?"بدء الفيديو بملء الشاشة على العربية":"Start videos fullscreen on the car").setCheckable(true).setChecked(StartFullscreen.enabled(c));menu.setOnMenuItemClickListener(item->{if(item.getItemId()==5){StartFullscreen.setEnabled(c,!item.isChecked());Toast.makeText(c,ar?"يُطبّق على الفيديو التالي على العربية":"Applies to the next video on the car",Toast.LENGTH_SHORT).show();return true;}if(item.getItemId()==4){PopupMenu advanced=new PopupMenu(c,more);Button[] extra={reload,ads,audio,setup};for(int i=0;i<extra.length;i++)advanced.getMenu().add(0,i,i,extra[i].getText());advanced.getMenu().findItem(2).setCheckable(true).setChecked(BackgroundAudio.enabled(c));advanced.getMenu().add(0,4,4,ar?"إعادة تهيئة الشاشة":"Reset browsing screen");advanced.setOnMenuItemClickListener(choice->{if(choice.getItemId()==4)reload.performLongClick();else extra[choice.getItemId()].performClick();return true;});advanced.show();}else options[item.getItemId()].performClick();return true;});menu.show();});
 account.setOnClickListener(v->new android.app.AlertDialog.Builder(c).setTitle(ar?"تسجيل دخول تجريبي":"Experimental sign-in").setMessage(ar?"تدخل بياناتك في صفحة Google. قد ترفض Google المتصفح المضمّن. لا نغيّر هوية المتصفح ولا نقرأ كلمات المرور. بعد النجاح افتح الاشتراكات، ثم تحديث على شاشة السيارة. لا تمسح بيانات التطبيق إن أردت الاحتفاظ بالجلسة.":"Enter your credentials only on Google's page. Google may reject this embedded browser. We do not change browser identity or read passwords. After success, check Subscriptions, then Reload on the car. Clearing app data removes the session.").setNegativeButton(ar?"إلغاء":"Cancel",null).setPositiveButton(ar?"متابعة":"Continue",(d,w)->{Diagnostics.event("Account flow opened on phone");web.loadUrl("https://m.youtube.com/signin");}).show());
 ads.setOnClickListener(v->{AdAssist.toggle(c);AdAssist.configure(web);ads.setText(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ads block: ON"):(ar?"منع الإعلانات: متوقف":"Ads block: OFF"));if(!accountPage){AdAssist.apply(web);web.reload();}Toast.makeText(c,ar?"حدّث صفحة السيارة لتطبيق التغيير. لا يضمن منع جميع الإعلانات.":"Reload the car page to apply. Experimental; complete blocking is not guaranteed.",Toast.LENGTH_LONG).show();});
 logs.setOnClickListener(v->Diagnostics.open((android.app.Activity)c));
 }
 if(phone){
  controller=new PhoneController(c,ar);addView(controller,2,new LinearLayout.LayoutParams(-1,-2));
  phoneSafe=phoneSafe&&PhoneMediaPolicy.install(web)&&androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.WEB_MESSAGE_LISTENER);
  if(phoneSafe)androidx.webkit.WebViewCompat.addWebMessageListener(web,"MagdiController",new java.util.HashSet<>(java.util.Arrays.asList("https://m.youtube.com","https://www.youtube.com","https://youtube.com")),(view,message,origin,main,reply)->{
   if(!main||closed||!active)return;try{String data=message.getData();if(data==null||data.length()>5000)return;org.json.JSONObject packet=new org.json.JSONObject(data);long seq=packet.optLong("sequence");if(seq<=phoneSequence)return;phoneSequence=seq;controller.select(packet.optString("url"),packet.optLong("epoch",-1));}catch(org.json.JSONException ignored){}
  });
  if(!phoneSafe){web.getSettings().setJavaScriptEnabled(false);status.setText(ar?"حدّث Android System WebView لتفعيل التصفح بدون تشغيل محلي.":"Update Android System WebView to enable controller browsing.");setContentVisibility(status,VISIBLE);}
 }else {CarDocumentLease.install(web,"");AdAssist.configure(web);}
 if(!phone||phoneSafe)web.loadUrl("https://m.youtube.com/");
 }
 static boolean allowed(Uri u){String h=u.getHost();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&h!=null&&(h.equals("youtube.com")||h.endsWith(".youtube.com")||h.equals("youtu.be"));}

 // A covered host is still connected: keep its document, polling and controller lease.
 boolean keepCarMediaVisible(){background=BackgroundAudio.enabled(getContext());return !phone&&!closed&&active&&background&&!accountPage&&!phoneSuppressed&&CarPlaybackSession.connected();}
 void cover(){if(phone||closed||!active||covered)return;keepCarMediaVisible();coverPlayback();covered=true;cancelPictureRefresh();CookiePersistence.request();if(!background){pauseMedia();web.onPause();}Diagnostics.event("Car view covered; session retained; background audio="+background);}
 void pause(){if(phone&&controller!=null)controller.cancelLaunch();cancelPictureRefresh();active=false;covered=false;timer.removeCallbacks(controllerPoll);timer.removeCallbacks(sample);invalidateSample();CookiePersistence.request();if(!phone){CarController.detach(this);documentReady=false;}web.onPause();Diagnostics.event(phone?"Phone controller covered":"Car player unavailable; playback stopped");}
 void resume(){if(closed)return;background=BackgroundAudio.enabled(getContext());boolean wasCovered=covered;covered=false;active=true;web.onResume();invalidateSample();sampleDue=0;timer.removeCallbacks(sample);
  if(phone){timer.removeCallbacks(controllerPoll);timer.post(controllerPoll);}else{CarController.attach(this);if(documentReady)CarController.ready(this);if(!wasCovered&&(!documentReady||"about:blank".equals(web.getUrl())))web.loadUrl("https://m.youtube.com/");timer.post(sample);}
  Diagnostics.event("View resumed");schedulePictureRefresh();if(wasCovered&&!phone)scheduleFullscreenReturn();
 }
 public boolean isCarView(){return !phone;}
 boolean carAvailable(){return !phone&&active&&!closed&&CarController.accepts(this,CarController.epoch(),CarController.request());}
 private void nativeMute(boolean muted){if(androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.MUTE_AUDIO))androidx.webkit.WebViewCompat.setAudioMuted(web,muted);}
 public void cancelController(){selectionToken="";selectedId="";CarDocumentLease.install(web,"");invalidateSample();pendingResume=false;resumeCommandGeneration++;nativeMute(true);web.stopLoading();web.evaluateJavascript("window.__magdiCar?.cancel();window.__magdiPlayback?.stop();document.querySelectorAll('video,audio').forEach(v=>v.pause())",null);clearPlaybackConsumers();hideFullscreen();hideControllerFullscreen();web.loadUrl("about:blank");MediaCardService.forget(this);PlaybackService.release(this);}
 public void stopController(){cancelController();if(carAvailable())web.loadUrl("https://m.youtube.com/");}
 public void selectController(VideoSelection video,long epoch,long request){
  cancelController();selectionEpoch=epoch;selectionRequest=request;selectedId=video.id;selectionToken=epoch+"_"+request;if(!CarDocumentLease.install(web,selectionToken)||!ControllerCookies.toCar()){CarController.failed(this,epoch,request);return;}CookiePersistence.request();
  timer.postDelayed(()->{if(!CarController.accepts(this,epoch,request)||closed||!active)return;CarController.loading(this,epoch,request);web.loadUrl(video.url);},150);
  timer.postDelayed(()->{if(!CarController.accepts(this,epoch,request))return;String phase=CarController.state().optString("phase");if(phase.equals("sending")||phase.equals("loading"))CarController.failed(this,epoch,request);},30000);
 }
 public void commandController(String action,double value,long epoch,long request){
  if(!carAvailable()||!CarController.accepts(this,epoch,request)||!CarController.can(action))return;
  if("pause".equals(action)||"play".equals(action)||"next".equals(action)||"previous".equals(action)){resumeCommandGeneration++;pendingResume=false;}
  if("fullscreen".equals(action)){startFullscreen.manual(CarController.state().optJSONObject("playback"));toggleControllerFullscreen();return;}
  if("fill".equals(action)){ownedScript("return c.setFill("+(value==1)+",t,d,id,g);",result->Diagnostics.event("Fullscreen picture mode="+(value==1?"fill":"full-picture")+" result="+("true".equals(result)?"applied":"rejected")));return;}
  ownedScript("return c.command("+org.json.JSONObject.quote(action)+","+value+",t,d,id,g);",null);
 }
 // The JS guard checks ownership when Chromium actually executes, not just when queued.
 private void ownedScript(String body,ValueCallback<String> callback){
  if(!carAvailable()||!CarController.fresh())return;
  org.json.JSONObject p=CarController.state().optJSONObject("playback");
  long e=CarController.epoch(),r=CarController.request(),o=CarController.owner(),g=resumeCommandGeneration;
  String js="(()=>{const c=window.__magdiCar,t="+org.json.JSONObject.quote(p.optString("token"))+",d="+org.json.JSONObject.quote(p.optString("document"))+",id="+org.json.JSONObject.quote(p.optString("id"))+",g="+g+";if(!c||!c.guard(t,d,id,g))return false;"+body+"})()";
  web.evaluateJavascript(js,callback==null?null:value->{if(!closed&&active&&CarController.accepts(this,e,r)&&CarController.owner()==o&&resumeCommandGeneration==g&&CarController.fresh())callback.onReceiveValue(value);});
 }
 private void clearPlaybackConsumers(){pendingResume=false;resumeCommandGeneration++;MediaCardService.forget(this);PlaybackService.release(this);PlaybackCoordinator.observe(this,false);}
 private final Runnable expireObservation=()->{if(!closed&&!CarController.fresh())clearPlaybackConsumers();};
 private void toggleControllerFullscreen(){
  if(fullscreen!=null){hideFullscreen();return;}
  if(controllerFullscreenRequested){hideControllerFullscreen();return;}
  controllerFullscreenRequested=true;long generation=++controllerFullscreenGeneration;
  timer.postDelayed(()->{if(generation==controllerFullscreenGeneration&&controllerFullscreenRequested&&!controllerFullscreen){hideControllerFullscreen();Diagnostics.event("Car fullscreen acknowledgement expired");}},1500);
  ownedScript("return c.fullscreen(true,t,d,id,g);",value->{
   if(generation!=controllerFullscreenGeneration||!controllerFullscreenRequested)return;
   if(!"true".equals(value)){controllerFullscreenRequested=false;Diagnostics.event("Car fullscreen unavailable; original layout retained");return;}
   controllerFullscreen=true;hideChrome();
   addFullscreenExit(content,"controller-fullscreen-exit",this::hideControllerFullscreen);
   content.requestLayout();schedulePictureRefresh();Diagnostics.event("Controller fullscreen entered");
  });
 }
 private void hideControllerFullscreen(){
  if(controllerFullscreenRequested||controllerFullscreen)startFullscreen.manual(CarController.state().optJSONObject("playback"));
  ++controllerFullscreenGeneration;
  if(!controllerFullscreenRequested&&!controllerFullscreen)return;
  fullscreenControls.clear();
  controllerFullscreenRequested=false;boolean restore=controllerFullscreen;controllerFullscreen=false;
  View exit=content.findViewWithTag("controller-fullscreen-exit");if(exit!=null)content.removeView(exit);
  View fit=content.findViewWithTag("controller-fullscreen-exit-fit");if(fit!=null)content.removeView(fit);
  web.evaluateJavascript("window.__magdiCar?.exitFullscreen()",null);
  if(restore)restoreChildren();Diagnostics.event("Controller fullscreen exited");
 }
 void suppressHiddenPhone(){
  if(!phone||active||closed||phoneSuppressed||accountPage)return;phoneSuppressed=true;cancelPictureRefresh();invalidateSample();timer.removeCallbacks(sample);pauseMedia();
  web.evaluateJavascript("if(window.__magdiPlayback)window.__magdiPlayback.suppress(true)",null);web.onPause();Diagnostics.event("Hidden phone playback suspended while car audio plays; instance="+instance);
 }
 private void releasePhoneSuppression(){if(!phoneSuppressed)return;phoneSuppressed=false;pendingResume=false;web.evaluateJavascript("if(window.__magdiPlayback)window.__magdiPlayback.suppress(false)",null);Diagnostics.event("Phone playback available again; automatic resume disabled; instance="+instance);}

 void pauseMedia(){if(!carAvailable())return;commandController("pause",0,CarController.epoch(),CarController.request());}
 void playFromMediaCard(){
  if(!carAvailable())return;
  android.media.AudioManager audio=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
  if(audio.getMode()!=android.media.AudioManager.MODE_NORMAL)return;
  commandController("play",0,CarController.epoch(),CarController.request());
 }
 private void coverPlayback(){
  if(phone||!active||web==null||closed||accountPage||!background||phoneSuppressed||!CarController.fresh())return;
  ownedScript("return window.__magdiPlayback?window.__magdiPlayback.cover():false;",value->{pendingResume="true".equals(value);Diagnostics.event("Playback cover; resume intent="+pendingResume);});
 }
 private void tryResumePlayback(){
  if(phone||!carAvailable()||closed||accountPage||!background||phoneSuppressed||!pendingResume||!CarController.fresh())return;
  android.media.AudioManager audio=(android.media.AudioManager)getContext().getSystemService(Context.AUDIO_SERVICE);
  if(audio.getMode()!=android.media.AudioManager.MODE_NORMAL||audio.isMusicActive()){Diagnostics.event("Resume deferred; audio is occupied");return;}
  pendingResume=false;
  ownedScript("return window.__magdiPlayback?window.__magdiPlayback.resume():'not-ready';",value->Diagnostics.event("Resume request evaluated; playback result follows in observed state"));
 }
 void prepareDetach(){pause();hideFullscreen();hideSearch();}
 void retainCoveredHost(){cover();hideSearch();}
 boolean handleBack(){if(controllerFullscreenRequested){hideControllerFullscreen();return true;}if(fullscreen!=null){hideFullscreen();return true;}if(searchPanel!=null){hideSearch();return true;}if(web.canGoBack()){web.goBack();return true;}return false;}
 private void setContentVisibility(View view,int visibility){if(hiddenViews.containsKey(view))hiddenViews.put(view,visibility);else view.setVisibility(visibility);}
 private void hideChrome(){for(int i=0;i<getChildCount();i++){View child=getChildAt(i);if(child==content)continue;hiddenViews.put(child,child.getVisibility());child.setVisibility(GONE);}}
 private void hideChildren(){hideChrome();
  // Keep the WebView measured at the full content size while an overlay owns input.
  // GONE formerly removed it from layout, leaving a retained document with a stale viewport.
  web.setVisibility(INVISIBLE);
 }
 private void restoreChildren(){for(java.util.Map.Entry<View,Integer> e:hiddenViews.entrySet())e.getKey().setVisibility(e.getValue());hiddenViews.clear();web.setVisibility(VISIBLE);content.requestLayout();schedulePictureRefresh();}
 // Shared visual and touch target for remote and YouTube-native fullscreen.
 private void updateFitControl(boolean fill){
  for(String tag:new String[]{"controller-fullscreen-exit-fit","native-fullscreen-exit-fit"}){View target=content.findViewWithTag(tag);if(target instanceof ImageButton){((ImageButton)target).setImageResource(fill?R.drawable.ic_picture_fit:R.drawable.ic_picture_fill);target.setSelected(fill);target.setContentDescription(fill?(ar?"عرض الصورة كاملة":"Show full picture"):(ar?"ملء الشاشة مع قص الأطراف":"Fill screen (crop edges)"));}}
 }
 private void addFullscreenExit(FrameLayout parent,String tag,Runnable onExit){
  ImageButton exit=new ImageButton(getContext());exit.setImageResource(R.drawable.ic_fullscreen_close);exit.setContentDescription(ar?"الخروج من ملء الشاشة":"Exit fullscreen");exit.setBackgroundResource(R.drawable.fullscreen_close_background);exit.setPadding(dp(14),dp(14),dp(14),dp(14));exit.setScaleType(ImageView.ScaleType.FIT_CENTER);exit.setElevation(0);exit.setStateListAnimator(null);
  // A 36 dp translucent circle inside a 48 dp touch target; keep the physical left edge in RTL too.
  FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER_VERTICAL|Gravity.LEFT);p.leftMargin=dp(2);exit.setTranslationY(-dp(26));parent.addView(exit,p);exit.setTag(tag);exit.setOnClickListener(v->onExit.run());
  ImageButton fit=new ImageButton(getContext());fit.setImageResource(R.drawable.ic_picture_fill);fit.setBackgroundResource(R.drawable.fullscreen_close_background);fit.setPadding(dp(14),dp(14),dp(14),dp(14));fit.setScaleType(ImageView.ScaleType.FIT_CENTER);fit.setElevation(0);fit.setStateListAnimator(null);fit.setContentDescription(ar?"ملء الشاشة مع قص الأطراف":"Fill screen (crop edges)");fit.setTag(tag+"-fit");FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.CENTER_VERTICAL|Gravity.LEFT);fp.leftMargin=dp(2);fit.setTranslationY(dp(26));parent.addView(fit,fp);
  fit.setOnClickListener(v->{org.json.JSONObject state=CarController.state().optJSONObject("playback");if(state!=null)commandController("fill",state.optBoolean("fill")?0:1,CarController.epoch(),CarController.request());});
  fullscreenControls.bind(exit,fit);
 }
 private void showFullscreen(View view,WebChromeClient.CustomViewCallback callback){
  if(fullscreen!=null){callback.onCustomViewHidden();return;}hideControllerFullscreen();hideSearch();hideChildren();fullscreenCallback=callback;
  fullscreen=new FrameLayout(getContext()){
   @Override public void dispatchWindowVisibilityChanged(int visibility){
    if(visibility!=VISIBLE&&background&&!closed){coverPlayback();Diagnostics.event("Fullscreen temporarily covered");}
    // Keep the custom video subtree alive just as we keep the WebView alive.
    // The real host window still controls what is visible on the car display.
    super.dispatchWindowVisibilityChanged(background&&!closed?VISIBLE:visibility);
    if(visibility==VISIBLE&&!closed)scheduleFullscreenReturn();
   }
  };fullscreen.setBackgroundColor(Color.BLACK);fullscreen.addView(view,new FrameLayout.LayoutParams(-1,-1));
  addFullscreenExit(fullscreen,"native-fullscreen-exit",this::hideFullscreen);
  content.addView(fullscreen,new FrameLayout.LayoutParams(-1,-1));if(phone){android.view.WindowInsetsController controller=((android.app.Activity)getContext()).getWindow().getInsetsController();if(controller!=null){controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);controller.hide(android.view.WindowInsets.Type.systemBars());}}Diagnostics.event("Fullscreen entered");
 }
 private void hideFullscreen(){timer.removeCallbacks(fullscreenReturn);if(fullscreen==null)return;startFullscreen.manual(CarController.state().optJSONObject("playback"));fullscreenControls.clear();FrameLayout frame=fullscreen;fullscreen=null;content.removeView(frame);frame.removeAllViews();restoreChildren();if(phone){android.view.WindowInsetsController controller=((android.app.Activity)getContext()).getWindow().getInsetsController();if(controller!=null)controller.show(android.view.WindowInsets.Type.systemBars());}WebChromeClient.CustomViewCallback cb=fullscreenCallback;fullscreenCallback=null;if(cb!=null)cb.onCustomViewHidden();Diagnostics.event("Fullscreen exited");}
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
   render[0]=()->{keys.removeAllViews();String[] rows=arabic[0]?new String[]{"ذ١٢٣٤٥٦٧٨٩٠","ضصثقفغعهخحجد","شسيبلاتنمكط","ئءؤرأإآىةوزظ"}:new String[]{"1234567890","qwertyuiop","asdfghjkl","zxcvbnm"};
    for(String chars:rows){LinearLayout line=new LinearLayout(getContext());for(int i=0;i<chars.length();i++){String letter=String.valueOf(chars.charAt(i));Button key=button(letter,letter);line.addView(key,new LinearLayout.LayoutParams(0,-1,1));key.setOnClickListener(v->input.getText().insert(Math.max(0,input.getSelectionStart()),letter));}keys.addView(line,new LinearLayout.LayoutParams(-1,0,1));}
    LinearLayout controls=new LinearLayout(getContext());Button lang=button(arabic[0]?"English":"عربي","Keyboard language"),space=button(ar?"مسافة":"Space","Space"),del=button("⌫","Delete");for(Button b:new Button[]{lang,space,del})controls.addView(b,new LinearLayout.LayoutParams(0,-1,1));keys.addView(controls,new LinearLayout.LayoutParams(-1,0,1));lang.setOnClickListener(v->{arabic[0]=!arabic[0];render[0].run();});space.setOnClickListener(v->input.getText().insert(Math.max(0,input.getSelectionStart())," "));del.setOnClickListener(v->{int pos=input.getSelectionStart();if(pos>0)input.getText().delete(pos-1,pos);});
   };render[0].run();
  }
  content.addView(panel,new FrameLayout.LayoutParams(-1,-1));input.requestFocus();
  if(phone)input.postDelayed(()->((android.view.inputmethod.InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT),150);
 }
 void close(){fullscreenControls.clear();if(phone&&controller!=null)controller.cancelLaunch();if(!phone)CarController.detach(this);timer.removeCallbacks(controllerPoll);closed=true;active=false;timer.removeCallbacks(expireObservation);PlaybackCoordinator.forget(this);invalidateSample();timer.removeCallbacks(sample);timer.removeCallbacks(refreshPicture);timer.removeCallbacks(fullscreenReturn);MediaCardService.forget(this);PlaybackService.release(this);hideFullscreen();hideSearch();Diagnostics.event("View destroyed");AdAssist.release(web);CarDocumentLease.release(web);content.removeView(web);web.destroy();}
}
