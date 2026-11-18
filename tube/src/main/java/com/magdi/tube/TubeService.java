package com.magdi.tube;
import com.google.android.apps.auto.sdk.*;
public final class TubeService extends CarActivityService {
 @Override public android.os.IBinder onBind(android.content.Intent intent){android.os.IBinder binder=super.onBind(intent);CarPlaybackSession.connectionAvailable(this);return binder;}
 @Override public void onRebind(android.content.Intent intent){super.onRebind(intent);CarPlaybackSession.connectionAvailable(this);}
 @Override public boolean onUnbind(android.content.Intent intent){CarPlaybackSession.connectionLost(this);return super.onUnbind(intent);}
 @Override public void onDestroy(){CarPlaybackSession.connectionLost(this);super.onDestroy();}
 public Class<? extends CarActivity> getCarActivity(){return TubeCarActivity.class;}
}
