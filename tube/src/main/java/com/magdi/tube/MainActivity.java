package com.magdi.tube;
public final class MainActivity extends android.app.Activity {
 private TubeView page;
 public void onBackPressed(){if(page==null||!page.handleBack())super.onBackPressed();}
 public void onCreate(android.os.Bundle b) { super.onCreate(b); page=new TubeView(this); setContentView(page);
 page.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
 if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},108); }
 protected void onPause(){ if(page!=null)page.pause(); super.onPause(); }
 protected void onResume(){super.onResume();if(page!=null)page.resume();}
 protected void onDestroy(){if(page!=null)page.close();super.onDestroy();}
}
