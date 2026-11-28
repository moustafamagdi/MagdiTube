package com.magdi.tube;
import android.app.Activity;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.webkit.WebChromeClient;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class FullscreenToggleTest {
 private Activity activity;private TubeView car;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));}
 @After public void cleanup(){car.close();activity.finish();}
 private Field field(String name)throws Exception{Field f=TubeView.class.getDeclaredField(name);f.setAccessible(true);return f;}
 private void request(WebChromeClient.CustomViewCallback callback)throws Exception{Method m=TubeView.class.getDeclaredMethod("showFullscreen",View.class,WebChromeClient.CustomViewCallback.class);m.setAccessible(true);m.invoke(car,new View(activity),callback);}
 @Test public void youtubeRequestExitsInlineFullscreenWithoutOpeningSecondLayer()throws Exception{
  field("controllerFullscreen").setBoolean(car,true);field("controllerFullscreenRequested").setBoolean(car,true);int[] hidden={0};request(()->hidden[0]++);
  assertEquals(1,hidden[0]);assertFalse(field("controllerFullscreen").getBoolean(car));assertFalse(field("controllerFullscreenRequested").getBoolean(car));assertNull(field("fullscreen").get(car));
 }
 @Test public void normalYoutubeRequestStillEntersNativeFullscreen()throws Exception{
  int[] hidden={0};request(()->hidden[0]++);assertEquals(0,hidden[0]);assertNotNull(field("fullscreen").get(car));
 }
}
