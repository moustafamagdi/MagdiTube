package com.magdi.tube;
/** Observations of our own view only. Never claims camera or Google Maps visibility. */
final class CarUiDiagnostics {
 private static Boolean full,fill;
 static void event(String state){Diagnostics.event("Car UI observation="+state+"; flags="+CarPlaybackSession.observationFlags());}
 static void player(org.json.JSONObject state){
  if(state.has("fullscreen")){boolean next=state.optBoolean("fullscreen");if(full==null||full!=next){full=next;event(next?"fullscreen-on":"fullscreen-off");}}
  if(state.has("fill")){boolean next=state.optBoolean("fill");if(fill==null||fill!=next){fill=next;event(next?"fill-on":"fill-off");}}
 }
 static void reset(){full=null;fill=null;}
 private CarUiDiagnostics(){}
}
