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
 private ThumbnailCache thumbnails;
 private String requestedArtwork="",publishedArtwork="";
 private android.net.Uri artwork;
 private long artworkGeneration;
 static final class State {
  final boolean playing,muted,ready;final long position;
  final ThumbnailCache.Art art;
  State(boolean playing,boolean muted,boolean ready,long position,ThumbnailCache.Art art){this.playing=playing;this.muted=muted;this.ready=ready;this.position=position;this.art=art;}
 }
 static void observe(TubeView view,boolean playing,boolean muted,boolean ready,long position){
  observe(view,playing,muted,ready,position,null);
 }
 static void observe(TubeView view,boolean playing,boolean muted,boolean ready,long position,ThumbnailCache.Art art){
  State old=observations.put(view,new State(playing,muted,ready,position,art));TubeView current=selected.get();
  // Muted feed previews must not take the car video's controls. Keep a paused target.
  if(ready&&!muted&&(current==null||(!current.isCarView()&&view.isCarView()))){selected=new WeakReference<>(view);Diagnostics.event("Media card target selected: "+(view.isCarView()?"car":"phone"));}
  if(selected.get()==view&&running.get()!=null&&(old==null||old.playing!=playing||old.ready!=ready))Diagnostics.event("Media card state reported: playing="+playing+"; ready="+ready+"; muted="+muted);
  if(selected.get()==view)refresh();
 }
 static void paused(TubeView view){State s=observations.get(view);if(s!=null)observe(view,false,s.muted,s.ready,s.position,s.art);}
 static void clearArtwork(TubeView view){State s=observations.get(view);if(s!=null)observe(view,s.playing,s.muted,s.ready,s.position,null);}
 static TubeView target(){return selected.get();}
 static boolean retains(TubeView view){return running.get()!=null&&selected.get()==view;}
 static void forget(TubeView view){observations.remove(view);if(selected.get()==view)selected=new WeakReference<>(null);refresh();}
 private static void refresh(){MediaCardService service=running.get();if(service!=null)service.publish();}
 // This session only controls an already selected video; it has no searchable catalog.
 @android.annotation.SuppressLint("MissingOnPlayFromSearch")
 @Override public void onCreate(){super.onCreate();thumbnails=new ThumbnailCache(this);running=new WeakReference<>(this);session=new MediaSessionCompat(this,"MagdiTubeAudio");
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
  MediaDescriptionCompat description=new MediaDescriptionCompat.Builder().setMediaId(CURRENT).setTitle(ar()?"صوت الفيديو الحالي":"Current video audio").setIconUri(artworkUri()).setSubtitle(available?(ar()?"تشغيل وإيقاف مؤقت":"Play and pause"):(ar()?"افتح Magdi Tube واختر فيديو أولًا":"Open Magdi Tube and choose a video first")).build();
  result.sendResult(Collections.singletonList(new MediaBrowserCompat.MediaItem(description,available?MediaBrowserCompat.MediaItem.FLAG_PLAYABLE:0)));
 }
 private void publish(){
  if(session==null)return;State state=observations.get(selected.get());boolean now=state!=null&&state.ready;
  updateArtwork(now?state.art:null);
  String icon=artworkUri().toString();boolean changed=!metadataPublished||now!=available||!icon.equals(publishedArtwork);
  if(changed){metadataPublished=true;publishedArtwork=icon;session.setMetadata(new MediaMetadataCompat.Builder().putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,CURRENT).putString(MediaMetadataCompat.METADATA_KEY_TITLE,"Magdi Tube").putString(MediaMetadataCompat.METADATA_KEY_ARTIST,now?(ar()?"صوت الفيديو الحالي":"Current video audio"):(ar()?"اختر فيديو في Magdi Tube أولًا":"Choose a video in Magdi Tube first")).putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,icon).putString(MediaMetadataCompat.METADATA_KEY_ART_URI,icon).putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,icon).build());}
  long actions=now?PlaybackStateCompat.ACTION_PLAY|PlaybackStateCompat.ACTION_PAUSE|PlaybackStateCompat.ACTION_PLAY_PAUSE|PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID:0;
  session.setPlaybackState(new PlaybackStateCompat.Builder().setActions(actions).setState(!now?PlaybackStateCompat.STATE_NONE:state.playing?PlaybackStateCompat.STATE_PLAYING:PlaybackStateCompat.STATE_PAUSED,now?state.position:0,now&&state.playing?1f:0f).build());
  available=now;if(changed)notifyChildrenChanged(ROOT);
 }
 private android.net.Uri artworkUri(){return artwork!=null?artwork:android.net.Uri.parse("android.resource://"+getPackageName()+"/drawable/ic_tube");}
 private void updateArtwork(ThumbnailCache.Art art){
  String url=art==null?"":art.url;if(url.equals(requestedArtwork))return;
  requestedArtwork=url;artwork=null;long generation=++artworkGeneration;
  if(art==null)return;
  Diagnostics.event("Media card thumbnail requested: YouTube artwork; no frame capture");
  thumbnails.load(art,uri->{
   if(session==null||generation!=artworkGeneration)return;
   if(uri!=null)try{grantUriPermission("com.google.android.projection.gearhead",uri,android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(RuntimeException e){uri=null;}
   artwork=uri;Diagnostics.event(uri==null?"Media card thumbnail unavailable; app icon fallback":"Media card YouTube thumbnail ready");publish();
  });
 }
 void command(boolean play){
  TubeView view=selected.get();State state=observations.get(view);
  if(view==null||state==null||!state.ready){Diagnostics.event("Media card command ignored: no current video");publish();return;}
  Diagnostics.event("Media card command: "+(play?"PLAY":"PAUSE")+" target="+(view.isCarView()?"car":"phone"));
  if(play)view.playFromMediaCard();else view.pauseMedia();
 }
 @Override public void onDestroy(){if(running.get()==this)running=new WeakReference<>(null);artworkGeneration++;if(thumbnails!=null)thumbnails.close();if(session!=null){session.setActive(false);session.release();session=null;}Diagnostics.event("Media card service disconnected");super.onDestroy();}
}
