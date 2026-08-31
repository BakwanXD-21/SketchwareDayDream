package extensions.anbui.daydream.git;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Path;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.file.FilesTools;

public class DayDreamGitConfigs {
    public static String TAG = Configs.universalTAG + "DayDreamProjectSettings";

    public static String getGitHubToken(String projectID) {
        Log.i(TAG, "getGitHubToken: " + projectID);
        return getDataString(projectID, "git", "token");
    }

    public static void setGitHubToken(String projectID, String token) {
        Log.i(TAG, "setGitHubToken: " + projectID);
        setDataString(projectID, "git", "token", token);
    }

    public static String getRemoteUrl(String projectID) {
        Log.i(TAG, "getRemoteUrl: " + projectID);
        return getDataString(projectID, "git", "remoteUrl");
    }

    public static void setRemoteUrl(String projectID, String remoteUrl) {
        Log.i(TAG, "setRemoteUrl: " + projectID);
        setDataString(projectID, "git", "remoteUrl", remoteUrl);
    }

    public static String getBranch(String projectID) {
        Log.i(TAG, "getBranch: " + projectID);
        return getDataString(projectID, "git", "branch");
    }

    public static void setBranch(String projectID, String branch) {
        Log.i(TAG, "setBranch: " + projectID);
        setDataString(projectID, "git", "branch", branch);
    }

    //Read and write data

    public static String getDataString(String projectID, String toplevelkey, String key) {
        Log.i(TAG, "getDataString: " + projectID + " " + toplevelkey + " " + key);
        JsonObject json = JsonParser.parseString(readDayDreamDataFile(projectID)).getAsJsonObject();
        if (json.has(toplevelkey)) {
            JsonObject edge = json.getAsJsonObject(toplevelkey);
            try {
                Log.i(TAG, "getDataString: " + edge.get(key).getAsString());
                return edge.get(key).getAsString();
            } catch (Exception e) {
                Log.e(TAG, "getDataString: " + e.getMessage());
            }
        }
        return "";
    }

    public static void setDataString(String projectID, String toplevelkey, String key, String value) {
        Log.i(TAG, "setDataString: " + projectID + " " + toplevelkey + " " + key + " " + value);
        JsonObject json = JsonParser.parseString(readDayDreamDataFile(projectID)).getAsJsonObject();
        if (!json.has(toplevelkey)) {
            JsonObject edge = new JsonObject();
            edge.addProperty(key, value);
            json.add(toplevelkey, edge);
        } else {
            JsonObject edge = json.getAsJsonObject(toplevelkey);
            edge.addProperty(key, value);
        }
        writeDayDreamDataFile(projectID, new Gson().toJson(json));
    }

    public static String readDayDreamDataFile(String projectID) {
        String contentProjectFile = FileUtils.readTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/DataDayDreamGit.json");
        if (contentProjectFile.isEmpty()) contentProjectFile = "{}";
        Log.i(TAG, "readDayDreamDataFile: " + projectID + " " + contentProjectFile);
        return contentProjectFile;
    }

    public static void writeDayDreamDataFile(String projectID, String content) {
        Log.i(TAG, "writeDayDreamDataFile: " + projectID + " " + content);
        FileUtils.writeTextFile(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/DataDayDreamGit.json", content);
    }

    public static void deleteConfigs(String projectID) {
        Log.i(TAG, "deleteConfigs: " + projectID);
        try {
        FilesTools.deleteFileOrDirectory(Path.of(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/DataDayDreamGit.json"));
        } catch (Exception e) {
            Log.e(TAG, "deleteConfigs: " + e.getMessage());
        }
    }

    public static boolean isSettedUp(String projectID) {
        Log.i(TAG, "isSettedUp: " + projectID);
        return FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.projectDataFolderDir + projectID + "/DataDayDreamGit.json");
    }
}
