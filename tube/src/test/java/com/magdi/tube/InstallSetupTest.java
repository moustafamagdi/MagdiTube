package com.magdi.tube;

import android.content.*;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=32)
public class InstallSetupTest {
 @Test public void setupScreenOpensWithoutStartingInstallation(){
  org.robolectric.android.controller.ActivityController<InstallSetupActivity> controller=org.robolectric.Robolectric.buildActivity(InstallSetupActivity.class).setup();
  InstallSetupActivity screen=controller.get();
  assertFalse(screen.getSharedPreferences(InstallSetup.PREFS,0).getBoolean("pending",false));
  assertNull(org.robolectric.Shadows.shadowOf(screen).getNextStartedActivity());
  assertTrue(InstallSetup.report(screen).contains("NOT_ATTEMPTED"));
  controller.pause().stop().destroy();
 }
 private InstallSetup.Snapshot snapshot(String installer,String initiator,long update){return new InstallSetup.Snapshot(installer,initiator,update,null);}
 @Test public void playInstallerAloneDoesNotPass(){
  assertFalse(snapshot("com.android.vending","com.android.shell",2).matches());
  assertFalse(snapshot("com.android.vending","evil.packageinstaller",2).matches());
  assertFalse(snapshot("com.android.vending",null,2).matches());
  assertTrue(snapshot("com.android.vending","com.google.android.packageinstaller",2).matches());
 }
 @Test public void existingMatchDoesNotClaimNewSuccess(){
  InstallSetup.Snapshot matched=snapshot("com.android.vending","com.android.packageinstaller",2);
  assertEquals("NO_REINSTALL_OBSERVED",InstallSetup.outcome(matched,matched,false));
  assertEquals("REINSTALL_OBSERVED_ALREADY_MATCHED_BEFORE",InstallSetup.outcome(matched,matched,true));
 }
 @Test public void distinguishesSuccessfulMetadataChangeFromIgnoredFlagsAndUnavailableData(){
  InstallSetup.Snapshot original=snapshot("com.google.android.packageinstaller","com.google.android.packageinstaller",1);
  assertEquals("REINSTALL_OBSERVED_METADATA_NOW_MATCH",InstallSetup.outcome(original,snapshot("com.android.vending","com.google.android.packageinstaller",2),true));
  assertEquals("REINSTALL_OBSERVED_METADATA_MISMATCH",InstallSetup.outcome(original,original,true));
  assertEquals("UNVERIFIED_METADATA_UNAVAILABLE",InstallSetup.outcome(original,new InstallSetup.Snapshot(null,null,0,"SecurityException"),true));
 }
 @Test public void selfInstallUsesReadableContentUriAndSystemConfirmation(){
  Uri uri=Uri.parse("content://com.magdi.tube.reports/installer/Magdi-Tube.apk");Intent intent=InstallSetup.intent(uri);
  assertEquals(Intent.ACTION_INSTALL_PACKAGE,intent.getAction());assertEquals(uri,intent.getData());
  assertEquals(uri,intent.getClipData().getItemAt(0).getUri());
  assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
  assertEquals(0,intent.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
  assertTrue(intent.getBooleanExtra(Intent.EXTRA_RETURN_RESULT,false));
  assertEquals("com.android.vending",intent.getStringExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME));
 }
}
