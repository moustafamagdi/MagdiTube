package com.magdi.tube;

import android.webkit.CookieManager;
import android.webkit.WebView;
import androidx.webkit.*;
import java.util.*;

/** Phone browsing has its own profile, so old/default-profile workers cannot bypass its policy.
 * Copy full cookie attributes through native APIs; never expose cookie values to page JS or logs.
 * Only YouTube cookies are synchronized to the car. Google sign-in stays in the phone profile.
 */
final class ControllerCookies {
 private static volatile CookieManager phone;
 private static final String[] ORIGINS={"https://youtube.com/","https://www.youtube.com/","https://m.youtube.com/"};
 static boolean prepare(WebView web){
  try {
  if(!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)||!WebViewFeature.isFeatureSupported(WebViewFeature.GET_COOKIE_INFO))return false;
  WebViewCompat.setProfile(web,"magdi-phone-controller-v1");Profile profile=WebViewCompat.getProfile(web);phone=profile.getCookieManager();
  profile.getServiceWorkerController().getServiceWorkerWebSettings().setBlockNetworkLoads(true);
  profile.getServiceWorkerController().setServiceWorkerClient(new android.webkit.ServiceWorkerClient(){public android.webkit.WebResourceResponse shouldInterceptRequest(android.webkit.WebResourceRequest r){return PhoneMediaPolicy.denied();}});
  android.content.SharedPreferences prefs=web.getContext().getSharedPreferences("tube",0);
  if(!prefs.getBoolean("controllerCookiesMigrated",false)){copy(CookieManager.getInstance(),phone);prefs.edit().putBoolean("controllerCookiesMigrated",true).apply();}
  return true;
  }catch(RuntimeException e){Diagnostics.event("Controller profile unavailable; phone browsing disabled");return false;}
 }
 static CookieManager phone(){return phone;}
 static boolean toCar(){try{CookieManager source=phone;if(source!=null)copy(source,CookieManager.getInstance());return true;}catch(RuntimeException e){Diagnostics.event("Controller cookie synchronization unavailable; selection not loaded");return false;}}
 static void flush(){CookieManager p=phone;if(p!=null)p.flush();CookieManager.getInstance().flush();}
 private static String key(String cookie){String[] parts=cookie.split(";");String name=parts[0].split("=",2)[0].trim();String domain="",path="/";for(int i=1;i<parts.length;i++){String part=parts[i].trim();if(part.toLowerCase(Locale.ROOT).startsWith("domain="))domain=part.substring(7).toLowerCase(Locale.ROOT);if(part.toLowerCase(Locale.ROOT).startsWith("path="))path=part.substring(5);}return name+"|"+domain+"|"+path;}
 private static void copy(CookieManager source,CookieManager destination){
  for(String origin:ORIGINS){List<String> current=CookieManagerCompat.getCookieInfo(source,origin);Set<String> keys=new HashSet<>();for(String cookie:current)keys.add(key(cookie));
   for(String old:CookieManagerCompat.getCookieInfo(destination,origin))if(!keys.contains(key(old)))destination.setCookie(origin,old+"; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
   for(String cookie:current)destination.setCookie(origin,cookie);
  }
 }
}
