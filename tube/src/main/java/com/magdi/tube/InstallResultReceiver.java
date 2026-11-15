package com.magdi.tube;
public final class InstallResultReceiver extends android.content.BroadcastReceiver {
 public void onReceive(android.content.Context c,android.content.Intent intent){if(android.content.Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction()))InstallSetup.observe(c,"package replaced");}
}
