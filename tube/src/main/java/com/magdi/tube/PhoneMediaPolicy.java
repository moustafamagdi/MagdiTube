package com.magdi.tube;

import android.net.Uri;
import android.webkit.*;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.io.*;
import java.net.*;
import java.util.*;

/** Native phone boundary: renderer audio mute, media request refusal and response CSP.
 * CSP is attached by Android, independently of the page's JavaScript or player styling.
 * Failed document fetches fail closed; never retry with an unrestricted document.
 */
final class PhoneMediaPolicy {
 static final String CSP="media-src 'none'; object-src 'none'; frame-src https://accounts.google.com https://accounts.google.com.sa https://accounts.google.com.qa; worker-src 'none'";
 private static final Map<WebView,String> agents=Collections.synchronizedMap(new WeakHashMap<>());
 static boolean install(WebView web){
  agents.put(web,web.getSettings().getUserAgentString());
  web.getSettings().setMediaPlaybackRequiresUserGesture(true);
  web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
  if(!WebViewFeature.isFeatureSupported(WebViewFeature.MUTE_AUDIO)||!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))return false;
  WebViewCompat.setAudioMuted(web,true);
  try{WebViewCompat.addDocumentStartJavaScript(web,AdAssist.asset(web.getContext(),"phone_controller.js"),Collections.singleton("*"));return true;}
  catch(IOException e){return false;}
 }
 static boolean blocked(Uri u){
  String h=u.getHost(),p=u.getPath();if(h==null)return false;h=h.toLowerCase(Locale.ROOT);p=p==null?"":p.toLowerCase(Locale.ROOT);
  return h.equals("googlevideo.com")||h.endsWith(".googlevideo.com")||p.contains("/videoplayback")||p.contains("/youtubei/v1/player")||p.startsWith("/embed/")||p.endsWith("/sw.js")||p.matches(".*\\.(mp4|m4a|mp3|webm|m3u8|mpd|ogg|wav|aac)$");
 }
 static WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",403,"Controller only",Collections.singletonMap("Cache-Control","no-store"),new ByteArrayInputStream(new byte[0]));}
 static WebResourceResponse intercept(WebView web,WebResourceRequest request){
  if(blocked(request.getUrl()))return denied();
  String destination="";for(Map.Entry<String,String> header:request.getRequestHeaders().entrySet())if(header.getKey().equalsIgnoreCase("Sec-Fetch-Dest"))destination=header.getValue();
  if((!request.isForMainFrame()&&!"iframe".equals(destination))||!"GET".equals(request.getMethod()))return null;
  Uri uri=request.getUrl();if(!TubeView.allowed(uri)&&!GoogleAccountNavigation.allowed(uri))return denied();
  return document(uri,request.getRequestHeaders(),agents.get(web));
 }
 static WebResourceResponse document(Uri uri,Map<String,String> input,String agent){
  HttpURLConnection connection=null;
  try {
   connection=(HttpURLConnection)new URL(uri.toString()).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(20000);
   for(Map.Entry<String,String> h:input.entrySet())if(!h.getKey().equalsIgnoreCase("Cookie")&&!h.getKey().equalsIgnoreCase("Accept-Encoding")&&!h.getKey().equalsIgnoreCase("Host"))connection.setRequestProperty(h.getKey(),h.getValue());
   connection.setRequestProperty("User-Agent",agent);connection.setRequestProperty("Accept-Encoding","identity");
   String cookie=ControllerCookies.phone().getCookie(uri.toString());if(cookie!=null)connection.setRequestProperty("Cookie",cookie);
   int code=connection.getResponseCode();
   for(Map.Entry<String,List<String>> h:connection.getHeaderFields().entrySet())if("Set-Cookie".equalsIgnoreCase(h.getKey()))for(String v:h.getValue())ControllerCookies.phone().setCookie(uri.toString(),v);
   if(code>=300&&code<400){
    String location=connection.getHeaderField("Location");Uri target=Uri.parse(new URL(new URL(uri.toString()),location).toString());
    if(!TubeView.allowed(target)&&!GoogleAccountNavigation.allowed(target))return denied();
    // WebResourceResponse cannot represent a redirect. Preserve the browser origin using navigation.
    String html="<!doctype html><script>location.replace("+org.json.JSONObject.quote(target.toString())+");</script>";
    return html(html);
   }
   String type=connection.getContentType();if(type==null||!type.toLowerCase(Locale.ROOT).contains("text/html"))return denied();
   Map<String,String> headers=new HashMap<>();
   for(Map.Entry<String,List<String>> h:connection.getHeaderFields().entrySet())if(h.getKey()!=null&&!h.getKey().equalsIgnoreCase("Set-Cookie")&&!h.getKey().equalsIgnoreCase("Content-Length")&&!h.getKey().equalsIgnoreCase("Transfer-Encoding"))headers.put(h.getKey(),String.join(", ",h.getValue()));
   String existing=connection.getHeaderField("Content-Security-Policy");headers.keySet().removeIf(k->k.equalsIgnoreCase("Content-Security-Policy"));headers.put("Content-Security-Policy",(existing==null?"":existing+", ")+CSP);headers.put("Cache-Control","no-store");
   final HttpURLConnection owned=connection;InputStream stream=code>=400?connection.getErrorStream():connection.getInputStream();
   if(stream==null)return denied();connection=null;
   return new WebResourceResponse("text/html","UTF-8",code,"Document",headers,new FilterInputStream(stream){@Override public void close()throws IOException{try{super.close();}finally{owned.disconnect();}}});
  }catch(Exception e){return html("<!doctype html><p>Unable to load browsing page. Check your connection and tap Reload.</p>");}
  finally{if(connection!=null)connection.disconnect();}
 }
 static WebResourceResponse html(String body){Map<String,String> headers=new HashMap<>();headers.put("Content-Security-Policy",CSP);headers.put("Cache-Control","no-store");return new WebResourceResponse("text/html","UTF-8",200,"OK",headers,new ByteArrayInputStream(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
}
