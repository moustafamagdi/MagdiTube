package com.magdi.tube;
public final class TubeCarActivity extends com.google.android.apps.auto.sdk.CarActivity {
 private TubeView page;
 public void onBackPressed(){if(page==null||!page.handleBack())super.onBackPressed();}
 public void onCreate(android.os.Bundle b){super.onCreate(b);page=CarPlaybackSession.attach(this,this);setContentView(page);android.util.Log.i("MagdiTube","Car WebView created; no screen capture");}
 public void onStart(){super.onStart();CarPlaybackSession.visibility(this,true);CarUiDiagnostics.event("start");if(page!=null&&CarPlaybackSession.isHost(this)&&CarPlaybackSession.connected())page.resume();}
 public void onResume(){super.onResume();CarPlaybackSession.visibility(this,true);CarUiDiagnostics.event("resume");}
 public void onPause(){CarPlaybackSession.visibility(this,false);CarUiDiagnostics.event("pause");super.onPause();}
 public void onStop(){CarPlaybackSession.visibility(this,false);CarUiDiagnostics.event("stop");if(page!=null&&CarPlaybackSession.isHost(this))page.cover();super.onStop();}
 public void onDestroy(){if(page!=null)CarPlaybackSession.detach(this);super.onDestroy();}
 void replacePage(){page=CarPlaybackSession.attach(this,this);setContentView(page);page.resume();}
}
