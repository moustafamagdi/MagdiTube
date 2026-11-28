package com.magdi.tube;
import android.content.Context;
import org.json.JSONObject;

/** Once per observed car video. Never re-enter after a manual fullscreen choice. */
final class StartFullscreen {
 private String handled="";
 static boolean enabled(Context c){return c.getSharedPreferences("tube",0).getBoolean("startFullscreen",false);}
 static void setEnabled(Context c,boolean enabled){c.getSharedPreferences("tube",0).edit().putBoolean("startFullscreen",enabled).apply();}
 private String key(JSONObject state){if(state==null||state.optString("document").isEmpty()||state.optString("id").isEmpty())return "";return state.optString("token")+"/"+state.optString("document")+"/"+state.optString("id");}
 void manual(JSONObject state){String key=key(state);if(!key.isEmpty())handled=key;}
 boolean shouldStart(boolean enabled,boolean visible,JSONObject state){
  String key=key(state);if(key.isEmpty()||key.equals(handled)||!visible||!state.optBoolean("hasVideo")||state.optInt("ready")<3||!state.optBoolean("playing")||state.optBoolean("paused")||state.optBoolean("ended")||state.optBoolean("error"))return false;
  handled=key;return enabled&&!state.optBoolean("fullscreen");
 }
}
