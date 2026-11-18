package com.magdi.tube;
import android.app.Application;
import android.content.*;
import android.os.Build;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
public final class Diagnostics extends Application {
 private static Context app;
 private static DiagnosticStore store;
 private static final ExecutorService io=Executors.newSingleThreadExecutor(r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"MagdiDiagnostics"));
 private static final Semaphore slots=new Semaphore(128);
 private static final java.util.concurrent.atomic.AtomicInteger omitted=new java.util.concurrent.atomic.AtomicInteger();
 private static final SimpleDateFormat timestamp=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US);
 public void onCreate(){super.onCreate();app=this;io.execute(()->{try{store=new DiagnosticStore(getFilesDir());store.migrateLegacy(getFilesDir(),System.currentTimeMillis());store.start("App",System.currentTimeMillis());}catch(IOException e){android.util.Log.e("MagdiTube","Cannot initialize diagnostic sessions");}});event("Process started; version="+version(getPackageName()));InstallSetup.observe(this,"process start");}
 static void event(String message){
  final long when=System.currentTimeMillis();if(!slots.tryAcquire()){omitted.incrementAndGet();return;}
  io.execute(()->{try{android.util.Log.i("MagdiTube",message);int skipped=omitted.getAndSet(0);String line=timestamp.format(new Date(when))+" "+message+"\n";
   if(skipped>0)line+=timestamp.format(new Date(when))+" Diagnostics throttled; omitted="+skipped+"\n";
   if(store!=null)store.append(line,when);
  }catch(IOException ignored){}finally{slots.release();}});
 }
 static void startSession(String kind){io.execute(()->{try{if(store!=null){store.append("=== Session ended: new recording session ===\n",System.currentTimeMillis());store.start(kind,System.currentTimeMillis());store.append("Version: "+version(app.getPackageName())+"\n",System.currentTimeMillis());}}catch(IOException ignored){}});}
 static String version(String pkg){try{return app.getPackageManager().getPackageInfo(pkg,0).versionName;}catch(Exception e){return "unavailable";}}
 static void open(android.app.Activity activity){activity.startActivity(new Intent(activity,DiagnosticsActivity.class));}
 static void list(android.app.Activity activity,java.util.function.Consumer<List<File>> done){io.execute(()->{if(store!=null)store.prune(System.currentTimeMillis());List<File> files=store==null?Collections.emptyList():store.list();activity.runOnUiThread(()->{if(!activity.isFinishing()&&!activity.isDestroyed())done.accept(files);});});}
 private static String header(String part){return "MM Tube "+version(app.getPackageName())+"\nSelected log part: "+part+"\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" API "+Build.VERSION.SDK_INT+"\nSecurity patch: "+Build.VERSION.SECURITY_PATCH+"\nAndroid Auto: "+version("com.google.android.projection.gearhead")+"\nWebView: "+(android.webkit.WebView.getCurrentWebViewPackage()==null?"unknown":android.webkit.WebView.getCurrentWebViewPackage().versionName)+"\nLanguage: "+Locale.getDefault().toLanguageTag()+"\n"+InstallSetup.report(app)+"App-only diagnostic events; not Android Auto/system logcat. Video samples are page observations, not proof of audible sound. Performance counters describe the WebView video, not car display latency or audio/video offset; appJavaHeapMiB excludes WebView renderer, native and GPU memory. Blocked navigation logs include scheme and hostname only. No full URLs, titles, cookies or credentials recorded.\nRetention: last 40 parts, up to 7 days; 128 KiB per part. A long session continues in numbered parts. Header describes the device at export time.\n\n";}
 static void output(android.app.Activity activity,String name,boolean copy){
  InstallSetup.observe(activity,copy?"copy":"export");event(copy?"Copy requested":"Export requested");
  io.execute(()->{try{
   if(store==null)throw new IOException();String selected=name==null?store.currentName():name;
   String reportText=header(selected)+store.read(selected);
   if(copy){activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;try{ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("MM Tube diagnostics",reportText));toast(activity,"Log part copied","تم نسخ جزء اللوج");}catch(RuntimeException e){toast(activity,"Copy failed; use Export","تعذر النسخ؛ استخدم التصدير");}});return;}
   File dir=new File(app.getCacheDir(),"reports");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException();
   File[] old=dir.listFiles();if(old!=null)for(File file:old)if(file.getName().startsWith("Magdi-Tube-")&&System.currentTimeMillis()-file.lastModified()>DiagnosticStore.MAX_AGE_MS)file.delete();
   File report=new File(dir,"Magdi-Tube-"+selected);try(OutputStream out=new FileOutputStream(report)){out.write(reportText.getBytes(StandardCharsets.UTF_8));}
   activity.runOnUiThread(()->{if(activity.isFinishing()||activity.isDestroyed())return;try{android.net.Uri uri=android.support.v4.content.FileProvider.getUriForFile(activity,activity.getPackageName()+".reports",report);Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("Diagnostics",uri));activity.startActivity(Intent.createChooser(share,"MM Tube diagnostics"));}catch(RuntimeException e){toast(activity,"Unable to share report","تعذر مشاركة التقرير");}});
  }catch(Exception e){activity.runOnUiThread(()->toast(activity,"Log unavailable; refresh the list and retry","اللوج غير متاح؛ حدّث القائمة وجرّب مرة أخرى"));}});
 }
 private static void toast(Context context,String en,String ar){android.widget.Toast.makeText(context,context.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar")?ar:en,android.widget.Toast.LENGTH_LONG).show();}
}
