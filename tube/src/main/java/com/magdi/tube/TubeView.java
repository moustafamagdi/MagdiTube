package com.magdi.tube;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.webkit.*;
import android.widget.*;
public final class TubeView extends LinearLayout {
 private final WebView web;
 private final TextView status;
 private final ProgressBar progress;
 private final Button back;
 private final android.os.Handler timer=new android.os.Handler(android.os.Looper.getMainLooper()); private boolean active; private final Runnable sample=new Runnable(){public void run(){if(!active)return;if(accountPage){timer.postDelayed(this,15000);return;}web.evaluateJavascript("(function(){var v=document.querySelector(\"video\");return v?JSON.stringify({paused:v.paused,ended:v.ended,time:Math.round(v.currentTime),ready:v.readyState,muted:v.muted,visible:document.visibilityState,adFilter:window.__magdiPlayerFilter?window.__magdiPlayerFilter.stats():null}):\"no-video\"})()",value->{if(active)Diagnostics.event("Video state "+value);});timer.postDelayed(this,15000);}};
 private final boolean ar; private boolean phone, accountPage;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private Button button(String text,String description){Button b=new Button(getContext());b.setText(text);b.setTextColor(0xffe6edf5);b.setTextSize(14);b.setAllCaps(false);b.setContentDescription(description);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff1b2a3c));return b;}
 @SuppressLint("SetJavaScriptEnabled") public TubeView(Context c){
 super(c);phone=c instanceof android.app.Activity;setOrientation(VERTICAL);setBackgroundColor(0xff0b1220);
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
 status=new TextView(c);status.setTextColor(0xffffcf8a);status.setPadding(dp(12),dp(6),dp(12),dp(6));status.setVisibility(GONE);addView(status);
 web=new WebView(c);web.setBackgroundColor(Color.BLACK);addView(web,new LinearLayout.LayoutParams(-1,0,1));
 CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
 web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(true);
 web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);
 web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
 web.setWebChromeClient(new WebChromeClient(){public void onProgressChanged(WebView v,int value){progress.setProgress(value);progress.setVisibility(value==100?INVISIBLE:VISIBLE);}});
 web.setWebViewClient(new WebViewClient(){
 public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
 Uri uri=r.getUrl();String host=uri.getHost();boolean auth=host!=null&&(host.equals("google.com")||host.endsWith(".google.com")||host.equals("accounts.google.com.sa"));
 if(auth&&!phone){Toast.makeText(c,ar?"سجّل الدخول من Magdi Tube على الموبايل ثم اضغط تحديث":"Sign in using Magdi Tube on your phone, then Reload",Toast.LENGTH_LONG).show();Diagnostics.event("Phone sign-in required");return true;}
 boolean block=!allowed(uri)&&!(phone&&"https".equals(uri.getScheme())&&auth);
 if(block&&r.isForMainFrame()){String scheme=uri.getScheme();String target=(scheme==null?"none":scheme)+"://"+(host==null?"none":host);target=target.replaceAll("[^a-zA-Z0-9.:/_-]","?");if(target.length()>160)target=target.substring(0,160);Diagnostics.event("Blocked navigation target="+target);status.setText((ar?"رابط غير مدعوم: ":"Unsupported link: ")+target+(ar?" — جرّب اشتراكاتي أو صدّر اللوج":" — try Subscriptions or export log"));status.setVisibility(VISIBLE);CookieManager.getInstance().flush();}return block;
 } public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){accountPage=!allowed(Uri.parse(u));status.setVisibility(GONE);Diagnostics.event(accountPage?"Account page started":"Page load started");}
 public void onPageFinished(WebView v,String u){back.setEnabled(v.canGoBack());CookieManager.getInstance().flush();Diagnostics.event(accountPage?"Account page loaded; authentication not verified":"Page loaded; cookies flushed");if(!accountPage)AdAssist.apply(v);}
 public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){Diagnostics.event("Main frame error code="+e.getErrorCode());status.setText(ar?"تعذر الاتصال. تحقق من الإنترنت واضغط تحديث.":"Unable to connect. Check your internet and tap Reload.");status.setVisibility(VISIBLE);}}
 });
 back.setEnabled(false);back.setOnClickListener(v->{if(web.canGoBack())web.goBack();});
 home.setOnClickListener(v->web.loadUrl("https://m.youtube.com/"));reload.setOnClickListener(v->{AdAssist.configure(web);web.reload();});
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
 Button logs=button(ar?"تصدير اللوج":"Export log",ar?"مشاركة تقرير التشخيص":"Share diagnostic report");
 for(Button b:new Button[]{account,subscriptions,ads,logs})actions.addView(b,new LinearLayout.LayoutParams(-2,dp(48)));
 HorizontalScrollView scroller=new HorizontalScrollView(c);scroller.addView(actions);addView(scroller,1,new LinearLayout.LayoutParams(-1,dp(50)));
 account.setOnClickListener(v->new android.app.AlertDialog.Builder(c).setTitle(ar?"تسجيل دخول تجريبي":"Experimental sign-in").setMessage(ar?"تدخل بياناتك في صفحة Google. قد ترفض Google المتصفح المضمّن. لا نغيّر هوية المتصفح ولا نقرأ كلمات المرور. بعد النجاح افتح الاشتراكات، ثم تحديث على شاشة السيارة. لا تمسح بيانات التطبيق إن أردت الاحتفاظ بالجلسة.":"Enter your credentials only on Google's page. Google may reject this embedded browser. We do not change browser identity or read passwords. After success, check Subscriptions, then Reload on the car. Clearing app data removes the session.").setNegativeButton(ar?"إلغاء":"Cancel",null).setPositiveButton(ar?"متابعة":"Continue",(d,w)->{Diagnostics.event("Account flow opened on phone");web.loadUrl("https://m.youtube.com/signin");}).show());
 subscriptions.setOnClickListener(v->web.loadUrl("https://m.youtube.com/feed/subscriptions"));
 ads.setOnClickListener(v->{AdAssist.toggle(c);AdAssist.configure(web);ads.setText(AdAssist.enabled(c)?(ar?"منع الإعلانات: يعمل":"Ad blocker: ON"):(ar?"منع الإعلانات: متوقف":"Ad blocker: OFF"));if(!accountPage){AdAssist.apply(web);web.reload();}Toast.makeText(c,ar?"حدّث صفحة السيارة لتطبيق التغيير. لا يضمن منع جميع الإعلانات.":"Reload the car page to apply. Experimental; complete blocking is not guaranteed.",Toast.LENGTH_LONG).show();});
 logs.setOnClickListener(v->Diagnostics.export((android.app.Activity)c));
 }
 AdAssist.configure(web);web.loadUrl("https://m.youtube.com/");
 }
 static boolean allowed(Uri u){String h=u.getHost();return "https".equals(u.getScheme())&&h!=null&&(h.equals("youtube.com")||h.endsWith(".youtube.com")||h.equals("youtu.be"));}
 void pause(){active=false;timer.removeCallbacks(sample);CookieManager.getInstance().flush();Diagnostics.event("View paused; cookies flushed");web.onPause();}
 void resume(){web.onResume();active=true;timer.removeCallbacks(sample);timer.postDelayed(sample,15000);Diagnostics.event("View resumed");}
 void close(){active=false;timer.removeCallbacks(sample);Diagnostics.event("View destroyed");AdAssist.release(web);removeView(web);web.destroy();}
}
