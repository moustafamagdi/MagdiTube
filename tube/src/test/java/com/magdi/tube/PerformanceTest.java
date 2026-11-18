package com.magdi.tube;

import android.app.Activity;
import android.view.ContextThemeWrapper;
import android.webkit.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import java.util.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,shadows=PerformanceTest.ObservedWebView.class)
public class PerformanceTest {
 @Implements(WebView.class)
 public static class ObservedWebView extends org.robolectric.shadows.ShadowWebView {
  final List<String> scripts=new ArrayList<>();final List<ValueCallback<String>> samples=new ArrayList<>();
  @Implementation protected void evaluateJavascript(String script,ValueCallback<String> callback){super.evaluateJavascript(script,callback);scripts.add(script);if(script.startsWith("window.__magdiSample"))samples.add(callback);}
 }
 private Activity activity;private TubeView car,phone;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));phone=new TubeView(activity);car.resume();CarController.ready(car);}
 @After public void close(){car.close();phone.close();activity.finish();}
 private Object field(TubeView view,String name)throws Exception{java.lang.reflect.Field f=TubeView.class.getDeclaredField(name);f.setAccessible(true);return f.get(view);}
 private ObservedWebView web(TubeView v)throws Exception{return (ObservedWebView)shadowOf((WebView)field(v,"web"));}
 @Test public void slowJavascriptDoesNotAccumulateSampleRequests()throws Exception{
  Runnable sampler=(Runnable)field(car,"sample");sampler.run();sampler.run();sampler.run();assertEquals(1,web(car).samples.size());
  web(car).samples.get(0).onReceiveValue(org.json.JSONObject.quote("{\"playing\":false,\"time\":0,\"ready\":0,\"muted\":false,\"resumeWanted\":false}"));sampler.run();assertEquals(2,web(car).samples.size());
 }
 @Test public void staleSampleAfterResumeCannotSelectOrKeepOldPlayback()throws Exception{
  ((Runnable)field(car,"sample")).run();ValueCallback<String> old=web(car).samples.get(0);car.resume();
  old.onReceiveValue(org.json.JSONObject.quote("{\"playing\":true,\"time\":12,\"ready\":4,\"muted\":false,\"resumeWanted\":true}"));assertNull(MediaCardService.target());
 }
 @Test public void phoneNeverSamplesOrControlsCarEvenWhenClosed()throws Exception{
  phone.resume();String before=web(car).getLastEvaluatedJavascript();phone.pause();assertTrue(web(phone).wasOnPauseCalled());
  ((Runnable)field(phone,"sample")).run();assertEquals(0,web(phone).samples.size());
  phone.playFromMediaCard();assertEquals(before,web(car).getLastEvaluatedJavascript());assertTrue(CarController.available());
  phone.resume();assertFalse((Boolean)field(phone,"pendingResume"));assertEquals(0,web(phone).samples.size());
 }
 @Test public void repeatedFocusEventsProduceOneBoundedPictureRefreshBurst()throws Exception{
  activity.setContentView(car);car.resume();for(int i=0;i<20;i++)car.onWindowFocusChanged(true);
  shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1200));
  assertEquals(3,((Integer)field(car,"pictureRefreshCount")).intValue());
 }
}
