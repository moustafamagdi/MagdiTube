package com.magdi.tube;

import android.content.Context;
import android.content.SharedPreferences;

/** Enable background audio once on upgrade; later user choices remain authoritative. */
final class BackgroundAudio {
 private static final String MIGRATED="backgroundAudioDefaultV1";
 static boolean enabled(Context context){
  SharedPreferences prefs=context.getSharedPreferences("tube",0);
  if(!prefs.getBoolean(MIGRATED,false))prefs.edit().putBoolean("backgroundAudio",true).putBoolean(MIGRATED,true).apply();
  return prefs.getBoolean("backgroundAudio",true);
 }
 static void setEnabled(Context context,boolean value){
  context.getSharedPreferences("tube",0).edit().putBoolean("backgroundAudio",value).putBoolean(MIGRATED,true).apply();
 }
}
