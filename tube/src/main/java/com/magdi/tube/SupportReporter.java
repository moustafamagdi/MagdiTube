package com.magdi.tube;
import android.content.*;
import android.app.job.*;
import android.os.*;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Separate bounded, allowlisted support history; never reads raw diagnostic files. */
final class SupportReporter {
 static final String ENDPOINT="https://hncaagdpxhbysdcdwuoa.supabase.co/functions/v1/mm-diagnostics";
 static final int JOB=77124; static final long AGE=86400000L, INTERVAL=2*60*60*1000L;
 static final ScheduledExecutorService io=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"MMSupport");t.setPriority(Thread.MIN_PRIORITY);return t;});
 private static final Semaphore slots=new Semaphore(128);
 private static Context app; private static SharedPreferences prefs; private static AtomicFile file;
 private static JSONArray history=new JSONArray(); private static volatile boolean enabled; private static volatile HttpURLConnection active;
 interface Delivery { int send(JSONObject body) throws Exception; }
 static Delivery delivery=SupportReporter::request;
 private static final Object gate=new Object();
 private static long lastSample; private static boolean initialized;
 static void init(Context c){io.execute(()->{if(initialized)return;initialized=true;app=c.getApplicationContext();prefs=app.getSharedPreferences("mm_support_v1",0);file=new AtomicFile(new File(app.getNoBackupFilesDir(),"support-v1.json"));
  if(!prefs.contains("id"))newIdentity();enabled=prefs.getBoolean("enabled",false);
  try{history=new JSONArray(new String(file.readFully(),StandardCharsets.UTF_8));}catch(Exception ignored){}prune();
  try{android.app.ActivityManager am=(android.app.ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);for(android.app.ApplicationExitInfo e:am.getHistoricalProcessExitReasons(app.getPackageName(),0,1)){if(e.getTimestamp()>prefs.getLong("exitAt",0)){prefs.edit().putLong("exitAt",e.getTimestamp()).commit();if(e.getReason()==android.app.ApplicationExitInfo.REASON_CRASH||e.getReason()==android.app.ApplicationExitInfo.REASON_CRASH_NATIVE||e.getReason()==android.app.ApplicationExitInfo.REASON_ANR)append(new JSONObject().put("at",e.getTimestamp()).put("kind","crash").put("code",e.getReason()));}}}catch(Exception ignored){}
  schedule();io.scheduleWithFixedDelay(()->upload(false,null),60,120,TimeUnit.SECONDS);
 });}
 private static void newIdentity(){prefs.edit().putString("id",UUID.randomUUID().toString()).putString("token",UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-","")).putLong("ack",0).putLong("lastSent",0).putString("status","never").commit();}
 static void record(String text,long at){JSONObject event=SupportPolicy.event(text,at);if(event==null||!slots.tryAcquire())return;io.execute(()->{try{if(app==null)return;if("media".equals(event.optString("kind"))&&(text.startsWith("Playback source=car ")||"telemetry".equals(event.optString("state")))){if(at-lastSample<60000)return;lastSample=at;}append(event);}finally{slots.release();}});}
 private static void append(JSONObject e){history.put(e);prune();save();}
 private static void prune(){JSONArray next=new JSONArray();long cut=System.currentTimeMillis()-AGE;for(int i=Math.max(0,history.length()-256);i<history.length();i++){JSONObject e=history.optJSONObject(i);if(e!=null&&e.optLong("at")>=cut)next.put(e);}history=next;}
 private static void save(){FileOutputStream out=null;try{out=file.startWrite();out.write(history.toString().getBytes(StandardCharsets.UTF_8));file.finishWrite(out);}catch(Exception e){if(out!=null)file.failWrite(out);}}
 static void changeConsent(boolean on,Consumer<String> done){if(!on){synchronized(gate){enabled=false;HttpURLConnection c=active;if(c!=null)c.disconnect();}}io.execute(()->{enabled=on;prefs.edit().putBoolean("enabled",on).putLong("ack",System.currentTimeMillis()).commit();schedule();reply(done,"ok");});}
 private static void schedule(){JobScheduler js=(JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(enabled)js.schedule(new JobInfo.Builder(JOB,new ComponentName(app,SupportUploadJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(INTERVAL).setBackoffCriteria(30*60*1000L,JobInfo.BACKOFF_POLICY_EXPONENTIAL).build());else js.cancel(JOB);}
 static void send(Consumer<String> done){io.execute(()->upload(true,done));}
 static void automatic(Runnable done){io.execute(()->{try{upload(false,null);}finally{done.run();}});}
 private static void upload(boolean manual,Consumer<String> done){if(app==null){reply(done,"failed");return;}long now=System.currentTimeMillis();if(!manual&&(!enabled||now-prefs.getLong("lastAttempt",0)<INTERVAL))return;
  prune();JSONArray events=new JSONArray();for(int i=Math.max(0,history.length()-128);i<history.length();i++){JSONObject e=history.optJSONObject(i);if(manual||e.optLong("at")>prefs.getLong("ack",0))events.put(e);}if(!manual&&events.length()==0)return;
  prefs.edit().putLong("lastAttempt",now).commit();String status="failed";
  try{JSONObject body=base().put("action","upload").put("events",events);String stable=prefs.getString("id","")+events.toString()+body.getJSONObject("environment").toString();body.put("batch",UUID.nameUUIDFromBytes(stable.getBytes(StandardCharsets.UTF_8)).toString());if(!manual&&!enabled)return;int code=delivery.send(body.put("manual",manual));status=code==200?"sent":code==429?"limit":"failed";if(code==200)prefs.edit().putLong("lastSent",now).putLong("ack",now).commit();}catch(Exception ignored){}
  prefs.edit().putString("status",status).commit();reply(done,status);
 }
 static void erase(Consumer<String> done){enabled=false;HttpURLConnection c=active;if(c!=null)c.disconnect();io.execute(()->{prefs.edit().putBoolean("enabled",false).commit();schedule();try{int status=delivery.send(base().put("action","delete").put("manual",true));if(status!=200){reply(done,"failed");return;}history=new JSONArray();save();newIdentity();reply(done,"deleted");}catch(Exception e){reply(done,"failed");}});}
 private static JSONObject base() throws JSONException{return new JSONObject().put("schema",1).put("app","tube").put("support_id",prefs.getString("id","")).put("environment",environment());}
 static JSONObject environment() throws JSONException {JSONObject o=new JSONObject().put("app_version",version(app.getPackageName())).put("android_api",Build.VERSION.SDK_INT).put("aa_version",version("com.google.android.projection.gearhead"));android.content.pm.PackageInfo w=android.webkit.WebView.getCurrentWebViewPackage();o.put("webview_version",w==null?"unknown":safeVersion(w.versionName));
  o.put("manufacturer",manufacturer());o.put("model",Build.MODEL!=null&&Build.MODEL.matches("[a-zA-Z0-9 _().-]{1,48}")?Build.MODEL:"other");
  try{android.content.pm.InstallSourceInfo s=app.getPackageManager().getInstallSourceInfo(app.getPackageName());o.put("installer",installer(s.getInstallingPackageName()));o.put("initiator",installer(s.getInitiatingPackageName()));}catch(Exception e){o.put("installer","unknown");o.put("initiator","unknown");}return o;}
 private static String manufacturer(){String m=Build.MANUFACTURER.toLowerCase(Locale.ROOT);return m.matches("samsung|xiaomi|google|huawei|honor|oppo|vivo|oneplus|motorola|sony|realme|nothing|asus|nokia")?m:"other";}
 private static String installer(String p){if(p==null)return "unknown";switch(p){case "com.android.vending":return "play";case "com.google.android.packageinstaller":return "google";case "com.android.packageinstaller":return "android";case "com.samsung.android.packageinstaller":return "samsung";case "com.miui.packageinstaller":return "xiaomi";default:return "other";}}
 private static String version(String pkg){try{return safeVersion(app.getPackageManager().getPackageInfo(pkg,0).versionName);}catch(Exception e){return "unknown";}}
 private static String safeVersion(String s){return s!=null&&s.matches("[0-9][a-zA-Z0-9._-]{0,63}")?s:"unknown";}
 private static int request(JSONObject body) throws Exception {HttpURLConnection c=(HttpURLConnection)new URL(ENDPOINT).openConnection();synchronized(gate){boolean manual=body.optBoolean("manual");body.remove("manual");if(!manual&&!enabled){c.disconnect();throw new IOException();}active=c;}try{c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+prefs.getString("token",""));byte[] data=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(data.length);try(OutputStream out=c.getOutputStream()){out.write(data);}return c.getResponseCode();}finally{active=null;c.disconnect();}}
 static void info(Consumer<JSONObject> done){io.execute(()->{try{JSONObject result=base().put("enabled",enabled).put("last_sent",prefs.getLong("lastSent",0)).put("status",prefs.getString("status","never"));replyJson(done,result);}catch(Exception ignored){replyJson(done,new JSONObject());}});}
 static void share(android.app.Activity a,boolean copy){io.execute(()->{try{prune();JSONObject report=base().put("events",history);String text="MM support report (last 24 hours, up to 256 safe events)\n"+report.toString(2);if(copy){new Handler(Looper.getMainLooper()).post(()->{if(a.isFinishing())return;((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("MM support report",text));android.widget.Toast.makeText(a,"✓",android.widget.Toast.LENGTH_SHORT).show();});return;}File dir=new File(app.getCacheDir(),"reports");dir.mkdirs();File f=new File(dir,"MM-support-report.txt");try(FileOutputStream out=new FileOutputStream(f)){out.write(text.getBytes(StandardCharsets.UTF_8));}new Handler(Looper.getMainLooper()).post(()->{if(a.isFinishing())return;android.net.Uri uri=android.support.v4.content.FileProvider.getUriForFile(a,a.getPackageName()+".reports",f);Intent send=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);send.setClipData(ClipData.newRawUri("Support",uri));a.startActivity(Intent.createChooser(send,"MM Support"));});}catch(Exception ignored){new Handler(Looper.getMainLooper()).post(()->android.widget.Toast.makeText(a,"Report unavailable / التقرير غير متاح",android.widget.Toast.LENGTH_LONG).show());}});}
 private static void reply(Consumer<String> done,String s){if(done!=null)new Handler(Looper.getMainLooper()).post(()->done.accept(s));}
 private static void replyJson(Consumer<JSONObject> done,JSONObject s){new Handler(Looper.getMainLooper()).post(()->done.accept(s));}
 private SupportReporter(){}
}
