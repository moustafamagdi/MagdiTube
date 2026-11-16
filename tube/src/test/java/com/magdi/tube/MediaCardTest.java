package com.magdi.tube;

import android.app.Activity;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;
import android.support.v4.media.session.PlaybackStateCompat;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32,shadows=MediaCardTest.CapturedSession.class)
public class MediaCardTest {
 // Robolectric does not round-trip platform session state through a controller.
 // Capture the actual state submitted to the platform; real AA binding is a device test.
 @org.robolectric.annotation.Implements(android.media.session.MediaSession.class)
 public static class CapturedSession extends org.robolectric.shadows.ShadowMediaSession {
  static android.media.session.PlaybackState published;
  static android.media.MediaMetadata metadata;
  @org.robolectric.annotation.Implementation protected void setMetadata(android.media.MediaMetadata value){metadata=value;}
  @org.robolectric.annotation.Implementation protected void setPlaybackState(android.media.session.PlaybackState state){published=state;}
 }
 private Activity activity;private TubeView car,phone;private MediaCardService service;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));phone=new TubeView(activity);service=Robolectric.buildService(MediaCardService.class).create().get();}
 @After public void cleanup(){service.onDestroy();car.close();phone.close();activity.finish();}
 private org.robolectric.shadows.ShadowWebView web(TubeView view)throws Exception{java.lang.reflect.Field f=TubeView.class.getDeclaredField("web");f.setAccessible(true);return shadowOf((WebView)f.get(view));}
 private PlaybackStateCompat state(){return PlaybackStateCompat.fromPlaybackState(CapturedSession.published);}
 @Test public void connectionAndObservationDoNotStartPauseOrRequestAudioFocus()throws Exception{
  String before=web(car).getLastEvaluatedJavascript();MediaCardService.observe(car,true,false,true,32000);
  assertEquals(before,web(car).getLastEvaluatedJavascript());assertNotNull(service.getSessionToken());
  assertNull(shadowOf(service.getSystemService(android.media.AudioManager.class)).getLastAudioFocusRequest());
  assertEquals(PlaybackStateCompat.STATE_PLAYING,state().getState());
 }
 @Test public void pauseKeepsSameTargetAndPlayControlsExistingDocument()throws Exception{
  MediaCardService.observe(car,true,false,true,32000);String url=web(car).getLastLoadedUrl();
  service.command(false);assertTrue(web(car).getLastEvaluatedJavascript().contains("v.pause()"));
  assertSame(car,MediaCardService.target());assertTrue(MediaCardService.retains(car));assertEquals(PlaybackStateCompat.STATE_PAUSED,state().getState());
  service.command(true);assertTrue(web(car).getLastEvaluatedJavascript().contains("v.play()"));assertEquals(url,web(car).getLastLoadedUrl());
  assertNull(shadowOf(service.getSystemService(android.media.AudioManager.class)).getLastAudioFocusRequest());
 }
 @Test public void mutedPhonePreviewDoesNotStealCarControls()throws Exception{
  MediaCardService.observe(phone,true,true,true,1000);assertNull(MediaCardService.target());
  MediaCardService.observe(phone,true,false,true,2000);assertSame(phone,MediaCardService.target());
  MediaCardService.observe(car,true,false,true,5000);assertSame(car,MediaCardService.target());
  MediaCardService.observe(phone,true,true,true,3000);service.command(false);
  assertSame(car,MediaCardService.target());assertTrue(web(car).getLastEvaluatedJavascript().contains("v.pause()"));assertFalse(String.valueOf(web(phone).getLastEvaluatedJavascript()).contains("v.pause()"));
 }
 @Test public void noVideoOrDestroyedTargetDoesNotAdvertisePlayableState()throws Exception{
  String before=web(car).getLastEvaluatedJavascript();service.command(true);assertEquals(before,web(car).getLastEvaluatedJavascript());assertEquals(PlaybackStateCompat.STATE_NONE,state().getState());
  MediaCardService.observe(car,true,false,true,5000);MediaCardService.forget(car);assertNull(MediaCardService.target());assertEquals(0,state().getActions());
 }
 @Test public void browserRejectsUntrustedCaller(){assertNull(service.onGetRoot("attacker",123456,null));assertNull(service.onGetRoot("com.google.android.projection.gearhead",123456,null));assertNotNull(service.onGetRoot(activity.getPackageName(),android.os.Process.myUid(),null));}
 private static final String FIRST="abcdefghijk",SECOND="12345678901";
 private ThumbnailCache.Art art(String id)throws Exception{return ThumbnailCache.Art.parse(new org.json.JSONObject().put("id",id).put("url","https://i.ytimg.com/vi/"+id+"/hqdefault.jpg"));}
 private String icon(){return CapturedSession.metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI);}
 private FakeThumbnails fake()throws Exception{
  java.lang.reflect.Field field=MediaCardService.class.getDeclaredField("thumbnails");field.setAccessible(true);((ThumbnailCache)field.get(service)).close();FakeThumbnails fake=new FakeThumbnails(activity);field.set(service,fake);return fake;
 }
 static class FakeThumbnails extends ThumbnailCache {
  final java.util.List<Result> callbacks=new java.util.ArrayList<>();
  FakeThumbnails(android.content.Context c){super(c);}
  @Override void load(Art art,Result result){callbacks.add(result);}
 }
 @Test public void delayedPreviousThumbnailCannotOverwriteCurrentVideo()throws Exception{
  FakeThumbnails loader=fake();MediaCardService.observe(car,true,false,true,1000,art(FIRST));
  MediaCardService.observe(car,true,false,true,2000,art(SECOND));assertEquals(2,loader.callbacks.size());
  android.net.Uri first=android.net.Uri.parse("content://com.magdi.tube.reports/artwork/first.jpg"),second=android.net.Uri.parse("content://com.magdi.tube.reports/artwork/second.jpg");
  loader.callbacks.get(1).loaded(second);assertEquals(second.toString(),icon());
  loader.callbacks.get(0).loaded(first);assertEquals(second.toString(),icon());
  MediaCardService.observe(phone,true,true,true,5000,art(FIRST));assertEquals(second.toString(),icon());assertEquals(2,loader.callbacks.size());
 }
 @Test public void thumbnailIsReusedAcrossSamplesAndPauseAndClearedOnNavigation()throws Exception{
  FakeThumbnails loader=fake();MediaCardService.observe(car,true,false,true,1000,art(FIRST));
  android.net.Uri image=android.net.Uri.parse("content://com.magdi.tube.reports/artwork/first.jpg");loader.callbacks.get(0).loaded(image);
  MediaCardService.observe(car,true,false,true,2000,art(FIRST));service.command(false);
  assertEquals(1,loader.callbacks.size());assertEquals(image.toString(),icon());assertEquals(PlaybackStateCompat.STATE_PAUSED,state().getState());
  MediaCardService.clearArtwork(car);assertTrue(icon().startsWith("android.resource://"));
 }
 @Test public void failedThumbnailUsesAppIconWithoutRepeatedDownloads()throws Exception{
  FakeThumbnails loader=fake();MediaCardService.observe(car,true,false,true,1000,art(FIRST));loader.callbacks.get(0).loaded(null);
  for(int i=0;i<10;i++)MediaCardService.observe(car,true,false,true,2000+i*2000,art(FIRST));
  assertTrue(icon().startsWith("android.resource://"));assertEquals(1,loader.callbacks.size());assertEquals(PlaybackStateCompat.STATE_PLAYING,state().getState());
 }
}
