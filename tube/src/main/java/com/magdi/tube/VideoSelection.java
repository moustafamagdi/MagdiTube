package com.magdi.tube;

import android.net.Uri;

/** Only navigation to a YouTube video; never accepts a media URL or credentials. */
final class VideoSelection {
 final String id,url;
 private VideoSelection(String id,String url){this.id=id;this.url=url;}
 static VideoSelection parse(String value){
  try {
   if(value==null||value.length()>4096)return null;
   Uri u=Uri.parse(value);String h=u.getHost(),id=null;
   if(!"https".equals(u.getScheme())||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return null;
   if("youtu.be".equals(h)&&u.getPathSegments().size()==1)id=u.getLastPathSegment();
   else if("youtube.com".equals(h)||"www.youtube.com".equals(h)||"m.youtube.com".equals(h)){
    if("/watch".equals(u.getPath()))id=u.getQueryParameter("v");
    else if(u.getPathSegments().size()==2&&("shorts".equals(u.getPathSegments().get(0))||"live".equals(u.getPathSegments().get(0))))id=u.getLastPathSegment();
   }
   if(id==null||!id.matches("[A-Za-z0-9_-]{11}"))return null;
   Uri.Builder b=Uri.parse("https://m.youtube.com/watch").buildUpon().appendQueryParameter("v",id);
   String list=u.getQueryParameter("list");if(list!=null&&list.matches("[A-Za-z0-9_-]{10,100}"))b.appendQueryParameter("list",list);
   return new VideoSelection(id,b.build().toString());
  }catch(RuntimeException e){return null;}
 }
}
