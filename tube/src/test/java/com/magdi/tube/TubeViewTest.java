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
 @Test public void transientAudioFocusResumesButPermanentLossStops() throws Exception {
  PlaybackService.keep(view);
  PlaybackService service=Robolectric.buildService(PlaybackService.class).create().get();
  try {
   service.onStartCommand(new android.content.Intent(),0,1);
   java.lang.reflect.Field focusField=PlaybackService.class.getDeclaredField("focus");focusField.setAccessible(true);
   android.media.AudioFocusRequest focus=(android.media.AudioFocusRequest)focusField.get(service);
   java.lang.reflect.Field webField=TubeView.class.getDeclaredField("web");webField.setAccessible(true);
   org.robolectric.shadows.ShadowWebView web=shadowOf((WebView)webField.get(view));
   shadowOf(service.getSystemService(android.media.AudioManager.class)).getLastAudioFocusRequest().listener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);
   assertTrue(web.getLastEvaluatedJavascript().contains("v.pause()"));
   shadowOf(service.getSystemService(android.media.AudioManager.class)).getLastAudioFocusRequest().listener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_GAIN);
   assertTrue(web.getLastEvaluatedJavascript().contains("v.play()"));
   shadowOf(service.getSystemService(android.media.AudioManager.class)).getLastAudioFocusRequest().listener.onAudioFocusChange(android.media.AudioManager.AUDIOFOCUS_LOSS);
   assertTrue(web.getLastEvaluatedJavascript().contains("v.pause()"));
  } finally {service.onDestroy();}
 }
 private Button button(View root,String label){if(root instanceof Button&&label.contentEquals(((Button)root).getText()))return (Button)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){Button match=button(group.getChildAt(i),label);if(match!=null)return match;}}return null;}
}
