package com.magdi.tube;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import java.io.*;

/** Phone-only setup; the APK is copied from our installed package, never downloaded. */
public final class InstallSetupActivity extends Activity {
 private TextView status; private Button install; private boolean ar,busy;
 protected void onCreate(Bundle b){super.onCreate(b);ar=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
  LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*getResources().getDisplayMetrics().density);panel.setPadding(pad,pad,pad,pad);
  panel.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(pad+bars.left,pad+bars.top,pad+bars.right,pad+bars.bottom);return insets;});
  ScrollView scroll=new ScrollView(this);scroll.addView(panel);setContentView(scroll);
  TextView title=new TextView(this);title.setText(ar?"إعداد Android Auto — تجريبي":"Android Auto setup — experimental");title.setTextSize(22);panel.addView(title);
  status=new TextView(this);status.setTextSize(16);panel.addView(status);
  install=action(panel,ar?"إعادة تثبيت للإعداد":"Reinstall for setup",()->new android.app.AlertDialog.Builder(this).setMessage(ar?"سيُعاد تثبيت نفس النسخة بتوقيعها الحالي مع الاحتفاظ بالبيانات. وافق على نافذة أندرويد، ثم افتح التطبيق مجددًا لفحص النتيجة. قد تختلف النتيجة حسب الهاتف.":"Reinstall this same signed version, keeping app data. Confirm Android's installer, then reopen the app to check the result. Results depend on the phone.").setNegativeButton(ar?"إلغاء":"Cancel",null).setPositiveButton(ar?"متابعة":"Continue",(d,w)->prepare()).show());
  action(panel,ar?"فحص النتيجة":"Check result",()->{InstallSetup.observe(this,"manual check");refresh();});
  action(panel,ar?"جلسات اللوج":"Log sessions",()->Diagnostics.open(this));
  action(panel,ar?"رجوع":"Back",this::finish);
 }
 private Button action(LinearLayout panel,String label,Runnable run){Button b=new Button(this);b.setText(label);b.setTextSize(18);b.setAllCaps(false);panel.addView(b,new LinearLayout.LayoutParams(-1,(int)(60*getResources().getDisplayMetrics().density)));b.setOnClickListener(v->run.run());return b;}
 protected void onResume(){super.onResume();InstallSetup.observe(this,"setup opened");refresh();}
 private void refresh(){if(status!=null)status.setText(InstallSetup.summary(this,ar));}
 private void prepare(){
  if(busy)return;
  if(!getPackageManager().canRequestPackageInstalls()){
   Diagnostics.event("Install setup: waiting for user to allow installs from this app");
   Toast.makeText(this,ar?"اسمح بالتثبيت من MM Tube ثم ارجع واضغط إعادة تثبيت":"Allow installs from MM Tube, then return and tap Reinstall",Toast.LENGTH_LONG).show();
   try{startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+getPackageName())));}catch(RuntimeException e){fail(e);}return;
  }
  busy=true;install.setEnabled(false);
  final Context app=getApplicationContext();
  new Thread(()->{try{
   if(app.getApplicationInfo().splitSourceDirs!=null&&app.getApplicationInfo().splitSourceDirs.length>0)throw new IOException("Split APK unsupported");
   File dir=new File(app.getCacheDir(),"installer");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Cannot create cache");
   File apk=new File(dir,"Magdi-Tube.apk");
   try(InputStream in=new FileInputStream(app.getApplicationInfo().sourceDir);OutputStream out=new FileOutputStream(apk)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
   Uri uri=android.support.v4.content.FileProvider.getUriForFile(app,getPackageName()+".reports",apk);
   runOnUiThread(()->{busy=false;if(isFinishing()||isDestroyed())return;install.setEnabled(true);launch(uri);});
  }catch(Exception e){runOnUiThread(()->{busy=false;if(!isFinishing()&&!isDestroyed()){install.setEnabled(true);fail(e);}});}},"Magdi-install-prepare").start();
 }
 private void launch(Uri uri){try{
  Intent intent=InstallSetup.intent(uri);ComponentName target=InstallSetup.systemInstaller(this,intent);if(target==null)throw new ActivityNotFoundException("No system installer");intent.setComponent(target);
  InstallSetup.Snapshot before=InstallSetup.read(this);if(before.error!=null)throw new IllegalStateException("Cannot record baseline");
  if(!InstallSetup.begin(this,before))throw new IOException("Cannot persist attempt");
  Diagnostics.event("Install setup launched: classic self-reinstall; version="+Diagnostics.version(getPackageName())+"; handler="+target.getPackageName()+"; before={"+before.text()+"}");
  startActivityForResult(intent,501);
 }catch(Exception e){InstallSetup.mark(this,"LAUNCH_FAILED_"+e.getClass().getSimpleName());fail(e);}}
 protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==501){Diagnostics.event("Install dialog returned: resultCode="+result+" (not proof of metadata change)");InstallSetup.observe(this,"installer returned");if(result!=RESULT_OK&&getSharedPreferences(InstallSetup.PREFS,0).getBoolean("pending",false))InstallSetup.mark(this,"CANCELLED_OR_FAILED_NO_REINSTALL_OBSERVED");refresh();}}
 private void fail(Exception e){Diagnostics.event("Install setup error: "+e.getClass().getSimpleName());Toast.makeText(this,ar?"تعذّر فتح التثبيت. صدّر اللوج لفحص السبب.":"Unable to prepare installation. Export the log for diagnosis.",Toast.LENGTH_LONG).show();refresh();}
}
