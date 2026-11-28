package com.magdi.tube;
import android.app.Application;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,application=Application.class)
public class StartFullscreenTest {
 private JSONObject video()throws Exception{return new JSONObject().put("token","lease").put("document","doc").put("id","one").put("hasVideo",true).put("ready",4).put("playing",true);}
 @Test public void defaultOnAndExplicitOptOutIsPreserved(){Application c=RuntimeEnvironment.getApplication();c.getSharedPreferences("tube",0).edit().clear().commit();assertTrue(StartFullscreen.enabled(c));StartFullscreen.setEnabled(c,true);assertTrue(StartFullscreen.enabled(c));StartFullscreen.setEnabled(c,false);assertFalse(StartFullscreen.enabled(c));}
 @Test public void entersOnceAndNextVideoCanStart()throws Exception{StartFullscreen p=new StartFullscreen();JSONObject s=video();assertTrue(p.shouldStart(true,true,s));assertFalse(p.shouldStart(true,true,s));assertTrue(p.shouldStart(true,true,s.put("id","two")));}
 @Test public void disabledDoesNotChangeCurrentVideoWhenEnabledLater()throws Exception{StartFullscreen p=new StartFullscreen();JSONObject s=video();assertFalse(p.shouldStart(false,true,s));assertFalse(p.shouldStart(true,true,s));assertTrue(p.shouldStart(true,true,s.put("id","two")));}
 @Test public void waitsForVisibleReadyPlaybackAndRejectsFeedPreviews()throws Exception{StartFullscreen p=new StartFullscreen();JSONObject s=video();assertFalse(p.shouldStart(true,false,s));assertFalse(p.shouldStart(true,true,s.put("id","")));s.put("id","one");assertFalse(p.shouldStart(true,true,s.put("ready",0)));s.put("ready",4);assertFalse(p.shouldStart(true,true,s.put("paused",true)));s.put("paused",false);assertFalse(p.shouldStart(true,true,s.put("error",true)));s.put("error",false);assertTrue(p.shouldStart(true,true,s));}
 @Test public void manualExitAndExistingFullscreenAreRespected()throws Exception{StartFullscreen p=new StartFullscreen();JSONObject s=video();p.manual(s);assertFalse(p.shouldStart(true,true,s));s.put("id","two").put("fullscreen",true);assertFalse(p.shouldStart(true,true,s));s.put("fullscreen",false);assertFalse(p.shouldStart(true,true,s));assertTrue(p.shouldStart(true,true,s.put("id","three")));}
}
