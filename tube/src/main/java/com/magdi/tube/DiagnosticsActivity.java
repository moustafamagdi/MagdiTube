package com.magdi.tube;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import java.io.File;
import java.util.*;

/** Phone-only session selection; copy and export use the same selected part. */
public final class DiagnosticsActivity extends Activity {
 private Spinner sessions;
 private Button copy,export;
 private final List<File> files=new ArrayList<>();
 private boolean ar;
 public void onCreate(Bundle state){super.onCreate(state);ar=getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
  LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);int p=(int)(16*getResources().getDisplayMetrics().density);body.setPadding(p,p,p,p);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(body);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
  TextView title=new TextView(this);title.setText(ar?"جلسات اللوج":"Log sessions");title.setTextSize(24);body.addView(title);
  TextView help=new TextView(this);help.setText(ar?"جلسة جديدة تلقائيًا عند تشغيل التطبيق من جديد أو اتصال شاشة السيارة، مع جمع إعادة الاتصال خلال 5 دقائق في نفس الجلسة. الرجوع من شاشة الكاميرا لا يبدأ جلسة جديدة.\n\nالاحتفاظ بآخر 40 جزءًا لمدة تصل إلى 7 أيام؛ يُحذف الأقدم تلقائيًا عند بلوغ أي حد. الجلسة الطويلة تُقسم إلى أجزاء 128 كيلوبايت. النسخ والتصدير للجزء المختار فقط، من غير اختصار. بدء جلسة جديدة لا يمسح القديمة. ملفات التصدير التي تحفظها خارج التطبيق تظل عندك.":"A new session starts when the app process starts or the car screen connects. Reconnections within 5 minutes stay in the same session. Returning from the camera does not start a new session.\n\nKeeps the latest 40 parts for up to 7 days; older parts are removed when either limit is reached. Long sessions continue in 128 KiB parts. Copy and Export include only the selected part, without truncation. Starting a session keeps earlier logs. Exports saved outside the app remain with you.");body.addView(help);
  sessions=new Spinner(this);sessions.setPrompt(ar?"اختيار الجلسة والجزء":"Choose session and part");body.addView(sessions);
  copy=addButton(body,ar?"نسخ الجزء المختار":"Copy selected part");export=addButton(body,ar?"تصدير الجزء المختار":"Export selected part");
  copy.setOnClickListener(v->output(true));export.setOnClickListener(v->output(false));
  addButton(body,ar?"بدء جلسة جديدة":"Start new session").setOnClickListener(v->{Diagnostics.startSession("Manual");refresh();});
  addButton(body,ar?"تحديث القائمة":"Refresh list").setOnClickListener(v->refresh());
  addButton(body,ar?"رجوع":"Back").setOnClickListener(v->finish());refresh();
 }
 private Button addButton(LinearLayout parent,String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);parent.addView(b);return b;}
 static String label(File file,boolean ar){
  String name=file.getName();String[] pieces=name.replace(".txt","").split("-");
  if(pieces.length!=6)return name;
  String date=pieces[0],time=pieces[1],kind=pieces[3],part=pieces[5].replace("part","").replaceFirst("^0+(?!$)","");
  if(date.length()!=8||time.length()!=6)return name;
  if(ar)kind=kind.equals("Car")?"سيارة":kind.equals("Manual")?"يدوية":kind.equals("Imported")?"لوج سابق":"تشغيل التطبيق";
  return kind+" · "+date.substring(6)+"/"+date.substring(4,6)+" "+time.substring(0,2)+":"+time.substring(2,4)+":"+time.substring(4)+" · "+(ar?"جزء ":"Part ")+part+" · "+((file.length()+1023)/1024)+" KiB";
 }
 private void refresh(){copy.setEnabled(false);export.setEnabled(false);Diagnostics.list(this,result->{files.clear();files.addAll(result);List<String> labels=new ArrayList<>();for(File file:files)labels.add(label(file,ar));ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sessions.setAdapter(adapter);copy.setEnabled(!files.isEmpty());export.setEnabled(!files.isEmpty());});}
 private void output(boolean clipboard){int index=sessions.getSelectedItemPosition();if(index>=0&&index<files.size())Diagnostics.output(this,files.get(index).getName(),clipboard);}
}
