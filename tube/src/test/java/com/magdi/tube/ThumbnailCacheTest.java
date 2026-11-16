package com.magdi.tube;

import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import java.io.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class ThumbnailCacheTest {
 @Before public void useActualImageParsing(){org.robolectric.shadows.ShadowBitmapFactory.setAllowInvalidImageData(false);}
 @After public void restoreBitmapShadow(){org.robolectric.shadows.ShadowBitmapFactory.reset();}
 private ThumbnailCache.Art parse(String url)throws Exception{return ThumbnailCache.Art.parse(new org.json.JSONObject().put("id","abcdefghijk").put("url",url));}
 @Test public void permitsOnlyMatchingVideoThumbnailOnTrustedHttpsHost()throws Exception{
  assertNotNull(parse("https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg"));
  assertNotNull(parse("https://i.ytimg.com/vi_webp/abcdefghijk/hq720.webp?sqp=example"));
  for(String url:new String[]{"https://i.ytimg.com.evil.test/vi/abcdefghijk/hqdefault.jpg","http://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg","https://i.ytimg.com/vi/12345678901/hqdefault.jpg","https://i.ytimg.com:444/vi/abcdefghijk/hqdefault.jpg","https://user:pass@i.ytimg.com/vi/abcdefghijk/hqdefault.jpg","file:///data/data/private","https://i.ytimg.com/vi/abcdefghijk/../../secret.jpg","https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg#fragment"})assertNull(url,parse(url));
 }
 @Test public void rejectsInvalidImageBytes(){try{ThumbnailCache.decode(new byte[]{1,2,3});fail("Invalid image accepted");}catch(IOException expected){}}
 @Test public void downloadedImageIsResizedWithoutChangingAspectRatio()throws Exception{
  android.graphics.Bitmap input=android.graphics.Bitmap.createBitmap(1280,720,android.graphics.Bitmap.Config.ARGB_8888);ByteArrayOutputStream bytes=new ByteArrayOutputStream();input.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);input.recycle();
  android.graphics.Bitmap result=ThumbnailCache.decode(bytes.toByteArray());assertEquals(480,result.getWidth());assertEquals(270,result.getHeight());result.recycle();
 }
 @Test public void refusesExcessiveImageDimensions()throws Exception{
  android.graphics.Bitmap input=android.graphics.Bitmap.createBitmap(4097,1,android.graphics.Bitmap.Config.ARGB_8888);ByteArrayOutputStream bytes=new ByteArrayOutputStream();input.compress(android.graphics.Bitmap.CompressFormat.PNG,100,bytes);input.recycle();
  try{ThumbnailCache.decode(bytes.toByteArray());fail("Oversized image accepted");}catch(IOException expected){}
 }
 @Test public void cacheEvictsOldImagesAndKeepsCurrent()throws Exception{
  File dir=java.nio.file.Files.createTempDirectory("thumbnail-cache-test").toFile();
  try{File keep=null;for(int i=0;i<12;i++){File f=new File(dir,i+".jpg");try(FileOutputStream out=new FileOutputStream(f)){out.write(1);}f.setLastModified(1000+i);keep=f;}
   ThumbnailCache.trim(dir,keep);assertTrue(keep.exists());assertEquals(8,dir.listFiles().length);
  }finally{for(File f:dir.listFiles())f.delete();dir.delete();}
 }
}
