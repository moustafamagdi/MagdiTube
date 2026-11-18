package com.magdi.tube;

import java.util.concurrent.*;

/** Coalesces disk flushes off the UI thread without clearing cookies. */
final class CookiePersistence {
 private static final CookiePersistence shared=new CookiePersistence(Executors.newSingleThreadScheduledExecutor(r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"MagdiCookieIO")),ControllerCookies::flush,500);
 private final ScheduledExecutorService worker;
 private final Runnable flush;
 private final long delay;
 private boolean queued;
 CookiePersistence(ScheduledExecutorService worker,Runnable flush,long delay){this.worker=worker;this.flush=flush;this.delay=delay;}
 static void request(){shared.schedule();}
 synchronized void schedule(){
  if(queued)return;queued=true;
  worker.schedule(()->{
   synchronized(this){queued=false;} // A request during the write schedules one follow-up.
   try{flush.run();}catch(RuntimeException e){Diagnostics.event("Cookie persistence unavailable");}
  },delay,TimeUnit.MILLISECONDS);
 }
}
