package com.magdi.tube;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;

/** One car document survives replacement of the host activity. */
final class CarPlaybackSession {
 private static TubeView page;
 private static MutableContextWrapper context;
 private static Object host;
 private static long detachedAt;
 private static boolean hasCarSession;
 static TubeView attach(Context car,Object token){
  if(!hasCarSession||(host==null&&android.os.SystemClock.elapsedRealtime()-detachedAt>=300000)){Diagnostics.startSession("Car");hasCarSession=true;}
  if(page==null){context=new MutableContextWrapper(car);page=new TubeView(context);Diagnostics.event("Car playback session created");}
  else {context.setBaseContext(car);Diagnostics.event("Car playback session reused");}
  if(page.getParent() instanceof ViewGroup)((ViewGroup)page.getParent()).removeView(page);
  host=token;return page;
 }
 static boolean isHost(Object token){return host==token;}
 static void detach(Object token){
  if(host!=token||page==null)return;
  page.prepareDetach();
  if(page.getParent() instanceof ViewGroup)((ViewGroup)page.getParent()).removeView(page);
  Context app=context.getApplicationContext();context.setBaseContext(new ContextThemeWrapper(app,android.R.style.Theme_Material_NoActionBar));host=null;
  detachedAt=android.os.SystemClock.elapsedRealtime();
  Diagnostics.event("Car host detached; playback session retained");
 }
 static boolean reset(TubeView requester){
  if(page!=requester||!(host instanceof TubeCarActivity))return false;
  TubeCarActivity activity=(TubeCarActivity)host;
  Diagnostics.event("Car view reset requested by user; rebuilding WebView");
  TubeView old=page;page=null;context=null;
  if(old.getParent() instanceof ViewGroup)((ViewGroup)old.getParent()).removeView(old);
  old.close();activity.replacePage();return true;
 }
 static void stopDetached(){if(host==null&&page!=null){TubeView old=page;page=null;context=null;old.close();Diagnostics.event("Detached car session released");}}
}
