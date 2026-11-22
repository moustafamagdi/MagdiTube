package com.magdi.tube;

import android.app.AlertDialog;
import android.app.Application;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.widget.TextView;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;

@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 35, application = Application.class)
public class SupportDefaultsTest {
    Context context;
    SharedPreferences prefs;
    AtomicInteger sent = new AtomicInteger();

    @Before public void prepare() throws Exception {
        SupportTestState.reset();
        context = RuntimeEnvironment.getApplication();
        prefs = context.getSharedPreferences("mm_support_v1", 0);
        prefs.edit().clear().commit();
        SupportReporter.delivery = body -> { sent.incrementAndGet(); return 200; };
    }

    @org.junit.After public void cleanup() throws Exception { SupportTestState.reset(); }

    private void drain() throws Exception {
        for(int i=0;i<3;i++){SupportReporter.io.submit(() -> {}).get(10, TimeUnit.SECONDS);
        Shadows.shadowOf(Looper.getMainLooper()).idle();}
    }

    private void init() throws Exception { SupportReporter.init(context); drain(); }

    private ActivityController<SupportActivity> screen(boolean ar) throws Exception {
        ActivityController<SupportActivity> activity = Robolectric.buildActivity(SupportActivity.class,
                new Intent(context, SupportActivity.class).putExtra("language", ar ? "ar" : "en"))
                .create().start().resume().visible().windowFocusChanged(true);
        drain(); drain();
        return activity;
    }

    @Test public void freshDefaultIsOnButNeitherSchedulesNorUploadsBeforeNotice() throws Exception {
        init();
        assertTrue(prefs.getBoolean("enabled", false));
        assertTrue(SupportPreferences.noticePending(prefs));
        assertNull(context.getSystemService(JobScheduler.class).getPendingJob(SupportReporter.JOB));
        SupportReporter.record("Process started; version=test", System.currentTimeMillis());
        SupportReporter.automatic(() -> {}); drain();
        assertEquals(0, sent.get());
    }

    @Test public void savedOffSurvivesEveryLegacyPolicyIncludingEarlierDefault() {
        for (int version : new int[]{0, 1, 2}) {
            prefs.edit().clear().putBoolean("enabled", false).putInt("consentVersion", version)
                    .putString("id", "retained-id").putString("token", "retained-secret").commit();
            assertFalse(SupportPreferences.initialize(prefs));
            assertFalse(SupportPreferences.noticePending(prefs));
            assertFalse(SupportPreferences.canSendAutomatically(prefs));
            assertEquals("retained-id", prefs.getString("id", ""));
            assertEquals("retained-secret", prefs.getString("token", ""));
        }
    }

    @Test public void existingOnIsPreservedAndNewDisclosurePrecedesUploads() {
        prefs.edit().putBoolean("enabled", true).putInt("consentVersion", 2).commit();
        assertTrue(SupportPreferences.initialize(prefs));
        assertTrue(SupportPreferences.noticePending(prefs));
        assertFalse(SupportPreferences.canSendAutomatically(prefs));
    }

    @Test public void englishNoticeContinueKeepsDefaultAndTwoHourLimit() throws Exception {
        init(); String id = prefs.getString("id", ""), token = prefs.getString("token", "");
        ActivityController<SupportActivity> activity = screen(false);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertTrue(dialog.isShowing());
        assertEquals("Turn off sharing", dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
        String notice = ((TextView) dialog.findViewById(android.R.id.message)).getText().toString();
        assertTrue(notice.contains("support ID"));
        assertTrue(notice.contains("passwords"));
        assertTrue(notice.contains("Settings"));
        assertFalse(notice.contains("Supabase"));
        assertFalse(notice.contains("Frankfurt"));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); drain();
        assertTrue(SupportPreferences.canSendAutomatically(prefs));
        assertNotNull(context.getSystemService(JobScheduler.class).getPendingJob(SupportReporter.JOB));
        assertEquals(id, prefs.getString("id", "")); assertEquals(token, prefs.getString("token", ""));
        SupportReporter.record("Process started; version=test", System.currentTimeMillis() + 1);
        SupportReporter.automatic(() -> {}); drain();
        assertEquals(1, sent.get());
        SupportReporter.record("Process started; version=test", System.currentTimeMillis() + 2);
        SupportReporter.automatic(() -> {}); drain(); assertEquals(1, sent.get());
        activity.pause().stop().destroy();
        ActivityController<SupportActivity> reopened = screen(false);
        assertFalse(SupportPreferences.noticePending(prefs));
        assertTrue(reopened.get().automatic.isChecked());
        assertFalse(dialog.isShowing()); reopened.pause().stop().destroy();
    }

    @Test public void arabicNoticeAllowsImmediatePersistentOptOut() throws Exception {
        init(); ActivityController<SupportActivity> activity = screen(true);
        AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals("إيقاف المشاركة", dialog.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
        String notice = ((TextView) dialog.findViewById(android.R.id.message)).getText().toString();
        assertTrue(notice.contains("رقم الدعم"));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick(); drain();
        assertFalse(activity.get().automatic.isChecked());
        assertFalse(SupportPreferences.initialize(prefs));
        assertNull(context.getSystemService(JobScheduler.class).getPendingJob(SupportReporter.JOB));
        SupportReporter.record("Process started; version=test", System.currentTimeMillis() + 1);
        SupportReporter.automatic(() -> {}); drain(); assertEquals(0, sent.get());
        activity.pause().stop().destroy();
    }

    @Test public void dismissingPhoneWithoutChoosingDoesNotAuthorizeSending() throws Exception {
        init(); ActivityController<SupportActivity> activity = screen(false);
        activity.pause().stop().destroy();
        SupportReporter.automatic(() -> {}); drain();
        assertEquals(0, sent.get()); assertTrue(SupportPreferences.noticePending(prefs));
    }

    @Test public void failedDeletionStillKeepsAutomaticSharingOff() throws Exception {
        init(); SupportReporter.changeConsent(true, null); drain();
        SupportReporter.delivery = body -> 503;
        SupportReporter.erase(s -> {}); drain();
        assertFalse(SupportPreferences.initialize(prefs));
        assertFalse(SupportPreferences.noticePending(prefs));
    }
}
