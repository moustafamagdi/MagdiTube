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
                    .setTitle(ar ? "المساعدة في تحسين التطبيق" : "Help improve the app")
                    .setMessage(ar
                            ? "مشاركة تقارير تقنية مفعّلة لمساعدتنا في تحسين التطبيق وحل المشاكل. تشمل معلومات الجهاز ورقم الدعم، ولا تشمل كلمات المرور أو محتوى المشاهدة. يمكنك إيقافها الآن أو لاحقًا من الإعدادات."
                            : "Technical reports are enabled to help us fix problems and improve the app. They include device information and a support ID, but no passwords or watched content. You can turn sharing off now or anytime in Settings.")
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
