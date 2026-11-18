package com.magdi.tube;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class DiagnosticStoreTest {
 @Rule public TemporaryFolder folder=new TemporaryFolder();
 private DiagnosticStore store;
 private final long now=1795000000000L;
 @Before public void setup()throws Exception{store=new DiagnosticStore(folder.getRoot());}
 @Test public void newSessionDoesNotMixOrDeletePreviousRecording()throws Exception{
  store.start("Car",now);store.append("old event\n",now);String first=store.currentName();
  store.start("Manual",now);store.append("new event\n",now);String second=store.currentName();
  assertNotEquals(first,second);assertTrue(store.read(first).contains("old event"));assertFalse(store.read(first).contains("new event"));assertFalse(store.read(second).contains("old event"));assertEquals(second,store.list().get(0).getName());
 }
 @Test public void longSessionSplitsAtWholeEventWithoutLosingUnicode()throws Exception{
  store.start("Car",now);String line=String.join("",Collections.nCopies(1000,"ش"))+"\n";
  for(int i=0;i<100;i++)store.append(i+":"+line,now+i);
  assertEquals(2,store.list().size());StringBuilder all=new StringBuilder();List<File> parts=store.list();Collections.reverse(parts);
  for(File file:parts){assertTrue(file.length()<=DiagnosticStore.PART_BYTES);all.append(store.read(file.getName()));}
  for(int i=0;i<100;i++)assertTrue(all.toString().contains(i+":"+line));
 }
 @Test public void retentionBoundsCountAndRemovesExpiredParts()throws Exception{
  for(int i=0;i<45;i++)store.start("Car",now+i);
  assertEquals(40,store.list().size());String active=store.currentName();
  store.prune(now+DiagnosticStore.MAX_AGE_MS+1000);assertEquals(1,store.list().size());assertEquals(active,store.list().get(0).getName());
  store.start("Manual",now+DiagnosticStore.MAX_AGE_MS+2000);assertEquals(1,store.list().size());
 }
 @Test public void migrationPreservesLegacyOrderAndRunsOnlyOnce()throws Exception{
  File old=new File(folder.getRoot(),"diagnostics.previous.txt"),current=new File(folder.getRoot(),"diagnostics.txt");
  Files.write(old.toPath(),"previous\n".getBytes(StandardCharsets.UTF_8));Files.write(current.toPath(),"current\n".getBytes(StandardCharsets.UTF_8));
  store.migrateLegacy(folder.getRoot(),now);assertFalse(old.exists());assertFalse(current.exists());assertEquals(2,store.list().size());
  assertTrue(store.read(store.currentName()).contains("current\n"));store.migrateLegacy(folder.getRoot(),now);assertEquals(2,store.list().size());
 }
 @Test public void restartRetainsExistingPartsAndReadCannotEscapeDirectory()throws Exception{
  store.start("Car",now);String name=store.currentName();store.append("kept\n",now);
  DiagnosticStore restarted=new DiagnosticStore(folder.getRoot());restarted.start("App",now+1000);assertTrue(restarted.read(name).contains("kept"));
  try{restarted.read("../diagnostics.txt");fail("Expected rejection");}catch(FileNotFoundException expected){}
 }
}
