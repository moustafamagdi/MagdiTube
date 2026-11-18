package com.magdi.tube;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32)
public class TubeViewTest {
 private Activity activity;
 private TubeView view;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();view=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));}
 @After public void cleanup(){view.close();activity.finish();}
 @Test public void fullscreenBackRestoresOriginalContentAndInvokesCallbackOnce() throws Exception {
  View original=view.getChildAt(0);int[] calls={0};
  java.lang.reflect.Method show=TubeView.class.getDeclaredMethod("showFullscreen",View.class,WebChromeClient.CustomViewCallback.class);show.setAccessible(true);
  show.invoke(view,new View(activity),(WebChromeClient.CustomViewCallback)()->calls[0]++);
  assertEquals(View.GONE,original.getVisibility());
  assertTrue(view.handleBack());assertEquals(View.VISIBLE,original.getVisibility());assertEquals(1,calls[0]);
 }
 @Test public void carKeyboardSubmitsEncodedSearchWithoutSystemIme() throws Exception {
  button(view,"Search").performClick();
  button(view,"a").performClick();button(view,"Space").performClick();button(view,"b").performClick();button(view,"Go").performClick();
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  assertEquals("https://m.youtube.com/results?search_query=a%20b",shadowOf((WebView)field.get(view)).getLastLoadedUrl());
  assertEquals(View.VISIBLE,view.getChildAt(0).getVisibility());
 }
 @Test public void searchBackRestoresToolbar(){button(view,"Search").performClick();assertTrue(view.handleBack());assertEquals(View.VISIBLE,view.getChildAt(0).getVisibility());}
 @Test public void rejectedUnreadySampleReleasesSlotWithoutReleasingNewerSample()throws Exception{
  CarController.attach(view);long epoch=CarController.epoch(),request=CarController.request();
  java.lang.reflect.Field generation=TubeView.class.getDeclaredField("sampleGeneration"),inFlight=TubeView.class.getDeclaredField("sampleInFlight");
  generation.setAccessible(true);inFlight.setAccessible(true);generation.setLong(view,7);inFlight.setBoolean(view,true);
  java.lang.reflect.Method accept=TubeView.class.getDeclaredMethod("acceptSample",long.class,long.class,long.class);accept.setAccessible(true);
  assertEquals(false,accept.invoke(view,7L,epoch,request));assertFalse(inFlight.getBoolean(view));
  CarController.ready(view);generation.setLong(view,8);inFlight.setBoolean(view,true);
  assertEquals(false,accept.invoke(view,7L,epoch,request));assertTrue(inFlight.getBoolean(view));
  assertEquals(true,accept.invoke(view,8L,epoch,request));assertFalse(inFlight.getBoolean(view));
 }

 @Test public void closingCarStopsEvenWhenBackgroundEnabled() throws Exception {
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  view.resume();CarController.ready(view);view.pause();assertTrue(shadowOf((WebView)field.get(view)).wasOnPauseCalled());assertEquals("about:blank",shadowOf((WebView)field.get(view)).getLastLoadedUrl());assertFalse(CarController.available());
 }
 @Test public void keepAliveDoesNotTakeAudioFocusOrPausePlayer() throws Exception {
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  org.robolectric.shadows.ShadowWebView web=shadowOf((WebView)field.get(view));
  view.resume();CarController.ready(view);shadowOf(android.os.Looper.getMainLooper()).idle();String before=web.getLastEvaluatedJavascript();
  PlaybackService.keep(view);
  PlaybackService service=Robolectric.buildService(PlaybackService.class).create().get();
  try {
   for(int i=0;i<3;i++){service.onStartCommand(new android.content.Intent(),0,i);PlaybackService.keep(view);}
   assertNull(shadowOf(service.getSystemService(android.media.AudioManager.class)).getLastAudioFocusRequest());
   assertEquals(before,web.getLastEvaluatedJavascript());
   service.onStartCommand(new android.content.Intent().setAction("stop"),0,4);
   assertTrue(web.getLastEvaluatedJavascript().contains("v.pause()"));
  } finally {service.onDestroy();}
 }
 @Test public void secondViewRegistrationDoesNotPauseFirstView() throws Exception {
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  org.robolectric.shadows.ShadowWebView first=shadowOf((WebView)field.get(view));String before=first.getLastEvaluatedJavascript();
  TubeView second=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));
  try{PlaybackService.keep(view);PlaybackService.keep(second);assertEquals(before,first.getLastEvaluatedJavascript());}
  finally{second.close();}
 }

 @Test public void carSessionSurvivesHostReplacement(){
  Object first=new Object(),second=new Object();
  TubeView retained=CarPlaybackSession.attach(activity,first);
  CarPlaybackSession.detach(first);
  assertSame(retained,CarPlaybackSession.attach(activity,second));
  CarPlaybackSession.detach(first); // An old host must not detach the new one.
  assertSame(retained,CarPlaybackSession.attach(activity,second));
  CarPlaybackSession.detach(second);CarPlaybackSession.stopDetached();
 }

 @Test public void pictureRefreshDoesNotPauseSeekOrReloadMedia() throws Exception {
  activity.setContentView(view);view.resume();
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  org.robolectric.shadows.ShadowWebView web=shadowOf((WebView)field.get(view));String url=web.getLastLoadedUrl();
  java.lang.reflect.Method refresh=TubeView.class.getDeclaredMethod("refreshVideoSurface");refresh.setAccessible(true);refresh.invoke(view);
  String js=web.getLastEvaluatedJavascript();assertTrue(js.contains("resize"));assertFalse(js.contains(".pause("));assertFalse(js.contains(".play("));assertFalse(js.contains("currentTime="));assertEquals(url,web.getLastLoadedUrl());
  view.pause();String before=web.getLastEvaluatedJavascript();refresh.invoke(view);assertEquals(before,web.getLastEvaluatedJavascript());
 }

 @Test public void fullscreenCoveragePreservesCustomVideoVisibility() throws Exception {
  activity.setContentView(view);view.resume();
  java.lang.reflect.Method show=TubeView.class.getDeclaredMethod("showFullscreen",View.class,WebChromeClient.CustomViewCallback.class);show.setAccessible(true);
  int[] delivered={-1};View video=new View(activity){protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);delivered[0]=visibility;}};
  show.invoke(view,video,(WebChromeClient.CustomViewCallback)()->{});
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("fullscreen");field.setAccessible(true);
  View frame=(View)field.get(view);frame.dispatchWindowVisibilityChanged(View.GONE);
  assertEquals(View.VISIBLE,delivered[0]);
  java.lang.reflect.Field webField=TubeView.class.getDeclaredField("web");webField.setAccessible(true);
  assertTrue(shadowOf((WebView)webField.get(view)).getLastEvaluatedJavascript().contains(".cover()"));
 }
 @Test public void fullscreenOverlayKeepsWebViewportSizedAcrossHostResizeAndExit()throws Exception{
  activity.setContentView(view);view.resume();measure(800,480);
  java.lang.reflect.Field webField=TubeView.class.getDeclaredField("web");webField.setAccessible(true);WebView web=(WebView)webField.get(view);
  int normalHeight=web.getMeasuredHeight();assertTrue(normalHeight>0);
  java.lang.reflect.Method show=TubeView.class.getDeclaredMethod("showFullscreen",View.class,WebChromeClient.CustomViewCallback.class);show.setAccessible(true);
  show.invoke(view,new View(activity),(WebChromeClient.CustomViewCallback)()->{});measure(800,480);
  assertEquals(View.INVISIBLE,web.getVisibility());assertEquals(800,web.getMeasuredWidth());assertEquals(480,web.getMeasuredHeight());
  // Progress callbacks must not bring the hidden toolbar strip into fullscreen layout.
  shadowOf(web).getWebChromeClient().onProgressChanged(web,25);measure(1024,600);
  assertEquals(1024,web.getMeasuredWidth());assertEquals(600,web.getMeasuredHeight());
  view.pause();view.resume();measure(1024,600);assertEquals(View.VISIBLE,web.getVisibility());assertTrue(web.getMeasuredHeight()<600);
  view.handleBack();measure(800,480);assertEquals(View.VISIBLE,web.getVisibility());assertEquals(normalHeight,web.getMeasuredHeight());
 }
 @Test public void searchOverlayAlsoPreservesWebViewportAndRestoresIt(){
  measure(800,480);button(view,"Search").performClick();measure(800,480);
  ViewGroup content=(ViewGroup)view.getChildAt(view.getChildCount()-1);View web=content.getChildAt(0);
  assertEquals(View.INVISIBLE,web.getVisibility());assertEquals(800,web.getMeasuredWidth());assertEquals(480,web.getMeasuredHeight());
  view.handleBack();measure(800,480);assertEquals(View.VISIBLE,web.getVisibility());assertTrue(web.getMeasuredHeight()>0);
 }
 private void measure(int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
 private Button button(View root,String label){if(root instanceof Button&&label.contentEquals(((Button)root).getText()))return (Button)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){Button match=button(group.getChildAt(i),label);if(match!=null)return match;}}return null;}
}
