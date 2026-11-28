package com.magdi.tube;
import java.io.*;import java.nio.charset.StandardCharsets;import java.text.SimpleDateFormat;import java.util.*;
/** Immutable share snapshots. File names are ASCII and timestamps are UTC. */
final class ReportExport {
 static final String PREFIX="MM-Tube-report-";
 static boolean validName(String name){return name!=null&&name.matches("MM-Tube-report-[0-9]{8}-[0-9]{6}-[0-9]{3}Z(-[0-9]+)?\\.txt");}
 static synchronized File write(File cache,String text,long now)throws IOException {
  File dir=new File(cache,"reports");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Report directory unavailable");
  File[] previous=dir.listFiles();if(previous!=null)for(File f:previous)if(validName(f.getName())&&now-f.lastModified()>7L*86400000)f.delete();
  SimpleDateFormat stamp=new SimpleDateFormat("yyyyMMdd-HHmmss-SSS'Z'",Locale.US);stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
  String base=PREFIX+stamp.format(new Date(now));File file=new File(dir,base+".txt");int suffix=0;
  while(!file.createNewFile())file=new File(dir,base+"-"+(++suffix)+".txt");
  SimpleDateFormat iso=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.US);iso.setTimeZone(TimeZone.getTimeZone("UTC"));
  String header="MM-Tube support report\nExported at: "+iso.format(new Date(now))+" (UTC)\nPhone timezone: "+TimeZone.getDefault().getID()+"\n";
  try(FileOutputStream out=new FileOutputStream(file)){out.write((header+text).getBytes(StandardCharsets.UTF_8));}return file;
 }
 private ReportExport(){}
}
