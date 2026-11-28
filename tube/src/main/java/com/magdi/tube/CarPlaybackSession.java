package com.magdi.tube;

import android.content.Context;
import android.content.MutableContextWrapper;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;

/** Keep a covered/recreated host in its bound car session; unbind always cancels playback. */
final class CarPlaybackSession {
 private static TubeView page;
 private static MutableContextWrapper context;
 private static Object host,connection;
 private static boolean visible;
 private static boolean disconnectedPage;
 static int observationFlags(){return (connected()?1:0)|(visible()?2:0)|(page!=null&&page.hasWindowFocus()?4:0)|(page!=null&&page.getWindowVisibility()==android.view.View.VISIBLE?8:0)|(page!=null&&page.isAttachedToWindow()?16:0);}
 static boolean visible(){return connected()&&host!=null&&visible;}
 static void visibility(Object token,boolean value){if(host==token)visible=value;}
 static boolean show(android.content.Intent intent){if(!connected()||!(host instanceof TubeCarActivity))return false;((TubeCarActivity)host).startCarActivity(intent);return true;}
 private static long detachedAt;
 private static boolean hasCarSession;
 static TubeView attach(Context car,Object token){
  if(!hasCarSession||(host==null&&android.os.SystemClock.elapsedRealtime()-detachedAt>=300000)){Diagnostics.startSession("Car");hasCarSession=true;}
  // A covered host is reusable, but a disconnected WebView can retain the
  // phone display's renderer scale. Recreate only on the next real attachment.
  if(disconnectedPage&&page!=null){
   if(page.getParent() instanceof ViewGroup)((ViewGroup)page.getParent()).removeView(page);
   page.close();page=null;context=null;host=null;
   Diagnostics.event("Car display recreated after disconnection");
  }
  disconnectedPage=false;
  if(page==null){context=new MutableContextWrapper(car);page=new TubeView(context);Diagnostics.event("Car playback session created");}
  else {if(host!=token){if(connected())page.retainCoveredHost();else page.pause();}context.setBaseContext(car);Diagnostics.event("Car playback session reused");}
  if(page.getParent() instanceof ViewGroup)((ViewGroup)page.getParent()).removeView(page);
  if(host!=token)visible=false;host=token;return page;
 }
 static boolean connected(){return connection!=null;}
 static void connectionAvailable(Object service){if(connection!=null&&connection!=service&&page!=null){page.pause();disconnectedPage=true;}if(connection!=service)visible=false;connection=service;}
 static void connectionLost(Object service){if(connection!=service)return;connection=null;visible=false;CarUiDiagnostics.reset();disconnectedPage=true;if(page!=null)page.pause();Diagnostics.event("Car service disconnected; playback cancelled");}
 static boolean isHost(Object token){return host==token;}
 static void detach(Object token){
  if(host!=token||page==null)return;
  if(connected())page.retainCoveredHost();else page.prepareDetach();
  if(page.getParent() instanceof ViewGroup)((ViewGroup)page.getParent()).removeView(page);
  Context app=context.getApplicationContext();context.setBaseContext(new ContextThemeWrapper(app,android.R.style.Theme_Material_NoActionBar));host=null;visible=false;
  detachedAt=android.os.SystemClock.elapsedRealtime();
  Diagnostics.event(connected()?"Car host covered/detached; connected session retained":"Car host detached; playback cancelled");
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
