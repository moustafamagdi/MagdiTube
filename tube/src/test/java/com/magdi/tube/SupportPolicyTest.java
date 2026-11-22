package com.magdi.tube;
import org.junit.*;import static org.junit.Assert.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.json.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35,application=android.app.Application.class)
public class SupportPolicyTest {
 @Test public void iptvMediaKeepsNumbersOnly(){JSONObject e=SupportPolicy.event("car media=seek-failed time=1823 ready=1 frames=40 engine=native code=0",100);assertNotNull(e);assertEquals(1823,e.optLong("time"));assertEquals("seek-failed",e.optString("state"));}
 @Test public void unknownAndSensitiveTextDropped(){for(String s:new String[]{"password=secret","Blocked navigation target=https://private.example","title=Secret title","car media=playing time=0 ready=4 frames=4 engine=native code=0 token=SECRET","provider account https://host/live/user/password"})assertNull(SupportPolicy.event(s,100));}
 @Test public void tubeSampleRebuildsKnownFields(){JSONObject e=SupportPolicy.event("Playback source=car instance=3 visible=true {\"playing\":true,\"time\":21,\"ready\":4,\"title\":\"SECRET\",\"url\":\"https://private\",\"performance\":{\"totalFrames\":125,\"droppedFrames\":3}}",100);assertNotNull(e);assertEquals(125,e.optLong("frames"));assertFalse(e.toString().contains("SECRET"));assertFalse(e.toString().contains("https"));}
 @Test public void installMetadataDoesNotLeakPackages(){JSONObject e=SupportPolicy.event("Install source [start]: installer=secret.package; metadata=MISMATCH",100);assertEquals("mismatch",e.optString("state"));assertFalse(e.toString().contains("secret"));}
 @Test public void controllerReasonsClosedVocabulary(){assertNotNull(SupportPolicy.event("Controller observation=accepted",100));assertNull(SupportPolicy.event("Controller observation=private-token",100));}
 @Test public void networkErrorHasNoExceptionMessage(){assertNotNull(SupportPolicy.event("Stream connection error=UnknownHostException",100));assertNull(SupportPolicy.event("Stream connection error=UnknownHostException: secret.example",100));}
 @Test public void malformedSampleIgnored(){assertNull(SupportPolicy.event("Playback source=car {bad}",100));}
}
