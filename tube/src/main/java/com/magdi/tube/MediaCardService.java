package com.magdi.tube;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import java.lang.ref.WeakReference;
import java.util.*;

/** A control-only session: WebView remains the only player and audio-focus owner. */
public final class MediaCardService extends MediaBrowserServiceCompat {
 private static final String ROOT="magdi",CURRENT="current";
 private static final Map<TubeView,State> observations=new WeakHashMap<>();
 private static WeakReference<TubeView> selected=new WeakReference<>(null);
 private static WeakReference<MediaCardService> running=new WeakReference<>(null);
 private MediaSessionCompat session;
 private boolean available,metadataPublished;
 static final class State {
  final boolean playing,muted,ready;final long position;
  State(boolean playing,boolean muted,boolean ready,long position){this.playing=playing;this.muted=muted;this.ready=ready;this.position=position;}
 }
 static void observe(TubeView view,boolean playing,boolean muted,boolean ready,long position){
  State old=observations.put(view,new State(playing,muted,ready,position));TubeView current=selected.get();
  // Muted feed previews must not take the car video's controls. Keep a paused target.
  if(ready&&!muted&&(current==null||(!current.isCarView()&&view.isCarView()))){selected=new WeakReference<>(view);Diagnostics.event("Media card target selected: "+(view.isCarView()?"car":"phone"));}
  if(selected.get()==view&&running.get()!=null&&(old==null||old.playing!=playing||old.ready!=ready))Diagnostics.event("Media card state reported: playing="+playing+"; ready="+ready+"; muted="+muted);
  if(selected.get()==view)refresh();
 }
 static void paused(TubeView view){State s=observations.get(view);if(s!=null)observe(view,false,s.muted,s.ready,s.position);}
 static TubeView target(){return selected.get();}
 static boolean retains(TubeView view){return running.get()!=null&&selected.get()==view;}
 static void forget(TubeView view){observations.remove(view);if(selected.get()==view)selected=new WeakReference<>(null);refresh();}
 private static void refresh(){MediaCardService service=running.get();if(service!=null)service.publish();}
 // This session only controls an already selected video; it has no searchable catalog.
 @android.annotation.SuppressLint("MissingOnPlayFromSearch")
 @Override public void onCreate(){super.onCreate();running=new WeakReference<>(this);session=new MediaSessionCompat(this,"MagdiTubeAudio");
  session.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS|MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
  session.setCallback(new MediaSessionCompat.Callback(){
   public void onPlay(){command(true);}
   public void onPause(){command(false);}
   public void onStop(){command(false);}
   public void onPlayFromMediaId(String id,Bundle extras){if(CURRENT.equals(id))command(true);}
  });
  setSessionToken(session.getSessionToken());session.setActive(true);publish();Diagnostics.event("Media card service connected; no additional audio focus requested");
 }
 private boolean ar(){return getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");}
 @Override public BrowserRoot onGetRoot(String pkg,int uid,Bundle hints){
  boolean trusted=uid==android.os.Process.myUid();
  if(!trusted&&"com.google.android.projection.gearhead".equals(pkg))try{trusted=getPackageManager().getApplicationInfo(pkg,0).uid==uid;}catch(Exception ignored){}
  if(!trusted)return null;
  Diagnostics.event("Media card browser accepted");return new BrowserRoot(ROOT,null);
 }
 @Override public void onLoadChildren(String parent,Result<List<MediaBrowserCompat.MediaItem>> result){
  if(!ROOT.equals(parent)){result.sendResult(Collections.emptyList());return;}
  MediaDescriptionCompat description=new MediaDescriptionCompat.Builder().setMediaId(CURRENT).setTitle(ar()?"صوت الفيديو الحالي":"Current video audio").setSubtitle(available?(ar()?"تشغيل وإيقاف مؤقت":"Play and pause"):(ar()?"افتح Magdi Tube واختر فيديو أولًا":"Open Magdi Tube and choose a video first")).build();
  result.sendResult(Collections.singletonList(new MediaBrowserCompat.MediaItem(description,available?MediaBrowserCompat.MediaItem.FLAG_PLAYABLE:0)));
 }
 private void publish(){
  if(session==null)return;State state=observations.get(selected.get());boolean now=state!=null&&state.ready;
  if(!metadataPublished||now!=available){metadataPublished=true;session.setMetadata(new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,CURRENT).putString(MediaMetadataCompat.METADATA_KEY_TITLE,"Magdi Tube").putString(MediaMetadataCompat.METADATA_KEY_ARTIST,now?(ar()?"صوت الفيديو الحالي":"Current video audio"):(ar()?"اختر فيديو في Magdi Tube أولًا":"Choose a video in Magdi Tube first")).build());}
  long actions=now?PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE|PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID:0;
  session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions).setState(!now?PlaybackStateCompat.STATE_NONE:state.playing?PlaybackStateCompat.STATE_PLAYING:PlaybackStateCompat.STATE_PAUSED,now?state.position:0,now&&state.playing?1f:0f).build());
  if(now!=available){available=now;notifyChildrenChanged(ROOT);}
 }
 void command(boolean play){
  TubeView view=selected.get();State state=observations.get(view);
  if(view==null||state==null||!state.ready){Diagnostics.event("Media card command ignored: no current video");publish();return;}
  Diagnostics.event("Media card command: "+(play?"PLAY":"PAUSE")+" target="+(view.isCarView()?"car":"phone"));
  if(play)view.playFromMediaCard();else view.pauseMedia();
 }
 @Override public void onDestroy(){if(running.get()==this)running=new WeakReference<>(null);if(session!=null){session.setActive(false);session.release();session=null;}Diagnostics.event("Media card service disconnected");super.onDestroy();}
}
