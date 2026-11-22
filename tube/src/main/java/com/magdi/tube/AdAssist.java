package com.magdi.tube;
import android.content.Context;
import android.webkit.WebView;
import androidx.webkit.*;
import java.util.*;
final class AdAssist {
 private static final Map<WebView,ScriptHandler> scripts=new WeakHashMap<>();
 private static final Map<String,String> assets=new HashMap<>();
 static boolean enabled(Context c){return c.getSharedPreferences("tube",0).getBoolean("adAssist",true);}
 static void toggle(Context c){boolean value=!enabled(c);c.getSharedPreferences("tube",0).edit().putBoolean("adAssist",value).apply();Diagnostics.event("Ad assist enabled="+value);}
 static synchronized String asset(Context c,String name)throws java.io.IOException{String cached=assets.get(name);if(cached!=null)return cached;try(java.io.InputStream in=c.getAssets().open(name);java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);String value=out.toString("UTF-8");assets.put(name,value);return value;}}
 static void configure(WebView web){
 ScriptHandler old=scripts.remove(web);if(old!=null)old.remove();
 if(!enabled(web.getContext()))return;
 if(WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))try{scripts.put(web,WebViewCompat.addDocumentStartJavaScript(web,asset(web.getContext(),"player_filter.js"),new HashSet<>(Arrays.asList("https://youtube.com","https://www.youtube.com","https://m.youtube.com"))));Diagnostics.event("Player filter registered at document start");}catch(java.io.IOException|RuntimeException e){Diagnostics.event("Player filter registration failed: "+e.getClass().getSimpleName());}
 else Diagnostics.event("Document-start filtering unsupported; only ad assistance available");
 }
 static void release(WebView web){ScriptHandler old=scripts.remove(web);if(old!=null)old.remove();}
 static void apply(WebView web){if(!enabled(web.getContext())){web.evaluateJavascript("if(window.__magdiAds)window.__magdiAds.stop();if(window.__magdiPlayerFilter)window.__magdiPlayerFilter.stop();",null);return;}try{web.evaluateJavascript(asset(web.getContext(),"ad_assist.js"),null);web.evaluateJavascript("window.__magdiPlayerFilter?JSON.stringify(window.__magdiPlayerFilter.stats()):'filter-not-active'",v->Diagnostics.event("Player filter status "+v));}catch(java.io.IOException e){Diagnostics.event("Ad assist asset load failed");}}
}
