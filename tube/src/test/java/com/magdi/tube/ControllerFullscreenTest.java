package com.magdi.tube;

import android.app.Activity;
import android.view.*;
import android.webkit.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class ControllerFullscreenTest {
 private Activity activity; private TubeView car; private WebView web;
 @Before public void setup() throws Exception {
  activity=Robolectric.buildActivity(Activity.class).setup().get();
  car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));
  web=(WebView)field("web");car.resume();CarController.ready(car);
  assertEquals("accepted",CarController.report(car,CarController.epoch(),CarController.request(),new JSONObject()
   .put("token","").put("document","fixture-doc").put("id","AAAAAAAAAAA")
   .put("hasVideo",true).put("ready",4).put("playing",true).put("paused",false)));
 }
 @After public void cleanup(){car.close();activity.finish();}
 private Object field(String name)throws Exception {java.lang.reflect.Field f=TubeView.class.getDeclaredField(name);f.setAccessible(true);return f.get(car);}
 private ValueCallback<String> enter(){car.commandController("fullscreen",0,CarController.epoch(),CarController.request());return shadowOf(web).getLastEvaluatedJavascriptCallback();}
 @Test public void failedOrMissingAcknowledgementKeepsOriginalLayout()throws Exception {
  ValueCallback<String> result=enter();assertEquals(View.VISIBLE,car.getChildAt(0).getVisibility());assertEquals(View.VISIBLE,web.getVisibility());
  result.onReceiveValue("false");assertEquals(false,field("controllerFullscreen"));assertEquals(false,field("controllerFullscreenRequested"));
  result=enter();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1501));
  result.onReceiveValue("true");assertEquals(false,field("controllerFullscreen"));assertEquals(View.VISIBLE,car.getChildAt(0).getVisibility());
 }
 @Test public void successfulEntryKeepsWebSurfaceVisibleAndExitRestoresChrome()throws Exception {
  enter().onReceiveValue("true");assertEquals(true,field("controllerFullscreen"));assertEquals(View.VISIBLE,web.getVisibility());assertEquals(View.GONE,car.getChildAt(0).getVisibility());
  assertTrue(car.handleBack());assertEquals(false,field("controllerFullscreen"));assertEquals(View.VISIBLE,web.getVisibility());assertEquals(View.VISIBLE,car.getChildAt(0).getVisibility());
 }
 @Test public void rapidSecondTapInvalidatesFirstAcknowledgement()throws Exception {
  ValueCallback<String> result=enter();enter();result.onReceiveValue("true");assertEquals(false,field("controllerFullscreen"));assertEquals(false,field("controllerFullscreenRequested"));assertEquals(View.VISIBLE,car.getChildAt(0).getVisibility());
 }
 @Test public void stopDisconnectAndReplacementRejectLateEntry()throws Exception {
  for(int action=0;action<3;action++){
   ValueCallback<String> result=enter();
   if(action==0)CarController.stop(CarController.epoch());else if(action==1)car.pause();else CarController.select("https://m.youtube.com/watch?v=BBBBBBBBBBB",CarController.epoch());
   result.onReceiveValue("true");assertEquals(false,field("controllerFullscreen"));assertEquals(false,field("controllerFullscreenRequested"));assertEquals(View.VISIBLE,car.getChildAt(0).getVisibility());
   if(action<2){cleanup();setup();}
  }
 }
 @Test public void navigationClearsRemoteOverlay()throws Exception {
  enter().onReceiveValue("true");web.getWebViewClient().onPageStarted(web,"https://m.youtube.com/",null);
  assertEquals(false,field("controllerFullscreen"));assertEquals(View.VISIBLE,car.getChildAt(0).getVisibility());
 }
}
