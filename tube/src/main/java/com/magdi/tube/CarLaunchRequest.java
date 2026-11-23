package com.magdi.tube;

import android.content.*;
import android.os.*;
import android.support.car.Car;
import android.support.car.CarConnectionCallback;
import com.google.android.apps.auto.sdk.service.CarFirstPartyManager;

/** Best-effort launch with an optional one-shot selection scoped to its readiness deadline. */
final class CarLaunchRequest {
 static final long TIMEOUT_MS=8000;
 interface Events { void connected(); void disconnected(); }
 interface Backend { void connect(Events events); void launch(Intent intent)throws Exception; void close(); }
 interface Factory { Backend create(); }
 interface Host { boolean visible(); boolean launch(Intent intent); }
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final Factory factory;private final Host host;private final Intent intent;private final Runnable changed;
 private Backend backend;private Object attempt;private boolean sent;private String status="idle";
 private Runnable deadline,poll;
 private java.util.function.BooleanSupplier selection;
 CarLaunchRequest(Context context,Class<?> service,Runnable changed){
  this(context,service,()->new Sdk(context.getApplicationContext()),new Host(){
   public boolean visible(){return CarPlaybackSession.visible();}
   public boolean launch(Intent i){return CarPlaybackSession.show(i);}
  },changed);
 }
 CarLaunchRequest(Context context,Class<?> service,Factory factory,Host host,Runnable changed){
  this.factory=factory;this.host=host;this.changed=changed;
  intent=new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(context,service))
   .addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION").putExtra(Intent.EXTRA_PACKAGE_NAME,context.getPackageName());
 }
 String status(){return "opened".equals(status)&&!host.visible()?"idle":status;}
 boolean pending(){return attempt!=null;}
 void request(){request(null);}
 void request(java.util.function.BooleanSupplier playback){
  if(playback!=null)selection=playback;
  if(pending())return;
  if(selection!=null&&host.visible()&&selection.getAsBoolean())selection=null;
  if(host.visible()&&selection==null){status="opened";changed.run();return;}
  Object own=new Object();attempt=own;sent=false;status="opening";changed.run();
  deadline=()->finish(own,"timeout");handler.postDelayed(deadline,TIMEOUT_MS);
  poll=new Runnable(){public void run(){if(attempt!=own)return;if(sent&&host.visible()&&(selection==null||selection.getAsBoolean()))finish(own,"opened");else handler.postDelayed(this,150);}};
  handler.post(poll);
  if(host.visible()){sent=true;return;}
  try{
   if(host.launch(new Intent(intent))){sent=true;Diagnostics.event("Car UI launch requested: existing host");return;}
   backend=factory.create();Backend b=backend;
   b.connect(new Events(){
    public void connected(){handler.post(()->{if(attempt!=own||backend!=b||sent)return;try{b.launch(new Intent(intent));sent=true;Diagnostics.event("Car UI launch requested: SDK");}catch(SecurityException e){finish(own,"denied");}catch(Exception|LinkageError e){finish(own,"unavailable");}});}
    public void disconnected(){handler.post(()->finish(own,"unavailable"));}
   });
  }catch(SecurityException e){finish(own,"denied");}catch(RuntimeException|LinkageError e){finish(own,"unavailable");}
 }
 void cancel(){if(attempt!=null)finish(attempt,"cancelled");}
 private void finish(Object own,String result){
  if(attempt!=own)return;attempt=null;selection=null;status=result;
  if(deadline!=null)handler.removeCallbacks(deadline);if(poll!=null)handler.removeCallbacks(poll);
  Backend old=backend;backend=null;if(old!=null)try{old.close();}catch(RuntimeException|LinkageError ignored){}
  Diagnostics.event("Car UI launch result="+result);changed.run();
 }
 private static final class Sdk implements Backend {
  private final Context context;private Car car;
  Sdk(Context context){this.context=context;}
  public void connect(Events events){
   car=Car.createCar(context,new CarConnectionCallback(){
    public void onConnected(Car c){events.connected();}
    public void onDisconnected(Car c){events.disconnected();}
   });
   if(car==null){events.disconnected();return;}car.connect();
  }
  public void launch(Intent intent)throws Exception {
   if(car==null||!car.isConnected())throw new IllegalStateException();
   int type=car.getCarConnectionType();
   if(type!=Car.CONNECTION_TYPE_USB&&type!=Car.CONNECTION_TYPE_WIFI&&type!=Car.CONNECTION_TYPE_EMULATOR&&type!=Car.CONNECTION_TYPE_ADB_EMULATOR&&type!=Car.CONNECTION_TYPE_ON_DEVICE_EMULATOR)throw new IllegalStateException();
   Object manager=car.getCarManager(CarFirstPartyManager.SERVICE_NAME);
   if(!(manager instanceof CarFirstPartyManager))throw new IllegalStateException();
   ((CarFirstPartyManager)manager).startCarActivity(intent);
  }
  public void close(){Car old=car;car=null;if(old!=null)old.disconnect();}
 }
}
