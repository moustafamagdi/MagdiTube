package com.magdi.tube;
public final class MainActivity extends android.app.Activity {
 private TubeView page;
 public void onCreate(android.os.Bundle b) { super.onCreate(b); page=new TubeView(this); setContentView(page); }
 protected void onPause(){ if(page!=null)page.pause(); super.onPause(); }
 protected void onResume(){super.onResume();if(page!=null)page.resume();}
 protected void onDestroy(){if(page!=null)page.close();super.onDestroy();}
}
