package extensions.anbui.daydream.git;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.file.FilesTools;
import extensions.anbui.daydream.project.ProjectDecryptor;
import extensions.anbui.daydream.tools.ToolCore;
import extensions.anbui.daydream.tools.project.ProjectFileManager;

public class GitApplyUtils {
    public static String TAG = Configs.universalTAG + "GitApplyUtils";

    public static boolean apply(String projectID, TextView statusTextView) {
        Log.i(TAG, "apply: " + projectID);
        try {
            cleanUpProject(projectID);
            ProjectFileManager.copyData(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/data",
                    FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID,
                    statusTextView,
                    true,
                    false);

            updateStatus(statusTextView, "Copying fonts...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/fonts",
                    FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + projectID,
                    null);

            updateStatus(statusTextView, "Copying icons...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/icons",
                    FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + projectID,
                    null);

            updateStatus(statusTextView, "Copying images...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/images",
                    FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + projectID,
                    null);

            updateStatus(statusTextView, "Copying sounds...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/sounds",
                    FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + projectID,
                    null);


            updateStatus(statusTextView, "Copying local libraries...");
            ProjectFileManager.copyLocalLibrary(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitLocalLibraryFolderName,
                    FileUtils.getInternalStorageDir() + Configs.projectLocalLibFolderDir,
                    true,
                    statusTextView);

            updateStatus(statusTextView, "Copying project info...");
            ProjectFileManager.copyProperties(projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/project",
                    FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID,
                    null);

            ToolCore.fixID(projectID);
            return true;
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
            return false;
        }
    }

    public static void cleanUpProject(String projectID) {
        Log.i(TAG, "cleanUpProject: " + projectID);
        try {
            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/files/"));

            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + projectID + "/"));
            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + projectID + "/"));
            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + projectID + "/"));
            FileUtils.deleteRecursive(new File(FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + projectID + "/"));
        } catch (Exception e) {
            Log.e(TAG, Objects.requireNonNull(e.getMessage()));
        }
    }

    private static void updateStatus(TextView statusTextView, String msg) {
        Log.i(TAG, "updateStatus: " + msg);
        if (statusTextView == null || statusTextView.getContext() == null) return;
        ((Activity) statusTextView.getContext()).runOnUiThread(() -> statusTextView.setText(msg));
    }
}
