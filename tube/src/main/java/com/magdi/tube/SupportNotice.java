package com.magdi.tube;

import android.app.Activity;
import android.app.AlertDialog;
import java.util.WeakHashMap;

/** Phone-only disclosure. No automatic upload until the user sees and closes it. */
final class SupportNotice {
    private static final WeakHashMap<Activity, Boolean> pending = new WeakHashMap<>();

    static void show(Activity activity, Runnable changed) {
        boolean ar = activity.getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ar");
        show(activity, ar, changed);
    }

    static void show(Activity activity, boolean ar, Runnable changed) {
        if (activity.isFinishing() || activity.isDestroyed() || pending.containsKey(activity)) return;
        pending.put(activity, true);
        SupportReporter.info(info -> {
            if (activity.isFinishing() || activity.isDestroyed() || !activity.hasWindowFocus()
                    || !info.optBoolean("notice_pending")) {
                pending.remove(activity);
                return;
            }
            AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle(ar ? "تقارير التشخيص مفعّلة" : "Diagnostic reports are enabled")
                    .setMessage(ar
                            ? "المشاركة التلقائية مفعّلة افتراضيًا للمساعدة في إصلاح المشاكل وتحسين التطبيق فقط.\n\nنرسل أحداثًا تقنية وتوقيتاتها وموديل الجهاز وإصدارات البرامج ورقم دعم عشوائي يربط تقارير تطبيقات MM على هذا الموبايل. لا نرسل كلمات المرور أو بيانات الحساب أو محتوى المشاهدة أو البحث أو روابط البث.\n\nتصل التقارير إلى قاعدة بيانات المطور على Supabase في فرانكفورت، وتُحفظ حتى ٣٠ يومًا. الخدمة تستقبل عنوان IP للاتصال؛ لا نخزّنه في تقارير التشخيص.\n\nالإرسال بحد أقصى محاولة كل ساعتين لكل تطبيق. يمكنك إيقافه الآن أو من «الدعم والتشخيص» في أي وقت، وحذف التقارير المرسلة من الخيارات المتقدمة. كل تطبيق يحتفظ باختياره."
                            : "Automatic sharing is on by default to diagnose problems and improve the app only.\n\nWe send technical events and their times, device model, software versions and a random support ID linking reports from your MM apps on this phone. We do not send passwords, account details, watched content, searches or stream URLs.\n\nReports go to the developer’s Supabase database in Frankfurt and are kept for up to 30 days. The service receives your IP for the connection; we do not store it in diagnostic reports.\n\nAt most one automatic attempt every 2 hours per app. Turn it off now or anytime in Support & diagnostics. Delete previously sent reports under Advanced options. Each app keeps its own setting.")
                    .setPositiveButton(ar ? "متابعة" : "Continue", (d, w) -> choose(activity, true, changed))
                    .setNegativeButton(ar ? "إيقاف المشاركة" : "Turn off sharing", (d, w) -> choose(activity, false, changed))
                    .setCancelable(false).create();
            dialog.setOnDismissListener(d -> pending.remove(activity));
            dialog.show();
        });
    }

    private static void choose(Activity activity, boolean enabled, Runnable changed) {
        SupportReporter.changeConsent(enabled, result -> {
            if (!activity.isFinishing() && !activity.isDestroyed() && changed != null) changed.run();
        });
    }

    private SupportNotice() {}
}
