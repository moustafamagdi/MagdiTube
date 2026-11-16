package com.magdi.tube;

import android.content.*;
import android.content.pm.*;
import android.net.Uri;

/** Diagnostics for an experimental, user-confirmed self-reinstall. No AA acceptance claim. */
final class InstallSetup {
 static final String PREFS="installSetup";
 static final class Snapshot {
  final String installer,initiator,error; final long updated;
  Snapshot(String installer,String initiator,long updated,String error){this.installer=installer;this.initiator=initiator;this.updated=updated;this.error=error;}
  boolean matches(){return error==null&&"com.android.vending".equals(installer)&&knownInitiator(initiator);}
  String text(){return "installer="+clean(installer)+"; initiator="+clean(initiator)+"; lastUpdateTime="+updated+"; metadata="+(error!=null?"UNAVAILABLE":matches()?"MATCH":"MISMATCH")+(error==null?"":"; error="+error);}
 }
 private static boolean knownInitiator(String name){return "com.android.vending".equals(name)||"com.google.android.packageinstaller".equals(name)||"com.android.packageinstaller".equals(name)||"com.samsung.android.packageinstaller".equals(name);}
 private static String clean(String s){return s==null?"unknown":s.replaceAll("[^a-zA-Z0-9._-]","?");}
 static Snapshot read(Context c){try{PackageManager pm=c.getPackageManager();InstallSourceInfo source=pm.getInstallSourceInfo(c.getPackageName());return new Snapshot(source.getInstallingPackageName(),source.getInitiatingPackageName(),pm.getPackageInfo(c.getPackageName(),0).lastUpdateTime,null);}catch(Exception e){return new Snapshot(null,null,0,e.getClass().getSimpleName());}}
 static String outcome(Snapshot before,Snapshot after,boolean replaced){
  if(after.error!=null)return "UNVERIFIED_METADATA_UNAVAILABLE";
  if(!replaced)return "NO_REINSTALL_OBSERVED";
  if(!after.matches())return "REINSTALL_OBSERVED_METADATA_MISMATCH";
  return before.matches()?"REINSTALL_OBSERVED_ALREADY_MATCHED_BEFORE":"REINSTALL_OBSERVED_METADATA_NOW_MATCH";
 }
 static boolean begin(Context c,Snapshot before){return c.getSharedPreferences(PREFS,0).edit().putBoolean("pending",true).putLong("attemptAt",System.currentTimeMillis()).putLong("beforeUpdated",before.updated).putString("beforeInstaller",before.installer).putString("beforeInitiator",before.initiator).putString("beforeError",before.error).putString("outcome","AWAITING_INSTALLER").putString("after","").commit();}
 static void mark(Context c,String result){c.getSharedPreferences(PREFS,0).edit().putBoolean("pending",false).putString("outcome",result).commit();Diagnostics.event("Install setup: "+result);}
 static void observe(Context c,String reason){
  SharedPreferences p=c.getSharedPreferences(PREFS,0);Snapshot now=read(c);
  Diagnostics.event("Install source ["+reason+"]: "+now.text());
  if(!p.getBoolean("pending",false))return;
  Snapshot before=new Snapshot(p.getString("beforeInstaller",null),p.getString("beforeInitiator",null),p.getLong("beforeUpdated",0),p.getString("beforeError",null));
  boolean replaced=now.updated>before.updated;
  String result=outcome(before,now,replaced);
  p.edit().putString("outcome",result).putString("after",now.text()).putBoolean("pending",!replaced).commit();
  Diagnostics.event("Install setup verification: "+result+"; before={"+before.text()+"}; after={"+now.text()+"}; Android Auto acceptance=NOT_TESTED");
 }
 static String report(Context c){SharedPreferences p=c.getSharedPreferences(PREFS,0);return "Installation source: "+read(c).text()+"\nSetup attempt time (epoch ms): "+p.getLong("attemptAt",0)+"\nSetup result: "+p.getString("outcome","NOT_ATTEMPTED")+"\nSetup before: installer="+clean(p.getString("beforeInstaller",null))+"; initiator="+clean(p.getString("beforeInitiator",null))+"; lastUpdateTime="+p.getLong("beforeUpdated",0)+"\nSetup observed after: "+p.getString("after","not observed")+"\nInstaller metadata match is diagnostic only; Android Auto acceptance is NOT_TESTED.\n";}
 static String summary(Context c,boolean ar){Snapshot s=read(c);String state=s.error!=null?(ar?"تعذّر قراءة البيانات":"Metadata unavailable"):s.matches()?(ar?"بيانات مصدر التثبيت مطابقة":"Installation metadata matches"):(ar?"بيانات مصدر التثبيت غير مطابقة أو غير معروفة":"Installation metadata differs or is unknown");return state+"\n\n"+(ar?"المثبّت: ":"Installer: ")+clean(s.installer)+"\n"+(ar?"بدأ التثبيت: ":"Initiator: ")+clean(s.initiator)+"\n\n"+(ar?"نتيجة آخر محاولة: ":"Last attempt: ")+resultLabel(c.getSharedPreferences(PREFS,0).getString("outcome","NOT_ATTEMPTED"),ar)+"\n\n"+(ar?"مطابقة البيانات لا تؤكد ظهور التطبيق على شاشة السيارة. لو ظهر عندك بالفعل، لا تحتاج لإعادة التثبيت.":"Matching metadata does not confirm acceptance on the car. If the app already appears, reinstallation is unnecessary.");}
 static String resultLabel(String value,boolean ar){
  switch(value){
   case "NOT_ATTEMPTED":return ar?"لم تُجرَ محاولة بعد":"Not attempted";
   case "AWAITING_INSTALLER":return ar?"في انتظار التثبيت":"Waiting for installation";
   case "NO_REINSTALL_OBSERVED":return ar?"لم تُرصد إعادة تثبيت بعد":"No reinstallation observed yet";
   case "REINSTALL_OBSERVED_METADATA_NOW_MATCH":return ar?"تمت إعادة التثبيت وأصبحت البيانات مطابقة":"Reinstalled; metadata now matches";
   case "REINSTALL_OBSERVED_ALREADY_MATCHED_BEFORE":return ar?"تمت إعادة التثبيت؛ البيانات كانت مطابقة بالفعل":"Reinstalled; metadata already matched before";
   case "REINSTALL_OBSERVED_METADATA_MISMATCH":return ar?"تمت إعادة التثبيت لكن البيانات لم تطابق المطلوب":"Reinstalled, but metadata does not match";
   case "CANCELLED_OR_FAILED_NO_REINSTALL_OBSERVED":return ar?"أُلغي التثبيت أو تعذّر إتمامه؛ لم تُرصد إعادة تثبيت":"Cancelled or failed; no reinstallation observed";
   case "UNVERIFIED_METADATA_UNAVAILABLE":return ar?"تعذّر التحقق من بيانات التثبيت":"Unable to verify installation metadata";
   default:return ar?"تعذّر بدء التثبيت؛ صدّر اللوج":"Unable to start installation; export the log";
  }
 }
 static Intent intent(Uri uri){Intent i=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME,"com.android.vending").putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE,true).putExtra("installerPackageName","com.android.vending").putExtra(Intent.EXTRA_RETURN_RESULT,true);i.setClipData(ClipData.newRawUri("MM Tube APK",uri));return i;}
 static ComponentName systemInstaller(Context c,Intent intent){
  PackageManager pm=c.getPackageManager();ResolveInfo preferred=pm.resolveActivity(intent,PackageManager.MATCH_DEFAULT_ONLY);
  for(ResolveInfo r:pm.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY))if(isSystem(r)&&preferred!=null&&preferred.activityInfo!=null&&r.activityInfo.packageName.equals(preferred.activityInfo.packageName)&&r.activityInfo.name.equals(preferred.activityInfo.name))return new ComponentName(r.activityInfo.packageName,r.activityInfo.name);
  for(ResolveInfo r:pm.queryIntentActivities(intent,PackageManager.MATCH_DEFAULT_ONLY))if(isSystem(r))return new ComponentName(r.activityInfo.packageName,r.activityInfo.name);
  return null;
 }
 private static boolean isSystem(ResolveInfo r){return r!=null&&r.activityInfo!=null&&r.activityInfo.exported&&(r.activityInfo.applicationInfo.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;}
 private InstallSetup(){}
}
