package com.magdi.tube;
import android.app.Activity;import android.content.Intent;import android.os.Looper;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.*;import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32,shadows=CarLaunchHostTest.Sdk.class)
public class CarLaunchHostTest {
 @Implements(com.google.android.apps.auto.sdk.CarActivity.class) public static class Sdk {
  static Intent launched;
  @Implementation protected void onStart(){} @Implementation protected void onStop(){}
  @Implementation protected void startCarActivity(Intent intent){launched=intent;}
 }
 Activity context;TubeCarActivity host;TubeView page;Object connection=new Object();
 @Before public void setup()throws Exception{Sdk.launched=null;context=Robolectric.buildActivity(Activity.class).setup().get();host=new TubeCarActivity();CarPlaybackSession.connectionAvailable(connection);page=CarPlaybackSession.attach(context,host);java.lang.reflect.Field field=TubeCarActivity.class.getDeclaredField("page");field.setAccessible(true);field.set(host,page);}
 @After public void cleanup(){CarPlaybackSession.connectionLost(connection);CarPlaybackSession.detach(host);CarPlaybackSession.stopDetached();context.finish();}
 @Test public void existingHostDispatchesAndOnlyActualStartConfirms(){CarLaunchRequest request=new CarLaunchRequest(context,TubeService.class,()->{throw new AssertionError("Warm launch must not use first-party manager");},new CarLaunchRequest.Host(){public boolean visible(){return CarPlaybackSession.visible();}public boolean launch(Intent intent){return CarPlaybackSession.show(intent);}},()->{});request.request();assertNotNull(Sdk.launched);assertEquals(TubeService.class.getName(),Sdk.launched.getComponent().getClassName());assertEquals("opening",request.status());host.onStart();shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(150));assertEquals("opened",request.status());host.onStop();assertEquals("idle",request.status());}
 @Test public void oldHostCannotMakeReplacementVisibleAndDisconnectClearsVisibility(){host.onStart();assertTrue(CarPlaybackSession.visible());Object replacement=new Object();CarPlaybackSession.attach(context,replacement);host.onStart();assertFalse(CarPlaybackSession.visible());CarPlaybackSession.visibility(replacement,true);assertTrue(CarPlaybackSession.visible());host.onStop();assertTrue(CarPlaybackSession.visible());CarPlaybackSession.connectionLost(connection);assertFalse(CarPlaybackSession.visible());CarPlaybackSession.detach(replacement);}
}
