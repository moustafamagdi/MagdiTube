package com.magdi.tube;
import android.app.Activity;
import android.widget.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class CarSeekControlsTest {
 @Test public void seekTracksObservedTimeAndDisablesForUnseekableMedia()throws Exception{
  Activity a=Robolectric.buildActivity(Activity.class).setup().get();try{String[] command={""};CarSeekControls c=new CarSeekControls(a,(action,value)->command[0]=action);SeekBar bar=(SeekBar)c.getChildAt(1);
   c.update(new JSONObject().put("seekable",true).put("duration",200).put("time",50));assertTrue(bar.isEnabled());assertEquals(250,bar.getProgress());((LinearLayout)c.getChildAt(0)).getChildAt(0).performClick();assertEquals("rewind",command[0]);
   c.update(new JSONObject().put("seekable",false));assertFalse(bar.isEnabled());
  }finally{a.finish();}
 }
 @Test public void timeLabelsIncludeHoursWhenNeeded(){assertEquals("1:30",CarSeekControls.clock(90));assertEquals("1:02:03",CarSeekControls.clock(3723));}
}
