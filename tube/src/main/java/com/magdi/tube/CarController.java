package com.magdi.tube;
import org.json.JSONObject;

/** Main-thread only. Connection, selection and observed document own every action. */
final class CarController {
 interface Player {
  boolean isCarView();
  void cancelController();
  default void stopController(){cancelController();}
  void selectController(VideoSelection video,long epoch,long request);
  void commandController(String action,double value,long epoch,long request);
 }
 static final long FRESH_MS=6500;
 private static Player car;
 private static long epoch,request,owner,lastReport=-1;
 private static boolean ready,selectionAcknowledged;
 private static VideoSelection selection;
 private static JSONObject playback=new JSONObject();
 private static String phase="disconnected";
 static long epoch(){return epoch;}
 static long request(){return request;}
 static long owner(){return owner;}
 static boolean available(){return car!=null&&ready;}
 static boolean fresh(){return available()&&lastReport>=0&&android.os.SystemClock.elapsedRealtime()-lastReport<FRESH_MS;}
 static boolean accepts(Player view,long e,long r){return car==view&&ready&&epoch==e&&request==r;}
 private static void clear(){playback=new JSONObject();lastReport=-1;owner++;}
 static void attach(Player view){
  if(!view.isCarView()||car==view)return;
  if(car!=null)detach(car);
  car=view;epoch++;request++;ready=false;selection=null;clear();phase="connecting";
 }
 static void ready(Player view){if(car==view){ready=true;if("connecting".equals(phase))phase="idle";}}
 static void detach(Player view){
  if(car!=view)return;
  car=null;ready=false;epoch++;request++;selection=null;clear();phase="disconnected";
  view.cancelController();Diagnostics.event("Controller disconnected; pending requests cancelled");
 }
 static boolean select(String url,long expectedEpoch){
  VideoSelection next=VideoSelection.parse(url);
  if(!available()||expectedEpoch!=epoch||next==null)return false;
  // Repeated taps must not keep unloading an in-progress start.
  if(selection!=null&&selection.url.equals(next.url)&&("sending".equals(phase)||"loading".equals(phase)))return true;
  selection=next;selectionAcknowledged=false;request++;clear();phase="sending";
  car.selectController(next,epoch,request);Diagnostics.event("Controller selection accepted; content not logged");return true;
 }
 // Stop is connection-wide, including when issued from an older selection display.
 static boolean stop(long expectedEpoch){
  if(!available()||epoch!=expectedEpoch||!canStop())return false;
  request++;selection=null;clear();phase="idle";car.stopController();return true;
 }
 static boolean command(String action,double value,long expectedEpoch,long expectedRequest,long expectedOwner){
  if(!available()||expectedEpoch!=epoch)return false;
  if("stop".equals(action))return stop(expectedEpoch);
  if(expectedRequest!=request||expectedOwner!=owner||!Double.isFinite(value))return false;
  if("retry".equals(action)){if(selection==null)return false;String url=selection.url;phase="error";return select(url,expectedEpoch);}
  if(!can(action)||("fill".equals(action)&&value!=0&&value!=1))return false;
  car.commandController(action,value,epoch,request);return true;
 }
 static boolean canStop(){return available()&&(selection!=null||(fresh()&&playback.optBoolean("hasVideo")&&playback.optInt("ready")>0&&!playback.optString("id").isEmpty()));}
 static boolean can(String action){
  if(!fresh()||!playback.optBoolean("hasVideo")||playback.optInt("ready")==0||"error".equals(phase)||"loading".equals(phase))return false;
  switch(action){
   case "play":return "paused".equals(phase)||("ended".equals(phase)&&playback.optBoolean("replay"));
   case "pause":return "playing".equals(phase)||"buffering".equals(phase);
   case "seek":case "forward":case "rewind":return playback.optBoolean("seekable");
   case "next":case "previous":return playback.optBoolean(action);
   case "fullscreen":return true;
   case "fill":return playback.optBoolean("fullscreen");
   default:return false;
  }
 }
 static void loading(Player view,long e,long r){if(accepts(view,e,r))phase="loading";}
 static void failed(Player view,long e,long r){if(accepts(view,e,r)){clear();phase="error";}}
 static void navigating(Player view){if(car==view){clear();phase=selection==null?"idle":"loading";}}
 static String report(Player view,long e,long r,JSONObject state){
  if(!accepts(view,e,r))return "stale-connection";
  if(!state.optString("token").equals(selection==null?"":e+"_"+r))return "document-mismatch";
  if(state.optString("document").isEmpty())return "document-unbound";
  boolean awaiting=selection!=null&&!selectionAcknowledged&&(!selection.id.equals(state.optString("id"))||!state.optBoolean("hasVideo")||state.optInt("ready")==0);
  // Current loading documents are legitimate, but their old/unready player is not.
  JSONObject next;
  try{next=awaiting?new JSONObject().put("token",state.optString("token")).put("document",state.optString("document")):new JSONObject(state.toString());}
  catch(org.json.JSONException ignored){return "invalid-report";}
  if(!next.optString("document").equals(playback.optString("document"))||!next.optString("id").equals(playback.optString("id")))owner++;
  playback=next;lastReport=android.os.SystemClock.elapsedRealtime();
  if(awaiting){phase=state.optBoolean("error")?"error":"loading";return "current-loading";}
  if(selection!=null)selectionAcknowledged=true;
  if(state.optBoolean("error"))phase="error";
  else if(!state.optBoolean("hasVideo")||state.optInt("ready")==0)phase=selection==null?"idle":"loading";
  else if(state.optBoolean("ended"))phase="ended";
  else if(state.optBoolean("paused"))phase="paused";
  else if(state.optBoolean("buffering")||state.optInt("ready")<3)phase="buffering";
  else if(state.optBoolean("playing"))phase="playing";
  else phase="loading";
  return "accepted";
 }
 static JSONObject state(){JSONObject out=new JSONObject();try{out.put("epoch",epoch).put("request",request).put("owner",owner).put("connected",available()).put("fresh",fresh()).put("phase",lastReport>=0&&!fresh()?"stale":phase).put("retry",selection!=null).put("playback",new JSONObject(playback.toString()));}catch(org.json.JSONException ignored){}return out;}
}
