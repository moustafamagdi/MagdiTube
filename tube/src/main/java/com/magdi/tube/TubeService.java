package com.magdi.tube;
import com.google.android.apps.auto.sdk.*;
public final class TubeService extends CarActivityService {
 @Override public boolean onUnbind(android.content.Intent intent){CarPlaybackSession.connectionLost();return super.onUnbind(intent);}
 @Override public void onDestroy(){CarPlaybackSession.connectionLost();super.onDestroy();}
 public Class<? extends CarActivity> getCarActivity(){return TubeCarActivity.class;}
}
