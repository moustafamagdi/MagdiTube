package com.magdi.tube;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class CookiePersistenceTest {
 @Test public void burstIsCoalescedAndRunsOffCallerThread()throws Exception{
  ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();CountDownLatch release=new CountDownLatch(1),done=new CountDownLatch(1);AtomicInteger writes=new AtomicInteger();AtomicReference<Thread> writer=new AtomicReference<>();Thread caller=Thread.currentThread();
  try{worker.execute(()->{try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
   CookiePersistence saver=new CookiePersistence(worker,()->{writes.incrementAndGet();writer.set(Thread.currentThread());done.countDown();},0);
   for(int i=0;i<100;i++)saver.schedule();assertEquals(0,writes.get());release.countDown();assertTrue(done.await(2,TimeUnit.SECONDS));worker.submit(()->{}).get(2,TimeUnit.SECONDS);
   assertEquals(1,writes.get());assertNotSame(caller,writer.get());
  }finally{release.countDown();worker.shutdownNow();}
 }
 @Test public void requestDuringFlushProducesOneFollowup()throws Exception{
  ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1),second=new CountDownLatch(1);AtomicInteger writes=new AtomicInteger();
  try{CookiePersistence saver=new CookiePersistence(worker,()->{if(writes.incrementAndGet()==1){started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}else second.countDown();},0);
   saver.schedule();assertTrue(started.await(2,TimeUnit.SECONDS));for(int i=0;i<100;i++)saver.schedule();release.countDown();assertTrue(second.await(2,TimeUnit.SECONDS));worker.submit(()->{}).get(2,TimeUnit.SECONDS);assertEquals(2,writes.get());
  }finally{release.countDown();worker.shutdownNow();}
 }
}
