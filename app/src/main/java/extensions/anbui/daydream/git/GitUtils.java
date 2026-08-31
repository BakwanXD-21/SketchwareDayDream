package extensions.anbui.daydream.git;

import android.app.Activity;
import android.util.Log;
import android.widget.TextView;

import java.nio.file.Path;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.file.FilesTools;

public class GitUtils {

    public static final String TAG = "GitUtils";

    public static boolean clone(String projectID) {

        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID));
        } catch (Exception e) {
            Log.e(TAG, "clone: " + e.getMessage());
        }

        if (!FileUtils.createDirectory(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID))
            return false;

        return GitFeaturesCore.cloneRepo(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID,
                DayDreamGitConfigs.getRemoteUrl(projectID),
                DayDreamGitConfigs.getGitHubToken(projectID),
                DayDreamGitConfigs.getBranch(projectID));
    }

    public static boolean startClone(String projectID, String remoteURL, String token, String branch) {

        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID));
        } catch (Exception e) {
            Log.e(TAG, "clone: " + e.getMessage());
        }

        if (!FileUtils.createDirectory(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID))
            return false;

        return GitFeaturesCore.cloneRepo(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID,
                remoteURL,
                token,
                branch);
    }

    public static boolean push(Activity activity, String projectID, TextView statusTextView, String title, String description, boolean locallibs, boolean customblocks, boolean includeApis, boolean pushProject, boolean pushSource, boolean keepFiles, boolean gradlefiles) {
        if (!GitPushUtils.preparefiles(activity, projectID, statusTextView, locallibs, customblocks, includeApis, pushProject, pushSource, keepFiles, gradlefiles))
            return false;

        updateStatus(statusTextView, "Pushing project...");

        return GitFeaturesCore.pushProject(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID,
                DayDreamGitConfigs.getRemoteUrl(projectID),
                DayDreamGitConfigs.getGitHubToken(projectID), title, description);
    }

    public static boolean pull(String projectID) {
        return GitFeaturesCore.pullProject(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID,
                DayDreamGitConfigs.getGitHubToken(projectID));
    }

    public static boolean startSwitch(String projectID, String remoteURL, String token, String branch) {

        return GitFeaturesCore.switchBranch(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID,
                remoteURL,
                token,
                branch);
    }

    public static boolean quickGetDiff(String projectID) {
        return GitFeaturesCore.hasChangesWithRemote(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID,
                DayDreamGitConfigs.getRemoteUrl(projectID),
                DayDreamGitConfigs.getGitHubToken(projectID),
                DayDreamGitConfigs.getBranch(projectID));
    }

    private static void updateStatus(TextView statusTextView, String msg) {
        Log.i(TAG, "updateStatus: " + msg);
        if (statusTextView == null || statusTextView.getContext() == null) return;
        ((Activity) statusTextView.getContext()).runOnUiThread(() -> statusTextView.setText(msg));
    }

    public static void remove(String projectID) {
        try {
            FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID));
        } catch (Exception e) {
            Log.e(TAG, "cleanUp: " + e.getMessage());
        }
        DayDreamGitConfigs.deleteConfigs(projectID);
    }
}
