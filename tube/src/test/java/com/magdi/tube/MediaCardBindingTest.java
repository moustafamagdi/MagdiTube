package com.magdi.tube;
import android.content.*;
import android.os.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class MediaCardBindingTest {
 private final MediaCardBinding binding=new MediaCardBinding();
 private FakeContext context;
 static class FakeContext extends ContextWrapper {
  int binds,unbinds;boolean allowed=true;Intent intent;ServiceConnection latest;
  FakeContext(Context c){super(c);}
  public Context getApplicationContext(){return this;}
  public boolean bindService(Intent i,ServiceConnection c,int flags){assertEquals(Context.BIND_AUTO_CREATE,flags);binds++;intent=i;latest=c;return allowed;}
  public void unbindService(ServiceConnection c){unbinds++;}
 }
 @Before public void setup(){context=new FakeContext(RuntimeEnvironment.getApplication());}
 @After public void cleanup(){binding.release();}
 private void advance(){shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(11));}
 @Test public void oneExplicitBindingNoRepeatedStarts(){binding.ensure(context);binding.ensure(context);assertEquals(1,context.binds);assertEquals(MediaCardService.class.getName(),context.intent.getComponent().getClassName());assertEquals("android.media.browse.MediaBrowserService",context.intent.getAction());context.latest.onServiceConnected(context.intent.getComponent(),new Binder());advance();binding.ensure(context);assertEquals(1,context.binds);}
 @Test public void failedBindingRetriesAreBoundedAndNewSelectionCanRetry(){context.allowed=false;for(int i=0;i<6;i++){binding.ensure(context);advance();}assertEquals(3,context.binds);binding.release();binding.ensure(context);assertEquals(4,context.binds);}
 @Test public void missingCallbackTimesOutAndOldCallbackCannotAffectReplacement(){binding.ensure(context);ServiceConnection old=context.latest;advance();binding.ensure(context);assertEquals(2,context.binds);old.onBindingDied(context.intent.getComponent());old.onServiceConnected(context.intent.getComponent(),new Binder());binding.ensure(context);assertEquals(2,context.binds);advance();binding.ensure(context);assertEquals(3,context.binds);}
 @Test public void releaseCancelsPendingConnectionAndIgnoresLateCallback(){binding.ensure(context);ServiceConnection old=context.latest;binding.release();int unbinds=context.unbinds;old.onServiceConnected(context.intent.getComponent(),new Binder());old.onNullBinding(context.intent.getComponent());assertEquals(unbinds,context.unbinds);advance();assertEquals(1,context.binds);}
 @Test public void suspendedConnectionCanReconnectWithoutPlaybackCommands(){binding.ensure(context);ServiceConnection current=context.latest;current.onServiceConnected(context.intent.getComponent(),new Binder());current.onServiceDisconnected(context.intent.getComponent());binding.ensure(context);assertEquals(1,context.binds);current.onServiceConnected(context.intent.getComponent(),new Binder());advance();binding.ensure(context);assertEquals(1,context.binds);}
}

