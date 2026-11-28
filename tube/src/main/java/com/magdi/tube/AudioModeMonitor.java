package com.magdi.tube;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import java.util.function.IntConsumer;

/** Observes call/VoIP mode without requesting competing audio focus or call permissions. */
final class AudioModeMonitor {
 static Runnable listen(Context context,IntConsumer changed){
  if(Build.VERSION.SDK_INT<31)return ()->{}; // TubeView retains polling on Android 11.
  AudioManager audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
  AudioManager.OnModeChangedListener listener=changed::accept;
  audio.addOnModeChangedListener(context.getMainExecutor(),listener);
  return ()->audio.removeOnModeChangedListener(listener);
 }
 private AudioModeMonitor(){}
}
