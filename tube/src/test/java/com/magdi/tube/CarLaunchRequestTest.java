package com.magdi.tube;
import android.content.*;import android.os.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.time.Duration;import static org.junit.Assert.*;import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class CarLaunchRequestTest {
 static class Driver implements CarLaunchRequest.Backend {CarLaunchRequest.Events events;Intent intent;int starts,closes;boolean denied;
  public void connect(CarLaunchRequest.Events e){events=e;}public void launch(Intent i){if(denied)throw new SecurityException("do not log this secret");starts++;intent=i;}public void close(){closes++;}
 }
 static class Host implements CarLaunchRequest.Host {boolean visible,warm;int starts;public boolean visible(){return visible;}public boolean launch(Intent i){if(warm)starts++;return warm;}}
 Driver driver;Host host;CarLaunchRequest request;int changes;
 @Before public void setup(){driver=new Driver();host=new Host();request=create(driver);}
 CarLaunchRequest create(Driver d){return new CarLaunchRequest(RuntimeEnvironment.getApplication(),TubeService.class,()->d,host,()->changes++);}
 void drain(){shadowOf(Looper.getMainLooper()).idle();}void advance(long ms){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 @After public void cleanup(){request.cancel();}
 @Test public void coldLaunchWaitsForConnectionAndActualVisibleHost(){request.request();assertTrue(request.pending());assertEquals(0,driver.starts);driver.events.connected();drain();assertEquals(1,driver.starts);assertEquals("opening",request.status());host.visible=true;advance(150);assertEquals("opened",request.status());assertEquals(1,driver.closes);assertFalse(request.pending());}
 @Test public void intentContainsOnlyOwnServiceAndNoMedia(){request.request();driver.events.connected();drain();assertEquals(TubeService.class.getName(),driver.intent.getComponent().getClassName());assertEquals(Intent.ACTION_MAIN,driver.intent.getAction());assertNull(driver.intent.getData());assertEquals(1,driver.intent.getExtras().size());assertEquals("com.magdi.tube",driver.intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME));}
 @Test public void missingConnectionTimesOutAndLateCallbackNeverLaunches(){request.request();advance(8000);assertEquals("timeout",request.status());driver.events.connected();drain();assertEquals(0,driver.starts);assertEquals(1,driver.closes);}
 @Test public void sentWithoutVisibleHostTimesOut(){request.request();driver.events.connected();drain();advance(8000);assertEquals("timeout",request.status());assertEquals(1,driver.starts);}
 @Test public void deniedLaunchIsNotRetried(){driver.denied=true;request.request();driver.events.connected();drain();assertEquals("denied",request.status());advance(16000);assertEquals(1,driver.closes);assertFalse(request.pending());}
 @Test public void disconnectCancelsAndReconnectCannotLaunch(){request.request();driver.events.disconnected();drain();driver.events.connected();drain();assertEquals("unavailable",request.status());assertEquals(0,driver.starts);}
 @Test public void cancellingPhoneOrStopPreventsLateLaunch(){request.request();request.cancel();driver.events.connected();drain();assertEquals("cancelled",request.status());assertEquals(0,driver.starts);}
 @Test public void rapidRequestsDoNotDuplicateLaunch(){request.request();request.request();driver.events.connected();driver.events.connected();drain();assertEquals(1,driver.starts);}
 @Test public void oldCallbacksCannotChangeNewAttempt(){request.request();CarLaunchRequest.Events old=driver.events;request.cancel();request.request();old.connected();old.disconnected();drain();assertTrue(request.pending());assertEquals(0,driver.starts);driver.events.connected();drain();assertEquals(1,driver.starts);}
 @Test public void warmHostDoesNotCreateColdConnection(){host.warm=true;request.request();assertEquals(1,host.starts);assertNull(driver.events);host.visible=true;advance(150);assertEquals("opened",request.status());}
 @Test public void coveredHostFallsBackOnceAndCancellationStopsFallback(){host.warm=true;request.request();advance(1200);assertNotNull(driver.events);driver.events.connected();drain();assertEquals(1,driver.starts);host.visible=true;advance(150);assertEquals("opened",request.status());host.visible=false;request.request();request.cancel();int before=driver.starts;advance(2000);assertEquals(before,driver.starts);}
 @Test public void alreadyVisibleHostNeedsNoLaunch(){host.visible=true;request.request();assertEquals("opened",request.status());assertFalse(request.pending());assertNull(driver.events);assertEquals(0,host.starts);}
 @Test public void openedStatusExpiresWhenHostIsCovered(){host.visible=true;request.request();assertEquals("opened",request.status());host.visible=false;assertEquals("idle",request.status());}
 @Test public void unavailableSdkFailsClosed(){request=new CarLaunchRequest(RuntimeEnvironment.getApplication(),TubeService.class,()->{throw new NoClassDefFoundError();},host,()->changes++);request.request();assertEquals("unavailable",request.status());assertFalse(request.pending());}
 @Test public void lastSelectionDispatchesOnceOnlyAfterHostAndPlayerAreReady(){
  java.util.concurrent.atomic.AtomicInteger delivered=new java.util.concurrent.atomic.AtomicInteger();
  request.request(()->{throw new AssertionError("Superseded selection");});
  java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();
  request.request(()->{if(!ready.get())return false;delivered.incrementAndGet();return true;});
  driver.events.connected();drain();host.visible=true;
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300));
  assertTrue(request.pending());assertEquals(0,delivered.get());
  ready.set(true);
  org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(10));
  assertEquals(1,delivered.get());assertFalse(request.pending());
 }
}
