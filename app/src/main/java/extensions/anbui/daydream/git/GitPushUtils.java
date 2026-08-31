package extensions.anbui.daydream.git;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.widget.TextView;

import com.besome.sketch.export.ExportSource;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.file.FilesTools;
import extensions.anbui.daydream.project.ProjectDecryptor;
import extensions.anbui.daydream.secret.SecretUtils;
import extensions.anbui.daydream.tools.project.ProjectFileManager;

public class GitPushUtils {
    public static final String TAG = "GitPushUtils";

    public static void quickPrepareFiles(Activity activity, String projectID) {
        Log.i(TAG, "quickPrepareFiles: " + projectID);

        preparefiles(activity, projectID, null, false, false, false, true, true, false, false);
    }

    public static boolean preparefiles(Activity activity, String projectID, TextView statusTextView, boolean locallibs, boolean customblocks, boolean includeApis, boolean pushProject, boolean pushSource, boolean keepFiles, boolean gradlefiles) {
        Log.i(TAG, "preparefiles: " + projectID);

        boolean result = FileUtils.createDirectory(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName);

        if (!result) return false;

        if (!keepFiles)
            cleanUpGitFolder(projectID, statusTextView, pushProject, pushSource);

        if (pushProject) {
            ProjectFileManager.copyData(projectID,
                    FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/data",
                    statusTextView,
                    customblocks,
                    false);

            updateStatus(statusTextView, "Copying fonts...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/fonts",
                    null);

            updateStatus(statusTextView, "Copying icons...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/icons",
                    null);

            updateStatus(statusTextView, "Copying images...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/images",
                    null);

            updateStatus(statusTextView, "Copying sounds...");
            ProjectFileManager.copyResources(projectID,
                    FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/sounds",
                    null);

            if (locallibs) {
                updateStatus(statusTextView, "Copying local libraries...");
                ProjectFileManager.copyLocalLibrary(projectID,
                        FileUtils.getInternalStorageDir() + Configs.projectLocalLibFolderDir,
                        FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitLocalLibraryFolderName,
                        false,
                        statusTextView);
            }

            updateStatus(statusTextView, "Copying project info...");
            ProjectFileManager.copyProperties(projectID,
                    FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + projectID + "/project",
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName,
                    null);

            if (!includeApis)
                ProjectDecryptor.saveEncryptedFile(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/data/library", SecretUtils.hideLibrary(projectID));
        }

        if (pushSource) {
            updateStatus(statusTextView, "Exporting source...");
            ProjectFileManager.copySourceCode(activity,
                    projectID,
                    FileUtils.getInternalStorageDir() + Configs.projectMySourceFolderDir + projectID,
                    FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitSourceFolderName,
                    gradlefiles,
                    statusTextView);

            if (!includeApis && FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitSourceFolderName + "/app/src/main/res/values/secrets.xml"))
                FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitSourceFolderName + "/app/src/main/res/values/secrets.xml", SecretUtils.hideXML(projectID));
        }

        updateStatus(statusTextView, "File prepared...");
        return true;
    }

    public static void cleanUpGitFolder(String projectID, TextView statusTextView, boolean pushProject, boolean pushSource) {
        Log.i(TAG, "cleanUpGitFolder: " + projectID);

        if (!FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName))
            return;

        try {
            if (!pushProject) {
                updateStatus(statusTextView, "Cleaning up project...");
                FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/"));
            }

            if (!pushSource) {
                updateStatus(statusTextView, "Cleaning up source...");
                FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitSourceFolderName + "/"));
            }
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
