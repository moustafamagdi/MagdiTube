package com.magdi.tube;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.Bundle;
import java.util.UUID;
/** Random, resettable MM-only identity. Never reads Android ID, IMEI or accounts. */
final class SharedSupportIdentity {
 static final String OWNER="com.magdi.auto", AUTHORITY=OWNER+".support.identity";
 static final String SIGNER="6d9d30d8f427a6cfc0241831d1e324e684fe2aefda72925c89ef872bf3a5ab65";
 static final String PREFS="mm_shared_support_v1";
 static final class Identity { final String id, secret; Identity(String i,String s){id=i;secret=s;} }
 static boolean valid(String id,String secret){return id!=null&&id.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")&&secret!=null&&secret.matches("[0-9a-f]{64}");}
 static boolean trusted(PackageManager pm,String pkg){
  if(!OWNER.equals(pkg)&&!"com.magdi.iptv".equals(pkg)&&!"com.magdi.tube".equals(pkg))return false;
  try {byte[] cert=new byte[32];for(int i=0;i<32;i++)cert[i]=(byte)Integer.parseInt(SIGNER.substring(i*2,i*2+2),16);return pm.hasSigningCertificate(pkg,cert,PackageManager.CERT_INPUT_SHA256);}catch(Exception e){return false;}
 }
 static synchronized Identity owned(Context c){
  SharedPreferences p=c.getSharedPreferences(PREFS,0);Identity old=read(p);if(old!=null)return old;
  Identity created=new Identity(UUID.randomUUID().toString(),UUID.randomUUID().toString().replace("-","")+UUID.randomUUID().toString().replace("-",""));
  if(!p.edit().putString("id",created.id).putString("secret",created.secret).commit())throw new IllegalStateException("Identity persistence failed");return created;
 }
 private static Identity read(SharedPreferences p){String id=p.getString("id",null),secret=p.getString("secret",null);return valid(id,secret)?new Identity(id,secret):null;}
 /** Called on the reporter IO executor, never on the UI thread. */
 static final java.util.concurrent.ExecutorService lookup=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"MMIdentity");t.setDaemon(true);return t;});
 static java.util.concurrent.Future<Identity> pending;
 static synchronized Identity resolve(Context c){
  if(OWNER.equals(c.getPackageName()))return owned(c);
  if(pending==null)pending=lookup.submit(()->probe(c.getApplicationContext()));
  try{Identity found=pending.get(3,java.util.concurrent.TimeUnit.SECONDS);pending=null;return found;}catch(Exception ignored){if(pending.isDone())pending=null;return read(c.getSharedPreferences(PREFS,0));}
 }
 private static Identity probe(Context c){
  if(OWNER.equals(c.getPackageName()))return owned(c);
  SharedPreferences p=c.getSharedPreferences(PREFS,0);
  try {PackageManager pm=c.getPackageManager();ProviderInfo provider=pm.resolveContentProvider(AUTHORITY,0);
   if(provider!=null&&OWNER.equals(provider.packageName)&&trusted(pm,OWNER)){
    try(ContentProviderClient client=c.getContentResolver().acquireUnstableContentProviderClient(Uri.parse("content://"+AUTHORITY))){
     if(client!=null){Bundle b=client.call("identity",null,null);if(b!=null&&valid(b.getString("id"),b.getString("secret"))){
      Identity next=new Identity(b.getString("id"),b.getString("secret"));if(p.edit().putString("id",next.id).putString("secret",next.secret).commit())return next;
     }}
    }
   }
  }catch(Exception ignored){}return read(p);
 }
 private SharedSupportIdentity(){}
}
