package com.magdi.tube;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

/** Accessed only on Diagnostics' single I/O queue. Each selectable part is clipboard-sized. */
final class DiagnosticStore {
 static final int PART_BYTES=128*1024, MAX_PARTS=40;
 static final long MAX_AGE_MS=7L*24*60*60*1000;
 private final File directory;
 private String session;
 private int part;
 private File current;
 DiagnosticStore(File files)throws IOException {
  directory=new File(files,"diagnostic-sessions");
  if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create log directory");
 }
 void start(String kind,long now)throws IOException {
  session=new SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.US).format(new Date(now))+"-"+kind+"-"+UUID.randomUUID().toString().substring(0,8);
  part=0;nextPart();append("=== Session started: "+session+" ===\n",now);prune(now);
 }
 private void nextPart(){current=new File(directory,session+String.format(Locale.US,"-part%03d.txt",++part));}
 void append(String text,long now)throws IOException {
  if(current==null)start("App",now);
  // Events are bounded; imported legacy text is passed one line at a time.
  byte[] bytes=text.getBytes(StandardCharsets.UTF_8);
  if(bytes.length>PART_BYTES)bytes="[Oversized diagnostic event omitted]\n".getBytes(StandardCharsets.UTF_8);
  if(current.length()+bytes.length>PART_BYTES){nextPart();}
  try(OutputStream out=new FileOutputStream(current,true)){out.write(bytes);}
  current.setLastModified(now);prune(now);
 }
 List<File> list(){
  File[] found=directory.listFiles((dir,name)->name.endsWith(".txt"));
  List<File> result=new ArrayList<>(found==null?Collections.emptyList():Arrays.asList(found));
  result.sort(Comparator.<File,Boolean>comparing(file->file.equals(current)).reversed().thenComparing(Comparator.comparing(File::getName).reversed()));return result;
 }
 String read(String name)throws IOException {
  for(File file:list())if(file.getName().equals(name))return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
  throw new FileNotFoundException("Log part expired");
 }
 String currentName(){return current==null?null:current.getName();}
 void prune(long now){
  List<File> files=list();int kept=0;
  for(File file:files){boolean active=file.equals(current);
   if(!active&&(now-file.lastModified()>MAX_AGE_MS||kept>=MAX_PARTS)){file.delete();}else kept++;
  }
 }
 void migrateLegacy(File files,long now)throws IOException {
  for(String name:new String[]{"diagnostics.previous.txt","diagnostics.txt"}){
   File legacy=new File(files,name);if(!legacy.exists())continue;
   start("Imported",now);
   try(BufferedReader in=new BufferedReader(new InputStreamReader(new FileInputStream(legacy),StandardCharsets.UTF_8))){String line;while((line=in.readLine())!=null)append(line+"\n",now);}
   // Retire only after the old file has been copied successfully.
   if(!legacy.delete())throw new IOException("Cannot retire legacy log");
  }
 }
}
