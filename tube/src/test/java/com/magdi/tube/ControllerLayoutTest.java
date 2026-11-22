package com.magdi.tube;
import android.app.Activity;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class ControllerLayoutTest {
 private Activity activity;private TubeView car;
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup().get();car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));car.resume();CarController.ready(car);}
 @After public void cleanup(){car.close();activity.finish();}
 private Button button(View v,String label){if(v instanceof Button&&label.contentEquals(((Button)v).getText()))return (Button)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button b=button(((ViewGroup)v).getChildAt(i),label);if(b!=null)return b;}return null;}
 private WebView web()throws Exception{java.lang.reflect.Field f=TubeView.class.getDeclaredField("web");f.setAccessible(true);return (WebView)f.get(car);}
 @Test public void idleStopIsDimmedAndDoesNotBlankHome()throws Exception{PhoneController phone=new PhoneController(activity,false);phone.update();assertFalse(button(phone,"Stop").isEnabled());assertEquals(0.38f,button(phone,"Stop").getAlpha(),0.01f);String before=shadowOf(web()).getLastLoadedUrl();long r=CarController.request();assertFalse(CarController.stop(CarController.epoch()));assertEquals(r,CarController.request());assertEquals(before,shadowOf(web()).getLastLoadedUrl());}
 @Test public void loadingStopReturnsHomeAndInvalidatesDelayedStart()throws Exception{CarController.select("https://m.youtube.com/watch?v=AAAAAAAAAAA",CarController.epoch());PhoneController phone=new PhoneController(activity,false);assertTrue(button(phone,"Stop").isEnabled());button(phone,"Stop").performClick();assertEquals("https://m.youtube.com/",shadowOf(web()).getLastLoadedUrl());shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));assertEquals("https://m.youtube.com/",shadowOf(web()).getLastLoadedUrl());assertFalse(button(phone,"Stop").isEnabled());assertFalse(CarController.stop(CarController.epoch()));}
 @Test public void disconnectDoesNotLoadHomeOrQueuePlayback()throws Exception{CarController.select("https://m.youtube.com/watch?v=AAAAAAAAAAA",CarController.epoch());car.pause();assertEquals("about:blank",shadowOf(web()).getLastLoadedUrl());shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(200));assertEquals("about:blank",shadowOf(web()).getLastLoadedUrl());car.resume();assertFalse(CarController.state().optBoolean("retry"));}
 @Test public void allTransportButtonsFitPortraitAndLandscapeWithoutHorizontalScrolling()throws Exception{CarController.report(car,CarController.epoch(),CarController.request(),new org.json.JSONObject().put("token","").put("document","fixture").put("id","AAAAAAAAAAA").put("hasVideo",true).put("ready",4).put("playing",true).put("title","A long observed video title that must fit without hiding controls").put("duration",60));for(int width:new int[]{320,384,832}){PhoneController phone=new PhoneController(activity,false);float d=activity.getResources().getDisplayMetrics().density;int px=(int)(width*d);phone.measure(View.MeasureSpec.makeMeasureSpec(px,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2000,View.MeasureSpec.AT_MOST));phone.layout(0,0,px,phone.getMeasuredHeight());for(String label:new String[]{"Play","Pause","Stop","−10s","+10s","Previous","Next","Retry","Car fullscreen"}){Button b=button(phone,label);assertNotNull(label,b);android.graphics.Rect bounds=new android.graphics.Rect(0,0,b.getWidth(),b.getHeight());phone.offsetDescendantRectToMyCoords(b,bounds);assertTrue(label,bounds.left>=0&&bounds.right<=px);assertTrue(label,b.getWidth()>=48*d&&b.getHeight()>=48*d);assertFalse(b.getParent() instanceof HorizontalScrollView);}assertTrue("width="+width+", height="+phone.getMeasuredHeight()/d+", state="+phone.getChildAt(0).getMeasuredHeight()+", title="+phone.getChildAt(1).getMeasuredHeight()+", seek="+phone.getChildAt(2).getMeasuredHeight(),phone.getMeasuredHeight()<(width>=720?190:240)*d);}}
 @Test public void phoneSettingsAreInNamedMenuAndCarAdsLabelIsExplicit(){TubeView phone=new TubeView(activity);try{activity.setContentView(phone);button(phone,"More").performClick();PopupMenu menu=org.robolectric.shadows.ShadowPopupMenu.getLatestPopupMenu();assertNotNull(menu);assertEquals(8,menu.getMenu().size());assertEquals("Support & diagnostics",menu.getMenu().getItem(6).getTitle());assertEquals("Android Auto setup",menu.getMenu().getItem(5).getTitle());menu.dismiss();assertNull(button(phone,"Android Auto setup"));assertNotNull(button(car,"Ads block: ON")==null?button(car,"Ads block: OFF"):button(car,"Ads block: ON"));}finally{phone.close();}}
}
