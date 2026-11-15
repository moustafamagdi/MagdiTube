package com.magdi.tube;
public final class TubeCarActivity extends com.google.android.apps.auto.sdk.CarActivity {
 private TubeView page;
 public void onBackPressed(){if(page==null||!page.handleBack())super.onBackPressed();}
 public void onCreate(android.os.Bundle b){super.onCreate(b);page=CarPlaybackSession.attach(this,this);setContentView(page);android.util.Log.i("MagdiTube","Car WebView created; no screen capture");}
 public void onStart(){super.onStart();if(page!=null&&CarPlaybackSession.isHost(this))page.resume();}
 public void onStop(){if(page!=null&&CarPlaybackSession.isHost(this))page.pause();super.onStop();}
 public void onDestroy(){if(page!=null)CarPlaybackSession.detach(this);super.onDestroy();}
}
