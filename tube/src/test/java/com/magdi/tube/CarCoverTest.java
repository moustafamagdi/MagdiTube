package com.magdi.tube;
import android.app.Activity;
import android.view.*;
import android.webkit.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.json.JSONObject;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,shadows=CarCoverTest.SdkLifecycle.class)
public class CarCoverTest {
 @org.robolectric.annotation.Implements(com.google.android.apps.auto.sdk.CarActivity.class) public static class SdkLifecycle {@org.robolectric.annotation.Implementation protected void onStop(){}}
 private Activity activity;private TubeView page;private final TubeCarActivity host=new TubeCarActivity();private final Object service=new Object();
 @Before public void setup()throws Exception{CarPlaybackSession.connectionAvailable(service);activity=Robolectric.buildActivity(Activity.class).setup().get();page=CarPlaybackSession.attach(activity,host);page.resume();CarController.ready(page);java.lang.reflect.Field f=TubeCarActivity.class.getDeclaredField("page");f.setAccessible(true);f.set(host,page);}
 @After public void cleanup(){CarPlaybackSession.connectionLost(service);CarPlaybackSession.detach(host);CarPlaybackSession.stopDetached();activity.finish();}
 private WebView web()throws Exception{java.lang.reflect.Field f=TubeView.class.getDeclaredField("web");f.setAccessible(true);return (WebView)f.get(page);}
 private void select(){CarController.select("https://m.youtube.com/watch?v=AAAAAAAAAAA",CarController.epoch());}
 private void ready()throws Exception{shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(160));WebView w=web();w.loadUrl("https://m.youtube.com/watch?v=AAAAAAAAAAA");w.getWebViewClient().onPageFinished(w,w.getUrl());CarController.report(page,CarController.epoch(),CarController.request(),new JSONObject().put("token",CarController.epoch()+"_"+CarController.request()).put("document","fixture").put("id","AAAAAAAAAAA").put("hasVideo",true).put("ready",4).put("playing",true));}
 private void stopHost(){host.onStop();}
 @Test public void hostStopKeepsCurrentRequestAndDocument()throws Exception{select();ready();long e=CarController.epoch(),r=CarController.request();String url=shadowOf(web()).getLastLoadedUrl();stopHost();assertTrue(CarController.available());assertEquals(e,CarController.epoch());assertEquals(r,CarController.request());assertEquals(url,shadowOf(web()).getLastLoadedUrl());}

 @Test public void coverDuringLoadingPreservesPendingSelection()throws Exception{select();long r=CarController.request();stopHost();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(160));assertEquals(r,CarController.request());assertTrue(CarController.state().optBoolean("retry"));assertTrue(CarController.available());}
 @Test public void returnAfterCoverDoesNotNavigateOrReplaceFullscreen()throws Exception{select();ready();int[] exits={0};java.lang.reflect.Method show=TubeView.class.getDeclaredMethod("showFullscreen",View.class,WebChromeClient.CustomViewCallback.class);show.setAccessible(true);show.invoke(page,new View(activity),(WebChromeClient.CustomViewCallback)()->exits[0]++);String url=shadowOf(web()).getLastLoadedUrl();long r=CarController.request();stopHost();page.resume();assertEquals(0,exits[0]);assertEquals(r,CarController.request());assertEquals(url,shadowOf(web()).getLastLoadedUrl());}
 @Test public void hostRecreationInSameConnectionRetainsRequest()throws Exception{select();ready();long r=CarController.request();String url=shadowOf(web()).getLastLoadedUrl();CarPlaybackSession.detach(host);Object replacement=new Object();assertSame(page,CarPlaybackSession.attach(activity,replacement));page.resume();assertEquals(r,CarController.request());assertEquals(url,shadowOf(web()).getLastLoadedUrl());CarPlaybackSession.detach(host);assertTrue(CarController.available());CarPlaybackSession.detach(replacement);}
 @Test public void disconnectWhileCoveredCancelsAndReconnectNeverQueues()throws Exception{select();stopHost();CarPlaybackSession.connectionLost(service);assertFalse(CarController.available());assertFalse(CarController.state().optBoolean("retry"));shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));assertEquals("about:blank",shadowOf(web()).getLastLoadedUrl());CarPlaybackSession.connectionAvailable(service);page.resume();assertEquals("https://m.youtube.com/",shadowOf(web()).getLastLoadedUrl());assertFalse(CarController.state().optBoolean("retry"));}
 @Test public void stopWhileCoveredCannotResumePreviousVideo()throws Exception{select();ready();stopHost();assertTrue(CarController.stop(CarController.epoch()));long r=CarController.request();page.resume();shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1200));assertEquals(r,CarController.request());assertFalse(CarController.state().optBoolean("retry"));assertEquals("https://m.youtube.com/",shadowOf(web()).getLastLoadedUrl());}
 @Test public void previousServiceCannotCancelNewConnection(){Object replacement=new Object();CarPlaybackSession.connectionAvailable(replacement);page.resume();CarController.ready(page);select();long r=CarController.request();CarPlaybackSession.connectionLost(service);assertTrue(CarController.available());assertEquals(r,CarController.request());CarPlaybackSession.connectionLost(replacement);}
}
