package com.magdi.tube;

import android.content.*;
import android.os.IBinder;
import android.os.SystemClock;

/** Keeps the existing media-card service bound; never starts a player or requests audio focus.
 * Main thread only. Failure/missing callbacks get at most three attempts per selection.
 */
final class MediaCardBinding {
 private static final long RETRY_MS=10000;
 private Link current;
 private int attempts;
 private long retryAt;
 void ensure(Context context){
  long now=SystemClock.elapsedRealtime();
  if(current!=null){
   if(current.connected||now-current.started<RETRY_MS)return;
   disconnect();Diagnostics.event("Media card binding timed out");
  }
  if(attempts>=3||now<retryAt)return;
  attempts++;retryAt=now+RETRY_MS;
  Link link=new Link(context.getApplicationContext(),now);current=link;
  try{
   boolean bound=link.context.bindService(new Intent(link.context,MediaCardService.class).setAction("android.media.browse.MediaBrowserService"),link,Context.BIND_AUTO_CREATE);
   if(!bound){disconnect();Diagnostics.event("Media card binding unavailable");}
  }catch(RuntimeException e){disconnect();Diagnostics.event("Media card binding failed: "+e.getClass().getSimpleName());}
 }
 void release(){disconnect();attempts=0;retryAt=0;}
 private void disconnect(){
  Link old=current;current=null;
  if(old!=null)try{old.context.unbindService(old);}catch(RuntimeException ignored){}
 }
 private final class Link implements ServiceConnection {
  final Context context;long started;boolean connected;
  Link(Context context,long started){this.context=context;this.started=started;}
  public void onServiceConnected(ComponentName name,IBinder binder){if(current!=this)return;connected=true;Diagnostics.event("Media card automatically connected");}
  public void onServiceDisconnected(ComponentName name){if(current!=this)return;connected=false;started=SystemClock.elapsedRealtime();Diagnostics.event("Media card connection suspended");}
  public void onBindingDied(ComponentName name){failed();}
  public void onNullBinding(ComponentName name){failed();}
  private void failed(){if(current!=this)return;disconnect();Diagnostics.event("Media card binding lost");}
 }
}

