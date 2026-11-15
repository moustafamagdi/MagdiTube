package com.magdi.tube;

import android.app.*;
import android.content.*;
import android.media.*;
import android.os.IBinder;
import java.lang.ref.WeakReference;

/** Keeps an already-playing WebView alive while its host is temporarily covered. */
public final class PlaybackService extends Service {
 private static WeakReference<TubeView> owner=new WeakReference<>(null);
 private static boolean requested, interrupted;
 private AudioManager audio;
 private AudioFocusRequest focus;
 private android.media.session.MediaSession session;
 static void keep(TubeView view){
  if(owner.get()==view&&requested)return;
  TubeView old=owner.get();if(old!=null&&old!=view)old.pauseMedia();
  owner=new WeakReference<>(view);requested=true;
  try{view.getContext().startForegroundService(new Intent(view.getContext(),PlaybackService.class));}
  catch(RuntimeException e){requested=false;owner.clear();Diagnostics.event("Background service unavailable: "+e.getClass().getSimpleName());}
 }
 static void idle(TubeView view){if(!interrupted)release(view);}
 static void release(TubeView view){if(owner.get()!=view)return;owner.clear();requested=false;interrupted=false;view.getContext().stopService(new Intent(view.getContext(),PlaybackService.class));}
 public IBinder onBind(Intent intent){return null;}
 public int onStartCommand(Intent intent,int flags,int id){
  boolean ar=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
  NotificationManager manager=getSystemService(NotificationManager.class);
  manager.createNotificationChannel(new NotificationChannel("playback","Magdi Tube playback",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,PlaybackService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification notification=new Notification.Builder(this,"playback").setSmallIcon(R.drawable.ic_tube).setContentTitle("Magdi Tube").setContentText(ar?"الصوت يعمل في الخلفية":"Background audio playing").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,ar?"إيقاف":"Stop",stop).build()).build();
  startForeground(108,notification,android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
  if(intent!=null&&"stop".equals(intent.getAction())){stopPlayback();return START_NOT_STICKY;}
  if(owner.get()==null){stopSelf();return START_NOT_STICKY;}
  if(focus==null){
   audio=getSystemService(AudioManager.class);
   focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()).setOnAudioFocusChangeListener(change->{
    TubeView view=owner.get();
    if(change==AudioManager.AUDIOFOCUS_LOSS)stopPlayback();
    else if(change<0){interrupted=true;if(view!=null)view.pauseForFocus();Diagnostics.event("Audio focus temporarily lost");}
    else if(change==AudioManager.AUDIOFOCUS_GAIN&&interrupted){interrupted=false;if(view!=null)view.playMedia();Diagnostics.event("Audio focus restored");}
   }).build();
   if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){Diagnostics.event("Audio focus unavailable; playback paused");stopPlayback();return START_NOT_STICKY;}
   session=new android.media.session.MediaSession(this,"MagdiTube");
   session.setCallback(new android.media.session.MediaSession.Callback(){public void onPause(){stopPlayback();}public void onStop(){stopPlayback();}});
   session.setPlaybackState(new android.media.session.PlaybackState.Builder().setActions(android.media.session.PlaybackState.ACTION_PAUSE|android.media.session.PlaybackState.ACTION_STOP).setState(android.media.session.PlaybackState.STATE_PLAYING,android.media.session.PlaybackState.PLAYBACK_POSITION_UNKNOWN,1f).build());session.setActive(true);
   Diagnostics.event("Background audio service started");
  }
  return START_NOT_STICKY;
 }
 private void stopPlayback(){TubeView view=owner.get();if(view!=null)view.pauseMedia();stopSelf();}
 public void onDestroy(){if(audio!=null&&focus!=null)audio.abandonAudioFocusRequest(focus);if(session!=null)session.release();requested=false;interrupted=false;owner.clear();Diagnostics.event("Background audio service stopped");super.onDestroy();}
}
