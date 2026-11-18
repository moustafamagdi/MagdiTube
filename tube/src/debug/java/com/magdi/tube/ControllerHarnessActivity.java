package com.magdi.tube;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.FutureTask;

/** Debug-only fixture, two production TubeViews, actual Chromium media decoder and native routing. */
public final class ControllerHarnessActivity extends Activity {
 private TubeView phone,car;private boolean connected=true,phoneOpen=true;
 private WebView phoneWeb,carWeb,probe;
 public void onCreate(Bundle b){super.onCreate(b);WebView.setWebContentsDebuggingEnabled(true);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);Button toggle=new Button(this);toggle.setText("Toggle test car");root.addView(toggle);
  LinearLayout panels=new LinearLayout(this);root.addView(panels,new LinearLayout.LayoutParams(-1,0,1));
  phone=new TubeView(this);car=new TubeView(new android.view.ContextThemeWrapper(this,android.R.style.Theme_Material_NoActionBar));
  phoneWeb=web(phone);carWeb=web(car);fixture(phoneWeb,true);fixture(carWeb,false);
  phoneWeb.addJavascriptInterface(new Harness(),"Harness");
  panels.addView(phone,new LinearLayout.LayoutParams(0,-1,1));panels.addView(car,new LinearLayout.LayoutParams(0,-1,1));setContentView(root);
  probe=new WebView(this);probe.getSettings().setJavaScriptEnabled(true);probe.getSettings().setMediaPlaybackRequiresUserGesture(false);root.addView(probe,new LinearLayout.LayoutParams(1,1));
  probe.setWebViewClient(new WebViewClient(){public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){return PhoneMediaPolicy.html(probeHtml());}});probe.loadUrl("https://native-policy.invalid/");
  toggle.setOnClickListener(v->toggle());phoneWeb.loadUrl("https://m.youtube.com/");carWeb.loadUrl("https://m.youtube.com/");
 }
 private WebView web(TubeView view){try{java.lang.reflect.Field f=TubeView.class.getDeclaredField("web");f.setAccessible(true);return (WebView)f.get(view);}catch(Exception e){throw new RuntimeException(e);}}
 private void toggle(){connected=!connected;if(connected)car.resume();else car.pause();}
 private final class Harness {
  private String ui(java.util.concurrent.Callable<String> action){FutureTask<String> task=new FutureTask<>(action);runOnUiThread(task);try{return task.get(10,java.util.concurrent.TimeUnit.SECONDS);}catch(Exception e){return "error";}}
  @JavascriptInterface public String state(){return ui(()->CarController.state().toString());}
  @JavascriptInterface public String controls(){return ui(()->{org.json.JSONObject out=new org.json.JSONObject();for(String label:new String[]{"Play","Pause","Stop","Previous","Next","Car fullscreen"}){Button button=findButton(phone,label);out.put(label,button!=null&&button.isEnabled());}return out.toString();});}
  @JavascriptInterface public String command(String action,double value){return ui(()->String.valueOf(CarController.command(action,value,CarController.epoch(),CarController.request(),CarController.owner())));}
  @JavascriptInterface public String toggleCar(){return ui(()->{toggle();return "ok";});}
  @JavascriptInterface public String closePhone(){return ui(()->{phoneOpen=false;phone.pause();phone.setVisibility(android.view.View.GONE);return "ok";});}
  @JavascriptInterface public String muted(){return ui(()->String.valueOf(androidx.webkit.WebViewCompat.isAudioMuted(phoneWeb)));}
  @JavascriptInterface public String cookies(){return ui(()->{
   String origin="https://m.youtube.com/",name="__Secure-MagdiFixture";
   ControllerCookies.phone().setCookie(origin,name+"=synthetic; Domain=.youtube.com; Path=/; Secure; HttpOnly; SameSite=Lax");ControllerCookies.toCar();
   boolean copied=androidx.webkit.CookieManagerCompat.getCookieInfo(CookieManager.getInstance(),origin).stream().anyMatch(c->c.startsWith(name+"=")&&c.toLowerCase(Locale.ROOT).contains("httponly")&&c.toLowerCase(Locale.ROOT).contains("secure"));
   ControllerCookies.phone().setCookie(origin,name+"=; Domain=.youtube.com; Path=/; Secure; Max-Age=0");ControllerCookies.toCar();
   boolean removed=androidx.webkit.CookieManagerCompat.getCookieInfo(CookieManager.getInstance(),origin).stream().noneMatch(c->c.startsWith(name+"="));return String.valueOf(copied&&removed);
  });}
 }
 private Button findButton(android.view.View view,String label){if(view instanceof Button&&label.contentEquals(((Button)view).getText()))return (Button)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=findButton(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 private String probeHtml(){try(InputStream in=getAssets().open("fixture.mp4");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);String base64=android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);
  return "<!doctype html><title>Native CSP Probe</title><script>window.violations=[];document.addEventListener('securitypolicyviolation',e=>violations.push({directive:e.effectiveDirective,uri:e.blockedURI}));window.addEventListener('DOMContentLoaded',()=>{const b='"+base64+"';const blob=new Blob([Uint8Array.from(atob(b),c=>c.charCodeAt(0))],{type:'video/mp4'});for(const src of ['https://m.youtube.com/fixture.mp4','data:video/mp4;base64,'+b,URL.createObjectURL(blob)]){let v=document.createElement('video');v.muted=true;v.autoplay=true;v.src=src;document.body.append(v);v.play().catch(()=>{});}});</script>";
 }catch(IOException e){return "<title>Probe failed</title>";}}
 private void fixture(WebView web,boolean isPhone){WebViewClient original=web.getWebViewClient();web.stopLoading();web.setWebViewClient(new WebViewClient(){
  public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return original.shouldOverrideUrlLoading(v,r);}
  public void onPageStarted(WebView v,String u,android.graphics.Bitmap icon){original.onPageStarted(v,u,icon);}
  public void onPageFinished(WebView v,String u){original.onPageFinished(v,u);}
  public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
   if(isPhone&&PhoneMediaPolicy.blocked(r.getUrl()))return PhoneMediaPolicy.denied();
   if(r.getUrl().getPath().equals("/fixture.mp4"))try{byte[] bytes;try(InputStream in=getAssets().open("fixture.mp4");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1)out.write(block,0,n);bytes=out.toByteArray();}
    String range=r.getRequestHeaders().get("Range");int start=0,end=bytes.length-1;if(range!=null&&range.startsWith("bytes=")){String[] parts=range.substring(6).split("-");start=Integer.parseInt(parts[0]);if(parts.length>1)end=Math.min(end,Integer.parseInt(parts[1]));}
    if(start>=bytes.length||end<start)return new WebResourceResponse("video/mp4",null,416,"Range Not Satisfiable",Collections.singletonMap("Content-Range","bytes */"+bytes.length),new ByteArrayInputStream(new byte[0]));
    Map<String,String> headers=new HashMap<>();headers.put("Accept-Ranges","bytes");headers.put("Content-Length",String.valueOf(end-start+1));if(range!=null)headers.put("Content-Range","bytes "+start+"-"+end+"/"+bytes.length);
    return new WebResourceResponse("video/mp4",null,range==null?200:206,range==null?"OK":"Partial Content",headers,new ByteArrayInputStream(bytes,start,end-start+1));}catch(IOException e){return PhoneMediaPolicy.denied();}
   if(!r.isForMainFrame())return PhoneMediaPolicy.denied();
   String id=r.getUrl().getQueryParameter("v");if("SSSSSSSSSSS".equals(id))try{Thread.sleep(1800);}catch(InterruptedException ignored){}
   String body="<!doctype html><meta name='viewport' content='width=device-width'><title>"+(isPhone?"Phone Controller":"Car Fixture")+"</title><style>body{background:#182030;color:white}a{display:block;padding:20px;color:cyan}</style><h3>Fixture browsing</h3><a href='/watch?v=AAAAAAAAAAA'>Video A</a><a href='/watch?v=BBBBBBBBBBB'>Video B</a><a href='/watch?v=SSSSSSSSSSS'>Slow video</a>";
   if(id!=null)body+="<script>history.replaceState({},\"\",location.pathname+location.search);</script>";
   if(id!=null)body+="<div id='player'><video id='video' controls loop preload='auto' src='https://m.youtube.com/fixture.mp4'></video></div><script>window.ytInitialPlayerResponse={videoDetails:{videoId:'"+id+"',title:'Fixture "+id+"',author:'Test'}};</script>";
   if(isPhone)return PhoneMediaPolicy.html(body);
   return new WebResourceResponse("text/html","UTF-8",new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
 });}
 protected void onResume(){super.onResume();if(phoneOpen)phone.resume();if(connected)car.resume();}
 protected void onPause(){phone.pause();car.pause();super.onPause();}
 protected void onDestroy(){phone.close();car.close();probe.destroy();super.onDestroy();}
}
