package extensions.anbui.daydream.git;

import android.app.Activity;

import extensions.anbui.daydream.ui.DialogUtils;
import pro.sketchware.R;

public class DayDreamGitTools {
    public static void checkDiff(Activity activity, String projectID, Runnable runnable) {

        if(DayDreamGitConfigs.getGitHubToken(projectID).isEmpty() || DayDreamGitConfigs.getRemoteUrl(projectID).isEmpty())
            return;

        new Thread(() -> {
            boolean isDiff = GitUtils.quickGetDiff(projectID);

            activity.runOnUiThread(() -> {
                if (isDiff) {
                    DialogUtils.twoDialog(activity,
                            "Need pull",
                            "The files on GitHub have been changed compared to the repository cloned on your device, you should pull and apply to avoid conflicts. If you are working in a team and to ensure that the encrypted data is not corrupted, each change and push should be done by only one member and the rest of the members will pull.",
                            "Close project and execute",
                            "Cancel",
                            true,
                            R.drawable.ic_mtrl_warning,
                            true,
                            runnable,
                            null,
                            null);
                }
            });
        }).start();
    }
}
