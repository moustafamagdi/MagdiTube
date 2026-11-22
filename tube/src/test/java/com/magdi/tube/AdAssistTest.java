package com.magdi.tube;

import android.content.Context;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class AdAssistTest {
 private Context context;
 @Before public void setup(){context=RuntimeEnvironment.getApplication();context.getSharedPreferences("tube",0).edit().clear().commit();}
 @After public void cleanup(){context.getSharedPreferences("tube",0).edit().clear().commit();}
 @Test public void missingPreferenceDefaultsOnWithoutOverwritingUserChoice(){
  assertTrue(AdAssist.enabled(context));
  AdAssist.toggle(context);assertFalse(AdAssist.enabled(context));
  assertFalse(AdAssist.enabled(context.createConfigurationContext(context.getResources().getConfiguration())));
  AdAssist.toggle(context);assertTrue(AdAssist.enabled(context));
 }
 @Test public void existingExplicitOffIsPreserved(){context.getSharedPreferences("tube",0).edit().putBoolean("adAssist",false).commit();assertFalse(AdAssist.enabled(context));}
}
