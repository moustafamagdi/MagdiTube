package com.magdi.tube;

import android.app.*;
import android.content.*;

import android.os.IBinder;
import java.util.*;

/** Keeps an already-playing WebView alive while its host is temporarily covered. */
public final class PlaybackService extends Service {
 // WebView owns audio focus and its media session. This service only keeps the
 // process foreground; requesting another focus owner can pause WebView itself.
 private static final Map<TubeView,Boolean> players=new WeakHashMap<>();
 private static boolean requested;
 static void keep(TubeView view){
  if(!view.carAvailable())return;
  players.put(view,Boolean.TRUE);
  if(requested)return;
  requested=true;
  try{view.getContext().startForegroundService(new Intent(view.getContext(),PlaybackService.class));}
  catch(RuntimeException e){requested=false;Diagnostics.event("Background service unavailable: "+e.getClass().getSimpleName());}
 }
 static void idle(TubeView view){release(view);}
 static void release(TubeView view){if(players.remove(view)==null||!players.isEmpty())return;requested=false;view.getContext().stopService(new Intent(view.getContext(),PlaybackService.class));}
 public IBinder onBind(Intent intent){return null;}
 public int onStartCommand(Intent intent,int flags,int id){
  boolean ar=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
  NotificationManager manager=getSystemService(NotificationManager.class);
  manager.createNotificationChannel(new NotificationChannel("playback","MM Tube playback",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PlaybackService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification notification=new Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_tube).setContentTitle(getString(R.string.app_name)).setContentText(ar?"الصوت يعمل في الخلفية":"Background audio playing").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,ar?"إيقاف":"Stop",stop).build()).build();
  startForeground(108,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
  if(intent!=null&&"stop".equals(intent.getAction())){stopPlayback();return START_NOT_STICKY;}
  if(players.isEmpty()){stopSelf();return START_NOT_STICKY;}
  Diagnostics.event("Background keep-alive active; audio focus remains with WebView");
  return START_NOT_STICKY;
 }
 private void stopPlayback(){CarController.stop(CarController.epoch());CarPlaybackSession.stopDetached();stopSelf();}
 public void onDestroy(){requested=false;Diagnostics.event("Background audio service stopped");super.onDestroy();}
}
