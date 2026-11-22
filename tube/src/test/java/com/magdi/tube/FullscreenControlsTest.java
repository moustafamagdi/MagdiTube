package com.magdi.tube;
import android.app.Activity;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.time.Duration;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class FullscreenControlsTest {
 private Activity activity;private ImageButton exit,fit;private FullscreenControls controls;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();FrameLayout root=new FrameLayout(activity);exit=new ImageButton(activity);fit=new ImageButton(activity);root.addView(exit);root.addView(fit);activity.setContentView(root);controls=new FullscreenControls(new Handler(Looper.getMainLooper()));controls.bind(exit,fit);}
 @After public void cleanup(){controls.clear();activity.finish();}
 private void advance(long ms){shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ms));}
 private boolean touch(int action){MotionEvent e=MotionEvent.obtain(0,0,action,10,10,0);try{return controls.onTouch(e);}finally{e.recycle();}}
 @Test public void bothButtonsHideAtFiveSecondsEvenWithAutomaticFocus(){exit.requestFocus();advance(4999);assertEquals(View.VISIBLE,exit.getVisibility());advance(1);assertEquals(View.INVISIBLE,exit.getVisibility());assertEquals(View.INVISIBLE,fit.getVisibility());}
 @Test public void hiddenGestureOnlyRevealsAndCannotClickThrough(){advance(5000);assertTrue(touch(MotionEvent.ACTION_DOWN));assertEquals(View.VISIBLE,exit.getVisibility());advance(6000);assertEquals(View.VISIBLE,fit.getVisibility());assertTrue(touch(MotionEvent.ACTION_MOVE));assertTrue(touch(MotionEvent.ACTION_UP));advance(4999);assertEquals(View.VISIBLE,exit.getVisibility());advance(1);assertEquals(View.INVISIBLE,exit.getVisibility());}
 @Test public void visibleTouchPassesThroughAndRestartsTimerAfterRelease(){advance(4000);assertFalse(touch(MotionEvent.ACTION_DOWN));advance(6000);assertEquals(View.VISIBLE,fit.getVisibility());assertFalse(touch(MotionEvent.ACTION_CANCEL));advance(5000);assertEquals(View.INVISIBLE,fit.getVisibility());}
 @Test public void keyboardFocusRemainsAccessibleUntilTouch(){controls.onKey();exit.requestFocus();advance(6000);assertEquals(View.VISIBLE,exit.getVisibility());touch(MotionEvent.ACTION_DOWN);touch(MotionEvent.ACTION_UP);advance(5000);assertEquals(View.INVISIBLE,exit.getVisibility());}
 @Test public void rebindCancelsOldTimerAndClearStopsIntercepting(){advance(4000);ImageButton nextExit=new ImageButton(activity),nextFit=new ImageButton(activity);controls.bind(nextExit,nextFit);advance(1000);assertEquals(View.VISIBLE,nextExit.getVisibility());advance(4000);assertEquals(View.INVISIBLE,nextFit.getVisibility());controls.clear();assertFalse(touch(MotionEvent.ACTION_DOWN));advance(6000);assertEquals(View.VISIBLE,exit.getVisibility());}
}
