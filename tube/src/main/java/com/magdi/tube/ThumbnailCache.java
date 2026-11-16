package com.magdi.tube;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.FileProvider;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/** Downloads existing YouTube artwork; never accesses a video stream or video frames. */
class ThumbnailCache {
 interface Result {void loaded(Uri uri);}
 static final class Art {
  final String id,url,title,author;
  final boolean next,previous;
  private Art(String id,String url,org.json.JSONObject data){this.id=id;this.url=url;title=label(data.opt("title"));author=label(data.opt("author"));next=data.optBoolean("next");previous=data.optBoolean("previous");}
  private static String label(Object value){if(!(value instanceof String))return "";String s=((String)value).replaceAll("[\\x00-\\x1f\\x7f]"," ").trim();return s.length()>300?s.substring(0,300):s;}
  static Art parse(org.json.JSONObject data){
   if(data==null)return null;
   String id=data.optString("id"),url=data.optString("url");
   if(!id.matches("[A-Za-z0-9_-]{11}")||url.length()>2048)return null;
   try{URI u=new URI(url);String host=u.getHost();
    if(!"https".equals(u.getScheme())||!("i.ytimg.com".equals(host)||"img.youtube.com".equals(host))||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||u.getFragment()!=null)return null;
    if(!u.getRawPath().matches("/vi(?:_webp)?/"+id+"/[A-Za-z0-9_-]+\\.(?:jpg|webp)"))return null;
    return new Art(id,url,data);
   }catch(Exception e){return null;}
  }
 }
 private final Context app;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ThreadPoolExecutor io=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
 private volatile boolean closed;
 ThumbnailCache(Context context){app=context.getApplicationContext();}
 void load(Art art,Result result){
  if(closed)return;
  io.execute(()->{
   Uri uri=null;
   try{File folder=new File(app.getCacheDir(),"artwork");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException();
    File file=new File(folder,key("square-fit-v1:"+art.url)+".jpg");
    if(!file.isFile()||file.length()==0){
     byte[] bytes=download(art.url);
     Bitmap decoded=decode(bytes),bitmap;
     try{bitmap=cardImage(decoded);}finally{decoded.recycle();}
     File tmp=new File(folder,file.getName()+".tmp");
     try{try(OutputStream out=new FileOutputStream(tmp)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,88,out))throw new IOException();}
      if(!tmp.renameTo(file))throw new IOException();
     }finally{bitmap.recycle();if(tmp.exists())tmp.delete();}
    }
    file.setLastModified(System.currentTimeMillis());trim(folder,file);
    uri=FileProvider.getUriForFile(app,app.getPackageName()+".reports",file);
   }catch(Exception e){/* No URLs, IDs, titles or network exception messages in diagnostics. */}
   final Uri answer=uri;main.post(()->{if(!closed)result.loaded(answer);});
  });
 }
 private static byte[] download(String url)throws IOException{
  HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
  connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
  connection.setRequestProperty("Accept","image/jpeg,image/webp");
  try{if(connection.getResponseCode()!=200||connection.getContentLengthLong()>1048576)throw new IOException();
   String type=connection.getContentType();if(type==null||!type.toLowerCase(Locale.ROOT).startsWith("image/"))throw new IOException();
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted()||out.size()+n>1048576)throw new IOException();out.write(buffer,0,n);}return out.toByteArray();
   }
  }finally{connection.disconnect();}
 }
 static Bitmap decode(byte[] bytes)throws IOException{
  BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
  if(options.outWidth<=0||options.outHeight<=0||options.outWidth>4096||options.outHeight>4096)throw new IOException();
  options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>960)options.inSampleSize*=2;
  options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException();
  int longest=Math.max(bitmap.getWidth(),bitmap.getHeight());
  if(longest>480){Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.max(1,bitmap.getWidth()*480/longest),Math.max(1,bitmap.getHeight()*480/longest),true);if(scaled!=bitmap)bitmap.recycle();bitmap=scaled;}
  return bitmap;
 }
 // Android Auto owns the artwork crop. Supply a square with the whole source
 // fitted inside it, including a small inset for rounded corners / host padding.
 static android.graphics.RectF cardBounds(int width,int height){
  if(width<=0||height<=0)throw new IllegalArgumentException("Invalid artwork dimensions");
  float scale=432f/Math.max(width,height),w=width*scale,h=height*scale;
  return new android.graphics.RectF((480-w)/2,(480-h)/2,(480+w)/2,(480+h)/2);
 }
 static Bitmap cardImage(Bitmap source){
  Bitmap result=Bitmap.createBitmap(480,480,Bitmap.Config.ARGB_8888);
  android.graphics.Canvas canvas=new android.graphics.Canvas(result);canvas.drawColor(android.graphics.Color.rgb(18,18,18));
  android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG|android.graphics.Paint.FILTER_BITMAP_FLAG);
  canvas.drawBitmap(source,null,cardBounds(source.getWidth(),source.getHeight()),paint);
  return result;
 }
 private static String key(String url)throws Exception{byte[] digest=MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"));StringBuilder key=new StringBuilder();for(byte b:digest)key.append(String.format(Locale.ROOT,"%02x",b&255));return key.toString();}
 static void trim(File folder,File keep){File[] files=folder.listFiles((d,n)->n.endsWith(".jpg"));if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified).reversed());int count=0;long bytes=0;for(File f:files){count++;bytes+=f.length();if((count>8||bytes>2097152)&&!f.equals(keep))f.delete();}}
 void close(){closed=true;io.shutdownNow();main.removeCallbacksAndMessages(null);}
}
