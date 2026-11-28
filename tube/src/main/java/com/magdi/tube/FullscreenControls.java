package com.magdi.tube;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

/** Visibility and input only; never issues player commands or changes media state. */
final class FullscreenControls {
 static final long HIDE_MS=5000;
 private final Handler handler;
 private View exit,fit,seek;
 private boolean held,keyboard;
 private final Runnable hide=new Runnable(){public void run(){
  if(exit==null||held)return;
  AccessibilityManager accessibility=(AccessibilityManager)exit.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
  if((accessibility!=null&&accessibility.isTouchExplorationEnabled())||focused(exit)||focused(fit)||focused(seek)){schedule();return;}
  exit.setVisibility(View.INVISIBLE);fit.setVisibility(View.INVISIBLE);if(seek!=null)seek.setVisibility(View.INVISIBLE);
 }};
 FullscreenControls(Handler handler){this.handler=handler;}
 private boolean focused(View view){return view!=null&&(view.isAccessibilityFocused()||keyboard&&view.hasFocus());}
 void bind(View exit,View fit){bind(exit,fit,null);}
 void bind(View exit,View fit,View seek){clear();this.exit=exit;this.fit=fit;this.seek=seek;reveal();}
 void clear(){handler.removeCallbacks(hide);exit=null;fit=null;seek=null;held=false;keyboard=false;}
 void onKey(){keyboard=true;reveal();}
 void reveal(){if(exit==null)return;exit.setVisibility(View.VISIBLE);fit.setVisibility(View.VISIBLE);if(seek!=null)seek.setVisibility(View.VISIBLE);schedule();}
 private void schedule(){handler.removeCallbacks(hide);if(exit!=null&&!held)handler.postDelayed(hide,HIDE_MS);}
 void beforeTouch(MotionEvent event){
  if(exit!=null&&event.getActionMasked()==MotionEvent.ACTION_DOWN){held=true;keyboard=false;handler.removeCallbacks(hide);}
 }
 void afterTouch(MotionEvent event){
  if(exit==null||!held)return;
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_UP){held=false;reveal();}
  else if(action==MotionEvent.ACTION_CANCEL){held=false;schedule();}
  // Keep hidden buttons out of hit testing for the entire original gesture. The player
  // receives it normally; only after dispatch finishes do our buttons become visible.
 }
}
