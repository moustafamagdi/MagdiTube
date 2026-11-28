package com.magdi.tube;
import android.app.Application;
import android.content.SharedPreferences;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,application=Application.class)
public class BackgroundAudioTest {
 private Application app;private SharedPreferences prefs;
 @Before public void setup(){app=RuntimeEnvironment.getApplication();prefs=app.getSharedPreferences("tube",0);prefs.edit().clear().commit();}
 @Test public void newInstallDefaultsOn(){assertTrue(BackgroundAudio.enabled(app));}
 @Test public void upgradesOldDisabledSettingOnlyOnce(){prefs.edit().putBoolean("backgroundAudio",false).putString("unrelated","keep").commit();assertTrue(BackgroundAudio.enabled(app));assertEquals("keep",prefs.getString("unrelated",""));BackgroundAudio.setEnabled(app,false);assertFalse(BackgroundAudio.enabled(app));assertFalse(BackgroundAudio.enabled(app.getApplicationContext()));}
 @Test public void explicitChoiceBeforeFirstReadIsPreserved(){BackgroundAudio.setEnabled(app,false);assertFalse(BackgroundAudio.enabled(app));BackgroundAudio.setEnabled(app,true);assertTrue(BackgroundAudio.enabled(app));}
}
