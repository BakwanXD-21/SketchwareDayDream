package extensions.anbui.daydream.git;

import android.util.Log;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import extensions.anbui.daydream.file.FilesTools;

public class GitFeaturesCore {
    public static final String TAG = "GitFeatureCore";
    public static String universalErrorContent = "";

    public static boolean cloneRepo(String localPath, String remoteUrl, String token, String branchName) {
        String branch = (branchName.isEmpty() ? "main" : branchName);

        try {
            CloneCommand cloneCommand = Git.cloneRepository()
                    .setURI(remoteUrl)
                    .setDirectory(new File(localPath))
                    .setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider(token, "")
                    );

            if (branch != null && !branch.trim().isEmpty()) {
                cloneCommand.setBranch(branch)
                        .setCloneAllBranches(false);
            } else {
                cloneCommand.setCloneAllBranches(false);
            }

            try (Git git = cloneCommand.call()) {
                System.out.println("Clone completed! Branch: " +
                        (branch == null || branch.isEmpty() ? "default" : branch));
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error clone: " + e.getMessage());
            universalErrorContent = e.getMessage();
            return false;
        }
    }

    public static boolean pushProject(String projectPath, String remoteUrl, String token, String title, String description) {

        String finalDescription = title;

        if (!description.isEmpty())
            finalDescription += "\n\n" + description;

        try {
            File repoDir = new File(projectPath);

            // Not have repo, new init
            Git git;
            if (new File(repoDir, ".git").exists()) {
                git = Git.open(repoDir);
            } else {
                git = Git.init().setDirectory(repoDir).call();
                git.remoteAdd()
                        .setName("origin")
                        .setUri(new URIish(remoteUrl))
                        .call();
            }

            // Add all file
            git.add().addFilepattern(".").call();

            // Commit
            git.commit()
                    .setMessage(finalDescription)
                    .call();

            // Push to GitHub
            git.push()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();

            Log.d(TAG, "Push completed!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error push: " + e.getMessage());
            universalErrorContent = e.getMessage();
            return false;
        }
    }

    public static boolean pullProject(String projectPath, String token) {
        try {
            Git git = Git.open(new File(projectPath));
            git.pull()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();
            Log.d(TAG, "Pull completed!");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error pull: " + e.getMessage());
            universalErrorContent = e.getMessage();
            return false;
        }
    }

    public static boolean switchBranch(String projectPath, String remoteUrl, String token, String branchName) {
        String branch = (branchName.isEmpty() ? "main" : branchName);

        try (Git git = Git.open(new File(projectPath))) {
            git.fetch()
                    .setRemote("origin")
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .call();

            git.checkout()
                    .setCreateBranch(true)
                    .setName(branch)
                    .setStartPoint("origin/" + branch)
                    .call();

            Log.d(TAG, "Switched to branch: " + branch);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Checkout failed, fallback to re-clone: " + e.getMessage());
            try {
                FilesTools.deleteFileOrDirectory(Path.of(projectPath));
                Git.cloneRepository()
                        .setURI(remoteUrl)
                        .setBranch(branch)
                        .setDirectory(new File(projectPath))
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                        .call();
                Log.d(TAG, "Re-cloned branch: " + branch);
                return true;
            } catch (Exception ex) {
                Log.e(TAG, "Re-clone failed: " + ex.getMessage());
                return false;
            }
        }
    }

    public static void getDiff(String projectPath) {
        try {
            Git git = Git.open(new File(projectPath));
            Repository repo = git.getRepository();

            ObjectId head = repo.resolve("HEAD^{tree}");
            ObjectId prevHead = repo.resolve("HEAD~1^{tree}");

            ObjectReader reader = repo.newObjectReader();
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, prevHead);

            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, head);

            List<DiffEntry> diffs = git.diff()
                    .setOldTree(oldTreeIter)
                    .setNewTree(newTreeIter)
                    .call();

            for (DiffEntry entry : diffs) {
                Log.d("GIT_DIFF", "File changed: " + entry.getNewPath());
            }
        } catch (Exception e) {
            Log.e("GIT_DIFF", "Error diff: " + e.getMessage());
        }
    }

    public static boolean hasChangesWithRemote(String projectPath, String remote, String token, String branchName) {
        String branch = (branchName.isEmpty() ? "main" : branchName);

        try {
            Git git = Git.open(new File(projectPath));
            Repository repository = git.getRepository();

            // 1. Check for uncommitted local changes
            Status status = git.status().call();
            if (!status.isClean()) {
                Log.d("GIT_CHECK", "There are local changes not yet committed");
                return true;
            }

            // 2. Get local commit ID
            ObjectId localHead = repository.resolve("refs/heads/" + branch);
            if (localHead == null) {
                Log.w("GIT_CHECK", "HEAD local not found");
                return true;
            }

            // 3. Parse repoUrl -> owner and repoName
            // Example: https://github.com/user/repo.git
            String cleanedUrl = remote.replace(".git", "").replace("git@", "https://");
            if (cleanedUrl.startsWith("git@")) {
                cleanedUrl = cleanedUrl.replace(":", "/");
            }
            // cleanedUrl will like: https://github.com/user/repo
            String[] parts = cleanedUrl.split("/");
            if (parts.length < 2) {
                Log.e("GIT_CHECK", "Invalid URL");
                return true;
            }
            String owner = parts[parts.length - 2];
            String repoName = parts[parts.length - 1];

            // 4. Call GitHub API to get remote commit ID
            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repoName + "/branches/" + branch;
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "token " + token);
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                Log.e("GIT_CHECK", "GitHub API Error: " + responseCode);
                return true; // If the API fails, it is considered a change.
            }

            // 5. Read the returned JSON
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 6. Parse JSON to get latest commit SHA
            JSONObject json = new JSONObject(response.toString());
            String remoteCommitId = json.getJSONObject("commit").getString("sha");

            // 7. Compare local and remote commit IDs
            boolean isChanged = !localHead.name().equals(remoteCommitId);
            Log.d("GIT_CHECK", "Local HEAD: " + localHead.name());
            Log.d("GIT_CHECK", "Remote HEAD: " + remoteCommitId);

            return isChanged;

        } catch (Exception e) {
            Log.e("GIT_CHECK", "Error: " + e.getMessage());
            return true; // Errors are considered to have changed.
        }
    }

}
