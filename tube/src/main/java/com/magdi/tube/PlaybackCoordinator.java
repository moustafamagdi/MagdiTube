package com.magdi.tube;

import java.util.*;

/** Main-thread only; hidden phone pages yield to audible car playback. */
final class PlaybackCoordinator {
 private static final Map<TubeView,Boolean> pages=new WeakHashMap<>();
 static void register(TubeView view){pages.put(view,false);}
 static void observe(TubeView view,boolean audiblePlaying){
  pages.put(view,view.isCarView()&&audiblePlaying);
  enforce();
 }
 static void enforce(){
  if(!pages.containsValue(true))return;
  for(TubeView view:new ArrayList<>(pages.keySet()))if(view!=null&&!view.isCarView())view.suppressHiddenPhone();
 }
 static void forget(TubeView view){pages.remove(view);}
}
