package com.magdi.tube;
import android.app.Activity;import android.os.Bundle;import android.content.Intent;import android.net.Uri;import android.widget.*;
/** Installation maintenance belongs to MM Auto; never exports or reinstalls this APK. */
public final class InstallSetupActivity extends Activity {
 public void onCreate(Bundle state){super.onCreate(state);boolean ar="ar".equals(getIntent().getStringExtra("language"))||getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
 LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);int pad=UiStyle.dp(this,24);panel.setPadding(pad,pad,pad,pad);panel.setLayoutDirection(ar?4:3);setContentView(panel);
 TextView title=new TextView(this);title.setText(ar?"التثبيت والتحديث عبر MM Auto":"Installation & updates with MM Auto");title.setTextSize(24);panel.addView(title);
 TextView help=new TextView(this);help.setText(ar?"افتح MM Auto، واختار التطبيق ثم المساعدة لإكمال الإعداد. بياناتك الحالية تفضل محفوظة. لو مش عندك MM Auto، حمّله من صفحة الإصدارات.":"Open MM Auto, choose this app, then Help to complete setup. Your existing data stays in place. If MM Auto is not installed, download it from its releases page.");help.setTextSize(17);panel.addView(help);
 Button open=UiStyle.button(this,ar?"فتح MM Auto":"Open MM Auto",true);panel.addView(open,UiStyle.actionParams(this));open.setOnClickListener(v->{try{startActivity(new Intent(Intent.ACTION_MAIN).setComponent(new android.content.ComponentName("com.magdi.auto","com.magdi.auto.MainActivity")));}catch(android.content.ActivityNotFoundException e){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://github.com/moustafamagdi/MM-Auto/releases")));}catch(RuntimeException unavailable){Toast.makeText(this,ar?"افتح صفحة MM Auto على GitHub":"Open MM Auto releases on GitHub",Toast.LENGTH_LONG).show();}}});
 Button back=UiStyle.button(this,ar?"رجوع":"Back",false);panel.addView(back,UiStyle.actionParams(this));back.setOnClickListener(v->finish());
 }
}
