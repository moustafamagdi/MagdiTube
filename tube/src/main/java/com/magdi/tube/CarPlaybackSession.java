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
 static TubeView attach(Context car,Object token){
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
  Diagnostics.event("Car host detached; playback session retained");
 }
 static void stopDetached(){if(host==null&&page!=null){TubeView old=page;page=null;context=null;old.close();Diagnostics.event("Detached car session released");}}
}
