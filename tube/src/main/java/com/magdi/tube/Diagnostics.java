package com.magdi.tube;
import android.app.Application;
import android.content.*;
import android.os.Build;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
public final class Diagnostics extends Application {
 private static Context app;
 private static final ExecutorService io=Executors.newSingleThreadExecutor();
 public void onCreate(){super.onCreate();app=this;event("Process started; version="+version(getPackageName()));InstallSetup.observe(this,"process start");}
 static void event(String message){android.util.Log.i("MagdiTube",message);final String line=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US).format(new Date())+" "+message+"\n";io.execute(()->{try{File f=new File(app.getFilesDir(),"diagnostics.txt");if(f.length()>262144){File old=new File(app.getFilesDir(),"diagnostics.previous.txt");if(old.exists()&&!old.delete())return;if(!f.renameTo(old))return;}try(FileOutputStream out=new FileOutputStream(f,true)){out.write(line.getBytes("UTF-8"));}}catch(IOException ignored){}});}
 static String version(String pkg){try{return app.getPackageManager().getPackageInfo(pkg,0).versionName;}catch(Exception e){return "unavailable";}}
 static void export(android.app.Activity activity){InstallSetup.observe(activity,"export");event("Export requested");io.execute(()->{try{File dir=new File(app.getCacheDir(),"reports");if(!dir.exists()&&!dir.mkdirs())throw new IOException();File report=new File(dir,"Magdi-Tube-diagnostics.txt");try(OutputStream out=new FileOutputStream(report)){String header="Magdi Tube "+version(app.getPackageName())+"\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" API "+Build.VERSION.SDK_INT+"\nSecurity patch: "+Build.VERSION.SECURITY_PATCH+"\nAndroid Auto: "+version("com.google.android.projection.gearhead")+"\nWebView: "+(android.webkit.WebView.getCurrentWebViewPackage()==null?"unknown":android.webkit.WebView.getCurrentWebViewPackage().versionName)+"\nLanguage: "+Locale.getDefault().toLanguageTag()+"\n"+InstallSetup.report(app)+"App-only diagnostic events; not Android Auto/system logcat. Video samples are page observations, not proof of audible sound. Blocked navigation logs include scheme and hostname only. No full URLs, titles, cookies or credentials recorded.\n\n";out.write(header.getBytes("UTF-8"));for(String n:new String[]{"diagnostics.previous.txt","diagnostics.txt"}){File f=new File(app.getFilesDir(),n);if(f.exists())try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int count;while((count=in.read(b))!=-1)out.write(b,0,count);}}}
 activity.runOnUiThread(()->{if(activity.isFinishing())return;try{android.net.Uri uri=android.support.v4.content.FileProvider.getUriForFile(activity,activity.getPackageName()+".reports",report);Intent share=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("Diagnostics",uri));activity.startActivity(Intent.createChooser(share,"Magdi Tube diagnostics"));}catch(RuntimeException e){android.widget.Toast.makeText(activity,"Unable to share report",android.widget.Toast.LENGTH_LONG).show();}});
 }catch(Exception e){activity.runOnUiThread(()->android.widget.Toast.makeText(activity,"Export failed; please retry",android.widget.Toast.LENGTH_LONG).show());}});}
}

