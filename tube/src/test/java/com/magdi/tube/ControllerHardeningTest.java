package com.magdi.tube;

import android.app.Activity;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class ControllerHardeningTest {
 private Activity activity;private CarControllerTest.Player player;private PhoneController phone;
 private static final String A="https://m.youtube.com/watch?v=AAAAAAAAAAA",B="https://m.youtube.com/watch?v=BBBBBBBBBBB";
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();player=new CarControllerTest.Player();CarController.attach(player);CarController.ready(player);CarController.select(A,CarController.epoch());phone=new PhoneController(activity,false);}
 @After public void cleanup(){CarController.detach(player);activity.finish();}
 private JSONObject observation()throws Exception{return new JSONObject().put("token",player.e+"_"+player.r).put("document","fixture-document").put("id",player.id).put("hasVideo",true).put("ready",4).put("playing",true).put("paused",false).put("ended",false).put("error",false).put("seekable",true).put("duration",60).put("replay",true);}
 private void report(JSONObject p){CarController.report(player,player.e,player.r,p);phone.update();}
 private Button button(View root,String name){if(root instanceof Button&&name.contentEquals(((Button)root).getText()))return (Button)root;if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){Button b=button(((ViewGroup)root).getChildAt(i),name);if(b!=null)return b;}return null;}
 @Test public void actualButtonsDisableWhenReportsStop()throws Exception{report(observation());assertTrue(button(phone,"Pause").isEnabled());shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(7));phone.update();assertFalse(button(phone,"Pause").isEnabled());assertFalse(button(phone,"Play").isEnabled());}
 @Test public void mediaErrorDisablesActualTransport()throws Exception{report(observation().put("error",true));assertEquals("error",CarController.state().optString("phase"));assertFalse(button(phone,"Pause").isEnabled());assertFalse(button(phone,"Play").isEnabled());}
 @Test public void bufferingIsNotPaused()throws Exception{report(observation().put("ready",2).put("buffering",true));assertEquals("buffering",CarController.state().optString("phase"));assertFalse(button(phone,"Play").isEnabled());assertTrue(button(phone,"Pause").isEnabled());}
 @Test public void endedNeedsObservedReplaySupport()throws Exception{report(observation().put("playing",false).put("paused",true).put("ended",true).put("replay",false));assertEquals("ended",CarController.state().optString("phase"));assertFalse(button(phone,"Play").isEnabled());report(observation().put("playing",false).put("paused",true).put("ended",true));assertTrue(button(phone,"Play").isEnabled());assertFalse(button(phone,"Pause").isEnabled());}
 @Test public void displayedSelectionCannotPauseReplacement()throws Exception{report(observation());CarController.select(B,CarController.epoch());CarController.report(player,player.e,player.r,observation());button(phone,"Pause").performClick();assertEquals(0,player.commands);}
 @Test public void repeatedLoadingSelectionDoesNotCancelStart(){long r=CarController.request();CarController.select(A,CarController.epoch());assertEquals(r,CarController.request());assertEquals(1,player.selected);}
 @Test public void commandsDoNotOptimisticallyChangeButtons()throws Exception{report(observation());button(phone,"Pause").performClick();assertEquals(1,player.commands);assertTrue(button(phone,"Pause").isEnabled());assertFalse(button(phone,"Play").isEnabled());report(observation().put("playing",false).put("paused",true));assertFalse(button(phone,"Pause").isEnabled());assertTrue(button(phone,"Play").isEnabled());assertEquals(0.38f,button(phone,"Pause").getAlpha(),0.01f);}
 @Test public void rejectedReportsCannotRenewFreshness()throws Exception{report(observation());shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(4));assertEquals("document-mismatch",CarController.report(player,player.e,player.r,observation().put("token","old")));shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(3));phone.update();assertFalse(button(phone,"Pause").isEnabled());assertEquals("stale",CarController.state().optString("phase"));}
 @Test public void currentUnreadyDocumentPublishesNeutralLoading()throws Exception{assertEquals("current-loading",CarController.report(player,player.e,player.r,observation().put("id","BBBBBBBBBBB")));phone.update();assertTrue(CarController.fresh());assertEquals("loading",CarController.state().optString("phase"));assertFalse(CarController.state().getJSONObject("playback").optBoolean("playing"));assertFalse(button(phone,"Pause").isEnabled());report(observation());assertTrue(button(phone,"Pause").isEnabled());}
 @Test public void seekGestureCannotSeekReplacement()throws Exception{report(observation());SeekBar seek=(SeekBar)phone.getChildAt(2);java.lang.reflect.Field listener=SeekBar.class.getDeclaredField("mOnSeekBarChangeListener");listener.setAccessible(true);SeekBar.OnSeekBarChangeListener gesture=(SeekBar.OnSeekBarChangeListener)listener.get(seek);gesture.onStartTrackingTouch(seek);CarController.select(B,CarController.epoch());report(observation());seek.setProgress(500);gesture.onStopTrackingTouch(seek);assertEquals(0,player.commands);}
 @Test public void stopIsConnectionWideButCannotCrossReconnect()throws Exception{report(observation());long e=player.e,r=player.r,o=CarController.owner();CarController.select(B,e);assertTrue(CarController.command("stop",0,e,r,o));assertFalse(CarController.state().optBoolean("retry"));CarController.detach(player);CarController.attach(player);CarController.ready(player);CarController.select(B,CarController.epoch());assertFalse(CarController.command("stop",0,e,r,o));assertTrue(CarController.state().optBoolean("retry"));}
 private Object field(Object target,String name)throws Exception{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 private void pending(TubeView v,boolean value)throws Exception{java.lang.reflect.Field f=TubeView.class.getDeclaredField("pendingResume");f.setAccessible(true);f.setBoolean(v,value);}
 @Test public void rejectedDocumentDoesNotChangeResumeOrMediaConsumers()throws Exception{
  TubeView car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));try{car.resume();CarController.ready(car);pending(car,true);((Runnable)field(car,"sample")).run();
   org.robolectric.shadows.ShadowWebView web=shadowOf((WebView)field(car,"web"));
   JSONObject sample=new JSONObject().put("playing",true).put("ready",4).put("resumeWanted",false).put("controller",observation().put("token","obsolete"));
   web.getLastEvaluatedJavascriptCallback().onReceiveValue(JSONObject.quote(sample.toString()));assertEquals(true,field(car,"pendingResume"));assertNull(MediaCardService.target());
   for(Class<?> type:new Class<?>[]{PlaybackService.class,PlaybackCoordinator.class}){java.lang.reflect.Field f=type.getDeclaredField(type==PlaybackService.class?"players":"pages");f.setAccessible(true);assertFalse(Boolean.TRUE.equals(((java.util.Map<?,?>)f.get(null)).get(car)));}
  }finally{car.close();}
 }
 @Test public void lateCoverCannotRestoreResumeAfterExplicitPause()throws Exception{
  TubeView car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));try{car.resume();CarController.ready(car);CarController.report(car,CarController.epoch(),CarController.request(),observation().put("token",""));
   java.lang.reflect.Method cover=TubeView.class.getDeclaredMethod("coverPlayback");cover.setAccessible(true);cover.invoke(car);ValueCallback<String> callback=shadowOf((WebView)field(car,"web")).getLastEvaluatedJavascriptCallback();
   car.commandController("pause",0,CarController.epoch(),CarController.request());callback.onReceiveValue("true");assertEquals(false,field(car,"pendingResume"));
  }finally{car.close();}
 }
 @Test public void lateCoverCannotRestoreAfterStopReplacementOrDisconnect()throws Exception{
  for(int action=0;action<3;action++){TubeView car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));try{car.resume();CarController.ready(car);CarController.report(car,CarController.epoch(),CarController.request(),observation().put("token",""));java.lang.reflect.Method cover=TubeView.class.getDeclaredMethod("coverPlayback");cover.setAccessible(true);cover.invoke(car);ValueCallback<String> callback=shadowOf((WebView)field(car,"web")).getLastEvaluatedJavascriptCallback();if(action==0)CarController.stop(CarController.epoch());else if(action==1)CarController.select(B,CarController.epoch());else car.pause();callback.onReceiveValue("true");assertEquals(false,field(car,"pendingResume"));}finally{car.close();}}
 }
}
