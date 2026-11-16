package com.magdi.tube;

import android.app.Activity;
import android.net.Uri;
import android.view.ContextThemeWrapper;
import android.webkit.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class) @Config(sdk=32)
public class GoogleAccountNavigationTest {
 @Test public void acceptsRegionalAccountsIncludingReportedQatarAndExistingSaudi(){
  for(String host:new String[]{"accounts.google.com.qa","accounts.google.com.sa","accounts.google.com.eg","accounts.google.ae","accounts.google.co.uk","accounts.google.co.in","accounts.google.de","accounts.google.com","myaccount.google.com"})
   assertTrue(host,GoogleAccountNavigation.allowed(Uri.parse("https://"+host+"/signin?continue=https%3A%2F%2Fm.youtube.com")));
  assertTrue(GoogleAccountNavigation.allowed(Uri.parse("https://ACCOUNTS.GOOGLE.COM.QA:443/")));
 }
 @Test public void rejectsLookalikesEmbeddedTextAndUnsafeAuthorities(){
  for(String url:new String[]{"https://accounts.google.com.evil.test/","https://evilaccounts.google.com.qa/","https://accounts.google.com.qa.evil.test/","https://evil.test/accounts.google.com","https://evil.test/?next=accounts.google.com.qa","https://accounts.google.com@evil.test/","https://evil@accounts.google.com.qa/","http://accounts.google.com.qa/","https://accounts.google.com.qa:444/","https://accounts.google.com.invalid/","https://accounts.google.com.zz/","https://accounts.google.com.qa%2eevil.test/","https://accounts.google.com.qa\\@evil.test/","javascript:accounts.google.com","https://accounts.google.com.qa./","https://google.com.qa/"})
   assertFalse(url,GoogleAccountNavigation.allowed(Uri.parse(url)));
  assertFalse(GoogleAccountNavigation.allowed(null));
 }
 @Test public void regionalLoginAllowedOnPhoneButBlockedOnCar()throws Exception{
  Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
  TubeView phone=new TubeView(activity),car=new TubeView(new ContextThemeWrapper(activity,android.R.style.Theme_Material_NoActionBar));
  try{
   assertFalse(navigation(phone,"https://accounts.google.com.qa/signin"));
   assertTrue(navigation(car,"https://accounts.google.com.qa/signin"));
   assertTrue(navigation(phone,"https://accounts.google.com.evil.test/signin"));
   assertFalse(navigation(car,"https://m.youtube.com/watch?v=abcdefghijk"));
  }finally{phone.close();car.close();activity.finish();}
 }
 private boolean navigation(TubeView view,String url)throws Exception{
  java.lang.reflect.Field field=TubeView.class.getDeclaredField("web");field.setAccessible(true);WebView web=(WebView)field.get(view);
  WebResourceRequest request=new WebResourceRequest(){
   public Uri getUrl(){return Uri.parse(url);}public boolean isForMainFrame(){return true;}public boolean isRedirect(){return true;}public boolean hasGesture(){return false;}public String getMethod(){return "GET";}public java.util.Map<String,String> getRequestHeaders(){return java.util.Collections.emptyMap();}
  };
  return shadowOf(web).getWebViewClient().shouldOverrideUrlLoading(web,request);
 }
}
