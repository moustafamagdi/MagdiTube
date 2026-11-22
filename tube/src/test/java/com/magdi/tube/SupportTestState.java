package com.magdi.tube;
final class SupportTestState {
 static void reset() throws Exception {
  SupportReporter.io.submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS);
  java.lang.reflect.Field periodic=SupportReporter.class.getDeclaredField("periodic");periodic.setAccessible(true);
  java.util.concurrent.Future<?> timer=(java.util.concurrent.Future<?>)periodic.get(null);if(timer!=null)timer.cancel(false);
  for(String name:new String[]{"initialized","enabled"}){java.lang.reflect.Field f=SupportReporter.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(null,false);}
 }
 private SupportTestState(){}
}
