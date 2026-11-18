package com.magdi.tube;

import org.json.JSONObject;

/** Main-thread only. A connection lease and request generation guard every async result. */
final class CarController {
 interface Player {
  boolean isCarView();
  void cancelController();
  void selectController(VideoSelection video,long epoch,long request);
  void commandController(String action,double value,long epoch,long request);
 }
 private static Player car;
 private static long epoch,request;
 private static boolean ready;
 private static VideoSelection selection;
 private static JSONObject playback=new JSONObject();
 private static String phase="disconnected";
 static long epoch(){return epoch;}
 static long request(){return request;}
 static boolean available(){return car!=null&&ready;}
 static boolean accepts(Player view,long e,long r){return car==view&&ready&&epoch==e&&request==r;}
 static void attach(Player view){
  if(!view.isCarView())return;
  if(car==view)return;
  if(car!=null)detach(car);
  car=view;epoch++;request++;ready=false;selection=null;playback=new JSONObject();phase="connecting";
 }
 static void ready(Player view){if(car==view){ready=true;if("connecting".equals(phase))phase="idle";}}
 static void detach(Player view){
  if(car!=view)return;
  car=null;ready=false;epoch++;request++;selection=null;playback=new JSONObject();phase="disconnected";
  view.cancelController();Diagnostics.event("Controller disconnected; pending requests cancelled");
 }
 static boolean select(String url,long expectedEpoch){
  VideoSelection next=VideoSelection.parse(url);
  if(!available()||expectedEpoch!=epoch||next==null)return false;
  selection=next;request++;playback=new JSONObject();phase="sending";
  car.selectController(next,epoch,request);Diagnostics.event("Controller selection accepted; content not logged");return true;
 }
 static boolean command(String action,double value,long expectedEpoch){
  if(!available()||expectedEpoch!=epoch)return false;
  if("stop".equals(action)){request++;selection=null;playback=new JSONObject();phase="idle";car.cancelController();return true;}
  if("retry".equals(action))return selection!=null&&select(selection.url,expectedEpoch);
  if(!java.util.Arrays.asList("play","pause","next","previous","seek","forward","rewind","fullscreen").contains(action)||!Double.isFinite(value))return false;
  if(("next".equals(action)||"previous".equals(action))&&!playback.optBoolean(action))return false;
  if(!playback.optBoolean("hasVideo")||playback.optInt("ready")==0)return false;
  car.commandController(action,value,epoch,request);return true;
 }
 static void loading(Player view,long e,long r){if(accepts(view,e,r))phase="loading";}
 static void failed(Player view,long e,long r){if(accepts(view,e,r))phase="error";}
 static void report(Player view,long e,long r,JSONObject state){
  if(!accepts(view,e,r))return;
  // A previous document cannot acknowledge a new selection (including selecting A, B, A).
  if(selection!=null&&!state.optString("token").equals(e+"_"+r))return;
  playback=state;
  if(!state.optBoolean("hasVideo")&&("playing".equals(phase)||"paused".equals(phase)))phase="idle";
  if(state.optInt("ready")>0&&state.optBoolean("hasVideo"))phase=state.optBoolean("playing")?"playing":"paused";
 }
 static JSONObject state(){
  JSONObject out=new JSONObject();try{out.put("epoch",epoch).put("request",request).put("connected",available()).put("phase",phase).put("retry",selection!=null).put("playback",new JSONObject(playback.toString()));}catch(org.json.JSONException ignored){}return out;
 }
}
