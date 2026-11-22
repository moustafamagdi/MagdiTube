package com.magdi.tube;
import org.json.*;
import java.util.regex.*;
/** Closed vocabulary: unknown log messages and all free text are dropped. */
final class SupportPolicy {
 static final String MEDIA="play|pause|waiting|playing|error|ended|telemetry|fullscreen|video-frozen|video-recovery-start|video-recovery-failed|video-recovery-complete|seek-start|seek-retry|seek-failed|seek-complete";
 static JSONObject event(String s,long at){try{
  JSONObject o=new JSONObject().put("at",at);Matcher m=Pattern.compile("^(car|phone) media=("+MEDIA+") time=(\\d+) ready=(\\d+) frames=(\\d+) engine=(native|hls|ts|unknown) code=(-?\\d+)$").matcher(s);
  if(m.matches())return o.put("kind","media").put("state",m.group(2)).put("time",bounded(m.group(3))).put("ready",bounded(m.group(4))).put("frames",bounded(m.group(5))).put("code",bounded(m.group(7)));
  if(s.startsWith("Playback source=car ")){int start=s.indexOf('{');if(start<0)return null;JSONObject j=new JSONObject(s.substring(start));o.put("kind","media").put("state",j.optBoolean("playing")?"playing":"pause");for(String k:new String[]{"time","ready","sampleDelayMs","callbackMs","appJavaHeapMiB"})if(j.has(k))o.put(k,bounded(j.optString(k)));JSONObject p=j.optJSONObject("performance");if(p!=null){o.put("frames",bounded(p.optString("totalFrames")));o.put("dropped",bounded(p.optString("droppedFrames")));}return o;}
  if(s.startsWith("Process started; version="))return o.put("kind","app-start");
  if(s.equalsIgnoreCase("Car view created")||s.equals("Car playback session created"))return o.put("kind","car-open");
  if(s.equals("Controller disconnected; pending requests cancelled"))return o.put("kind","car-disconnected");
  if(s.equals("Media card browser accepted"))return o.put("kind","audio-browser");
  if(s.startsWith("Install source [")){return o.put("kind","install-check").put("state",s.contains("metadata=MISMATCH")?"mismatch":s.contains("metadata=MATCH")?"match":"unknown");}
  m=Pattern.compile("^Stream HTTP status=([1-5][0-9]{2})$").matcher(s);if(m.matches())return o.put("kind","http-error").put("code",Integer.parseInt(m.group(1)));
  m=Pattern.compile("^Stream connection error=(UnknownHostException|SocketException|SocketTimeoutException|SSLHandshakeException|IOException)$").matcher(s);if(m.matches())return o.put("kind","network-error").put("state",m.group(1));
  if(s.startsWith("Controller observation=")){String v=s.substring(23);if(v.matches("accepted|current-loading|media-mismatch|request-mismatch|connection-mismatch|sample-unavailable|helper-unavailable|stale|document-mismatch|disconnected"))return o.put("kind","controller").put("state",v);}
  if(s.startsWith("Background service unavailable:"))return o.put("kind","background-unavailable");
  if(s.equals("Support crash observed"))return o.put("kind","crash");
  return null;
 }catch(Exception e){return null;}}
 static long bounded(String s){try{return Math.max(-10000,Math.min(1000000000000L,Long.parseLong(s)));}catch(Exception e){return 0;}}
 private SupportPolicy(){}
}
