package extensions.anbui.daydream.git

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.TextView
import com.android.tools.r8.internal.re
import extensions.anbui.daydream.configs.Configs
import extensions.anbui.daydream.file.FileUtils
import extensions.anbui.daydream.tools.ToolCore
import extensions.anbui.daydream.tools.project.ProjectFileManager.copyData
import extensions.anbui.daydream.tools.project.ProjectFileManager.copyLocalLibrary
import extensions.anbui.daydream.tools.project.ProjectFileManager.copyProperties
import extensions.anbui.daydream.tools.project.ProjectFileManager.copyResources
import extensions.anbui.daydream.tools.project.RemoveCore
import extensions.anbui.daydream.ui.DialogUtils
import pro.sketchware.R

object GitQuickLook {
    @JvmStatic
    var TAG: String = Configs.universalTAG + "GitQuickLook"

    @JvmStatic
    fun cleanUp(context : Context?) {
        if (FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + Configs.defaultQuickLookProjectID)) {
            Log.i(TAG, "cleanup: Starting...")
            RemoveCore.startNow(context as Activity, Configs.defaultQuickLookProjectID)
            Log.i(TAG, "cleanup: Done...")
        }
    }

    @JvmStatic
    fun isQuickLook(context : Context?, projectID : String, isShowWarningDialog : Boolean) : Boolean {
        if (projectID == Configs.defaultQuickLookProjectID) {
            if (context != null && isShowWarningDialog) {
                DialogUtils.oneDialog(
                    context as Activity?,
                    "View-only mode",
                    "All data will not be saved after exit.",
                    "OK",
                    true,
                    R.drawable.visibility_24px,
                    true, null, null
                )
            }
            return true
        }
        return false
    }

    @JvmStatic
    fun startView(projectID: String, statusTextView: TextView?): Boolean {
        Log.i(TAG, "startView: $projectID")
        try {
            cleanUp(null)
            copyData(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/data",
                FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + Configs.defaultQuickLookProjectID,
                statusTextView,
                customBlocks = true,
                includeGit = false
            )

            updateStatus(statusTextView, "Copying fonts...")
            copyResources(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/fonts",
                FileUtils.getInternalStorageDir() + Configs.resFontsFolderDir + Configs.defaultQuickLookProjectID,
                null
            )

            updateStatus(statusTextView, "Copying icons...")
            copyResources(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/icons",
                FileUtils.getInternalStorageDir() + Configs.resIconsFolderDir + Configs.defaultQuickLookProjectID,
                null
            )

            updateStatus(statusTextView, "Copying images...")
            copyResources(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/images",
                FileUtils.getInternalStorageDir() + Configs.resImagesFolderDir + Configs.defaultQuickLookProjectID,
                null
            )

            updateStatus(statusTextView, "Copying sounds...")
            copyResources(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/resources/sounds",
                FileUtils.getInternalStorageDir() + Configs.resSoundsFolderDir + Configs.defaultQuickLookProjectID,
                null
            )


            updateStatus(statusTextView, "Copying local libraries...")
            copyLocalLibrary(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitLocalLibraryFolderName,
                FileUtils.getInternalStorageDir() + Configs.projectLocalLibFolderDir,
                true,
                statusTextView
            )

            updateStatus(statusTextView, "Copying project info...")
            copyProperties(
                projectID,
                FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitProjectFolderName + "/project",
                FileUtils.getInternalStorageDir() + Configs.projectInfoFolderDir + Configs.defaultQuickLookProjectID,
                null
            )

            ToolCore.fixID(projectID)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "startView: ", e)
            return false
        }
    }

    @JvmStatic
    private fun updateStatus(statusTextView: TextView?, msg: String?) {
        Log.i(TAG, "updateStatus: $msg")
        if (statusTextView == null || statusTextView.context == null) return
        (statusTextView.context as Activity).runOnUiThread {
            statusTextView.text = msg
        }
    }
}