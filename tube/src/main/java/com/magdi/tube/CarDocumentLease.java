package com.magdi.tube;

import android.webkit.WebView;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import java.util.*;

/** Bind each new document before YouTube scripts run, independently of URL rewriting. */
final class CarDocumentLease {
 private static final Map<WebView,ScriptHandler> scripts=new WeakHashMap<>();
 static boolean install(WebView web,String token){
  release(web);
  if(!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))return false;
  try{
   String js="if(window===window.top&&!Object.prototype.hasOwnProperty.call(window,'__magdiCarLease'))Object.defineProperty(window,'__magdiCarLease',{value:"+org.json.JSONObject.quote(token)+",writable:false,configurable:false});";
   scripts.put(web,WebViewCompat.addDocumentStartJavaScript(web,js,new HashSet<>(Arrays.asList("https://youtube.com","https://www.youtube.com","https://m.youtube.com"))));return true;
  }catch(RuntimeException e){Diagnostics.event("Car document binding unavailable");return false;}
 }
 static void release(WebView web){ScriptHandler old=scripts.remove(web);if(old!=null)old.remove();}
}
