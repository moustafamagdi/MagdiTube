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
 private View exit,fit;
 private boolean held,swallowTouch,keyboard;
 private final Runnable hide=new Runnable(){public void run(){
  if(exit==null||held)return;
  AccessibilityManager accessibility=(AccessibilityManager)exit.getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
  if((accessibility!=null&&accessibility.isTouchExplorationEnabled())||focused(exit)||focused(fit)){schedule();return;}
  exit.setVisibility(View.INVISIBLE);fit.setVisibility(View.INVISIBLE);
 }};
 FullscreenControls(Handler handler){this.handler=handler;}
 private boolean focused(View view){return view.isAccessibilityFocused()||keyboard&&view.isFocused();}
 void bind(View exit,View fit){clear();this.exit=exit;this.fit=fit;reveal();}
 void clear(){handler.removeCallbacks(hide);exit=null;fit=null;held=false;swallowTouch=false;keyboard=false;}
 void onKey(){keyboard=true;reveal();}
 void reveal(){if(exit==null)return;exit.setVisibility(View.VISIBLE);fit.setVisibility(View.VISIBLE);schedule();}
 private void schedule(){handler.removeCallbacks(hide);if(exit!=null&&!held)handler.postDelayed(hide,HIDE_MS);}
 boolean onTouch(MotionEvent event){
  if(exit==null)return false;
  int action=event.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){swallowTouch=exit.getVisibility()!=View.VISIBLE;held=true;keyboard=false;reveal();}
  boolean consume=swallowTouch;
  if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){held=false;swallowTouch=false;schedule();}
  // The first hidden-controls gesture only reveals, even when over an old button position.
  return consume;
 }
}
