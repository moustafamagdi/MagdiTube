package com.magdi.tube;
import org.junit.*;import java.io.*;import java.nio.file.*;import static org.junit.Assert.*;
public class ReportExportTest {
 @Test public void exportsAreNamedImmutableAndCollisionSafe()throws Exception {
  File cache=Files.createTempDirectory("mm-report").toFile();long now=1795843020000L;
  File one=ReportExport.write(cache,"first",now),two=ReportExport.write(cache,"second",now);
  assertTrue(one.getName().startsWith("MM-Tube-report-20261128-051700-000Z"));
  assertTrue(ReportExport.validName(one.getName()));assertTrue(ReportExport.validName(two.getName()));assertNotEquals(one,two);
  String content=new String(Files.readAllBytes(one.toPath()),java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(content.contains("Exported at: 2026-11-28T05:17:00.000Z"));assertTrue(content.endsWith("first"));
  assertFalse(ReportExport.validName("../"+one.getName()));assertFalse(ReportExport.validName("MM-support-report.txt"));
  one.delete();two.delete();new File(cache,"reports").delete();cache.delete();
 }
}
