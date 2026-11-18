package com.magdi.tube;
import android.app.Activity;
import android.content.*;
import android.widget.Spinner;
import android.view.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class DiagnosticsTest {
 private Activity activity;
 @Before public void setup()throws Exception{activity=Robolectric.buildActivity(Activity.class).setup().get();drain();}
 @After public void cleanup(){activity.finish();}
 private void drain()throws Exception{java.lang.reflect.Field field=Diagnostics.class.getDeclaredField("io");field.setAccessible(true);((ExecutorService)field.get(null)).submit(()->{}).get(5,TimeUnit.SECONDS);shadowOf(android.os.Looper.getMainLooper()).idle();}
 private DiagnosticStore store()throws Exception{java.lang.reflect.Field field=Diagnostics.class.getDeclaredField("store");field.setAccessible(true);return (DiagnosticStore)field.get(null);}
 @Test public void copyAndExportContainOnlySelectedPartIncludingHeader()throws Exception{
  Diagnostics.startSession("Manual");Diagnostics.event("SELECTED_RECORD");drain();String chosen=store().currentName();
  Diagnostics.startSession("Manual");Diagnostics.event("UNSELECTED_RECORD");drain();
  Diagnostics.output(activity,chosen,true);drain();
  ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);String copied=clipboard.getPrimaryClip().getItemAt(0).getText().toString();
  assertTrue(copied.contains("SELECTED_RECORD"));assertFalse(copied.contains("UNSELECTED_RECORD"));assertTrue(copied.contains("Selected log part: "+chosen));
  Diagnostics.output(activity,chosen,false);drain();
  File file=new File(activity.getCacheDir(),"reports/Magdi-Tube-"+chosen);assertEquals(copied,new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8));
  Intent chooser=shadowOf(activity).getNextStartedActivity();assertEquals(Intent.ACTION_CHOOSER,chooser.getAction());Intent send=chooser.getParcelableExtra(Intent.EXTRA_INTENT);assertNotNull(send.getParcelableExtra(Intent.EXTRA_STREAM));assertTrue((send.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
 }
 @Test public void sessionScreenDefaultsToCurrentPartAndDoesNotChangeSession()throws Exception{
  Diagnostics.startSession("Manual");drain();String selected=store().currentName();
  DiagnosticsActivity screen=Robolectric.buildActivity(DiagnosticsActivity.class).setup().get();drain();
  try{Spinner picker=findSpinner(screen.getWindow().getDecorView());assertNotNull(picker);assertEquals(DiagnosticsActivity.label(store().list().get(0),false),picker.getSelectedItem().toString());assertEquals(selected,store().currentName());}finally{screen.finish();}
 }
 private Spinner findSpinner(View view){if(view instanceof Spinner)return (Spinner)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Spinner result=findSpinner(group.getChildAt(i));if(result!=null)return result;}}return null;}
}
