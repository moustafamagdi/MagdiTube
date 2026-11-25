package com.magdi.tube;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;
import android.widget.LinearLayout;

/** Shared phone UI styling; actions and enabled state remain owned by their screens. */
final class UiStyle {
 private UiStyle(){}
 static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static boolean dark(Context c){return (c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;}
 static void systemBars(android.app.Activity activity){android.view.WindowInsetsController controller=activity.getWindow().getInsetsController();if(controller!=null){int mask=android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;controller.setSystemBarsAppearance(dark(activity)?0:mask,mask);}}
 static int border(Context c){return dark(c)?0xff354964:0xffc4d2e7;}
 static GradientDrawable surface(Context c,int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));if(stroke!=0)d.setStroke(dp(c,1),stroke);return d;}
 static Button button(Context c,String title,boolean primary){Button b=new Button(c);b.setText(title);style(b,primary);return b;}
 static void style(Button b,boolean primary){
  Context c=b.getContext();boolean dark=dark(c);int accent=dark?0xff8bb4ff:0xff2563eb;
  int fill=primary?accent:(dark?0xff1b2c44:0xffedf3ff),ink=primary?(dark?0xff0b1729:Color.WHITE):(dark?0xffdce9ff:0xff244878);
  int disabledFill=dark?0xff1a2535:0xffe8edf4,disabledInk=dark?0xff718198:0xff8793a6;
  b.setAllCaps(false);b.setTextSize(15);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
  b.setMinHeight(dp(c,52));b.setMinimumHeight(dp(c,52));b.setMinWidth(dp(c,48));b.setMinimumWidth(dp(c,48));b.setPaddingRelative(dp(c,16),dp(c,10),dp(c,16),dp(c,10));
  ColorStateList colors=new ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{disabledInk,ink});b.setTextColor(colors);b.setCompoundDrawableTintList(colors);
  StateListDrawable states=new StateListDrawable();states.addState(new int[]{-android.R.attr.state_enabled},surface(c,disabledFill,14,border(c)));states.addState(new int[]{android.R.attr.state_focused},surface(c,fill,14,accent));states.addState(new int[]{},surface(c,fill,14,primary?accent:border(c)));
  b.setBackgroundTintList(null);b.setBackground(new RippleDrawable(ColorStateList.valueOf(dark?0x409dbdff:0x302563eb),states,surface(c,Color.WHITE,14,0)));b.setElevation(0);b.setStateListAnimator(null);
 }
 static void icon(Button b,int resource){android.graphics.drawable.Drawable d=b.getContext().getDrawable(resource).mutate();d.setBounds(0,0,dp(b.getContext(),20),dp(b.getContext(),20));b.setCompoundDrawablesRelative(d,null,null,null);b.setCompoundDrawablePadding(dp(b.getContext(),10));}
 static LinearLayout.LayoutParams actionParams(Context c){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(c,8);return p;}
}
