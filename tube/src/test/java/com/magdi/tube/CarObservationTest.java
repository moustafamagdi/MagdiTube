package com.magdi.tube;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.json.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class CarObservationTest {
 @Test public void capturesOnlyClosedOwnWindowObservations()throws Exception {
  for(String state:"launch-tap|launch-pending|already-visible|start|resume|pause|stop|focus-gained|focus-lost|window-visible|window-hidden|fullscreen-on|fullscreen-off|fill-on|fill-off|user-marker".split("\\|")){JSONObject e=SupportPolicy.event("Car UI observation="+state+"; flags=31",100);assertNotNull(state,e);assertEquals("car-ui",e.getString("kind"));assertEquals(31,e.getInt("code"));}
  assertNull(SupportPolicy.event("Car UI observation=camera-active; flags=31",100));
  assertNull(SupportPolicy.event("Car UI observation=focus-lost; flags=32",100));
  assertNull(SupportPolicy.event("Car UI observation=focus-lost; flags=1; title=private",100));
 }
 @Test public void mediaSessionReportsContainNoTitleOrUrl()throws Exception {
  for(String state:new String[]{"session-active","session-inactive","none","playing","paused","stopped","buffering","error"})assertEquals(state,SupportPolicy.event("Media card observation="+state,100).getString("state"));
  assertNull(SupportPolicy.event("Media card observation=playing; url=private",100));
 }
}
