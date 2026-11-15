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

 @Test public void temporaryCoverDoesNotExplicitlyPauseWebViewWhenBackgroundEnabled() throws Exception {
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  view.pause();assertFalse(shadowOf((WebView)field.get(view)).wasOnPauseCalled());
 }
 @Test public void keepAliveDoesNotTakeAudioFocusOrPausePlayer() throws Exception {
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);
  org.robolectric.shadows.ShadowWebView web=shadowOf((WebView)field.get(view));
  String before=web.getLastEvaluatedJavascript();
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
 private Button button(View root,String label){if(root instanceof Button&&label.contentEquals(((Button)root).getText()))return (Button)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){Button match=button(group.getChildAt(i),label);if(match!=null)return match;}}return null;}
}
