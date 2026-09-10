package pro.sketchware.activities.main.activities;

import android.Manifest;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.besome.sketch.lib.base.BasePermissionAppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import a.a.a.DB;
import a.a.a.GB;
import dev.chrisbanes.insetter.Insetter;
import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.file.FilesTools;
import extensions.anbui.daydream.git.GitQuickLook;
import extensions.anbui.daydream.setup.DRSetup;
import mod.hey.studios.project.backup.BackupFactory;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.hey.studios.util.Helper;
import mod.hilal.saif.activities.tools.ConfigActivity;
import mod.tyron.backup.SingleCopyTask;
import pro.sketchware.R;
import pro.sketchware.activities.about.AboutActivity;
import pro.sketchware.activities.main.fragments.projects.ProjectsFragment;
import pro.sketchware.activities.main.fragments.projects_store.ProjectsStoreFragment;
import pro.sketchware.databinding.MainBinding;
import pro.sketchware.lib.base.BottomSheetDialogView;
import pro.sketchware.utility.DataResetter;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

//DR
public class MainActivity extends BasePermissionAppCompatActivity {
    private static final String PROJECTS_FRAGMENT_TAG = "projects_fragment";
    private static final String PROJECTS_STORE_FRAGMENT_TAG = "projects_store_fragment";
    private ActionBarDrawerToggle drawerToggle;
    private DB u;
    private Snackbar storageAccessDenied;
    private MainBinding binding;
    private final OnBackPressedCallback closeDrawer = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            setEnabled(false);
            binding.drawerLayout.closeDrawers();
        }
    };
    private ProjectsFragment projectsFragment;
    private ProjectsStoreFragment projectsStoreFragment;
    private Fragment activeFragment;
    private BackupRestoreManager backupRestoreManager;
    public static boolean needRefreshProjectList = false;

    // FAB expand/collapse state
    private boolean isFabExpanded = false;

    @IdRes
    private int currentNavItemId = R.id.item_projects;

    @Override
    public void g(int i) {
        if (i == 9501) {
            allFilesAccessCheck();
            if (activeFragment instanceof ProjectsFragment) {
                projectsFragment.refreshProjectsList();
            }
        }
    }

    @Override
    public void h(int i) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
        startActivityForResult(intent, i);
    }

    @Override
    public void l() {
    }

    @Override
    public void m() {
    }

    public void n() {
        if (activeFragment instanceof ProjectsFragment) {
            projectsFragment.refreshProjectsList();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 105:
                    DataResetter.a(this, data.getBooleanExtra("onlyConfig", true));
                    break;
                case 111:
                    invalidateOptionsMenu();
                    break;
                case 113:
                    if (data != null && data.getBooleanExtra("not_show_popup_anymore", false)) {
                        u.a("U1I2", (Object) false);
                    }
                    break;
                case 212:
                    if (!(data.getStringExtra("save_as_new_id") == null ? "" : data.getStringExtra("save_as_new_id")).isEmpty()
                            && isStoragePermissionGranted()) {
                        if (activeFragment instanceof ProjectsFragment) {
                            projectsFragment.refreshProjectsList();
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeNoContrast();

        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar hanya untuk ActionBarDrawerToggle, judul disembunyikan
        setSupportActionBar(binding.toolbar);
        binding.statusBarOverlapper.setMinimumHeight(UI.getStatusBarHeight(this));
        UI.addSystemWindowInsetToPadding(binding.appbar, true, false, true, false);

        u = new DB(getApplicationContext(), "U1");
        int u1I0 = u.a("U1I0", -1);
        long u1I1 = u.e("U1I1");
        if (u1I1 <= 0) {
            u.a("U1I1", System.currentTimeMillis());
        }
        if (System.currentTimeMillis() - u1I1 > 1000 * 60 * 60 * 24) {
            u.a("U1I0", Integer.valueOf(u1I0 + 1));
        }

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);

        drawerToggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, R.string.app_name, R.string.app_name);
        binding.drawerLayout.addDrawerListener(drawerToggle);
        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                closeDrawer.setEnabled(true);
                getOnBackPressedDispatcher().addCallback(closeDrawer);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        // Hilangkan underline SearchView
        View searchPlate = binding.searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) {
            searchPlate.setBackgroundColor(Color.TRANSPARENT);
        }
        
        // Tambahkan di dalam onCreate() MainActivity.java di dekat inisialisasi searchView:
        ImageButton btnCloseApp = binding.getRoot().findViewById(R.id.btn_close_app);
        if (btnCloseApp != null) {
           btnCloseApp.setOnClickListener(v -> finish());
        }

        // Hubungkan SearchView ke ProjectsFragment
        binding.searchView.setOnQueryTextListener(
                new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextChange(String s) {
                        if (projectsFragment != null) {
                            projectsFragment.filterFromSearch(s);
                        }
                        return true;
                    }

                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        return false;
                    }
                });

        boolean hasStorageAccess = isStoragePermissionGranted();
        if (!hasStorageAccess) {
            showNoticeNeedStorageAccess();
        }
        if (hasStorageAccess) {
            allFilesAccessCheck();
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            if (data != null) {
                new SingleCopyTask(this, new SingleCopyTask.CallBackTask() {
                    @Override
                    public void onCopyPreExecute() {
                    }

                    @Override
                    public void onCopyProgressUpdate(int progress) {
                    }

                    @Override
                    public void onCopyPostExecute(@NonNull String path, boolean wasSuccessful,
                            @NonNull String reason) {
                        if (wasSuccessful) {
                            BackupRestoreManager manager =
                                    new BackupRestoreManager(MainActivity.this, projectsFragment);
                            if (BackupFactory.zipContainsFile(path, "local_libs")) {
                                new MaterialAlertDialogBuilder(MainActivity.this)
                                        .setTitle("Warning")
                                        .setMessage(BackupRestoreManager
                                                .getRestoreIntegratedLocalLibrariesMessage(
                                                        false, -1, -1, null))
                                        .setPositiveButton("Copy",
                                                (dialog, which) -> manager.doRestore(path, true))
                                        .setNegativeButton("Don't copy",
                                                (dialog, which) -> manager.doRestore(path, false))
                                        .setNeutralButton(R.string.common_word_cancel, null)
                                        .show();
                            } else {
                                manager.doRestore(path, true);
                            }
                            getIntent().setData(null);
                        } else {
                            SketchwareUtil.toastError(
                                    "Failed to copy backup file to temporary location: " + reason,
                                    Toast.LENGTH_LONG);
                        }
                    }
                }).copyFile(data);
            }
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.item_projects) {
                navigateToProjectsFragment();
                return true;
            } else if (id == R.id.item_sketchub) {
                navigateToSketchubFragment();
                return true;
            }
            return false;
        });

        if (savedInstanceState != null) {
            projectsFragment = (ProjectsFragment) getSupportFragmentManager()
                    .findFragmentByTag(PROJECTS_FRAGMENT_TAG);
            projectsStoreFragment = (ProjectsStoreFragment) getSupportFragmentManager()
                    .findFragmentByTag(PROJECTS_STORE_FRAGMENT_TAG);
            currentNavItemId = savedInstanceState.getInt("selected_tab_id");
            Fragment current = getFragmentForNavId(currentNavItemId);
            if (current instanceof ProjectsFragment) {
                navigateToProjectsFragment();
            } else if (current instanceof ProjectsStoreFragment) {
                navigateToSketchubFragment();
            }
            setupFab();
            return;
        }

        navigateToProjectsFragment();
        setupFab();

        backupRestoreManager = new BackupRestoreManager(this, projectsFragment);
        Configs.mainActivity = this;
        DRSetup.startNow(this);
    }

    // ── FAB expand/collapse ──────────────────────────────────────────────────

    private void setupFab() {
        // Navigation bar inset untuk semua FAB agar tidak tertutup gesture bar
        Insetter.builder()
                .margin(WindowInsetsCompat.Type.navigationBars())
                .applyToView(binding.createNewProject);
        Insetter.builder()
                .margin(WindowInsetsCompat.Type.navigationBars())
                .applyToView(binding.fabRestore);
        Insetter.builder()
                .margin(WindowInsetsCompat.Type.navigationBars())
                .applyToView(binding.fabCreate);

        // FAB utama: toggle expand/collapse
        binding.createNewProject.setOnClickListener(v -> toggleFab());

        // FAB Restore
        binding.fabRestore.setOnClickListener(v -> {
            collapseFab();
            if (projectsFragment != null) projectsFragment.restoreProject();
        });

        // FAB Create New Project
        binding.fabCreate.setOnClickListener(v -> {
            collapseFab();
            if (projectsFragment != null) projectsFragment.toProjectSettingsActivity();
        });

        // Overlay: klik di luar FAB → collapse
        binding.fabOverlay.setOnClickListener(v -> collapseFab());
    }

    private void toggleFab() {
        if (isFabExpanded) {
            collapseFab();
        } else {
            expandFab();
        }
    }

    private void expandFab() {
        isFabExpanded = true;
        binding.fabOverlay.setVisibility(View.VISIBLE);
        binding.fabRestore.setVisibility(View.VISIBLE);
        binding.fabCreate.setVisibility(View.VISIBLE);
        binding.fabRestore.extend();
        binding.fabCreate.extend();
    }

    private void collapseFab() {
        isFabExpanded = false;
        binding.fabOverlay.setVisibility(View.GONE);
        binding.fabRestore.setVisibility(View.GONE);
        binding.fabCreate.setVisibility(View.GONE);
    }

    // ────────────────────────────────────────────────────────────────────────

    private Fragment getFragmentForNavId(int navItemId) {
        if (navItemId == R.id.item_projects) {
            return projectsFragment;
        } else if (navItemId == R.id.item_sketchub) {
            return projectsStoreFragment;
        }
        throw new IllegalArgumentException();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selected_tab_id", currentNavItemId);
    }

    private void navigateToProjectsFragment() {
        if (projectsFragment == null) {
            projectsFragment = new ProjectsFragment();
        }

        boolean shouldShow = true;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        binding.createNewProject.show();
        if (activeFragment != null) transaction.hide(activeFragment);
        if (fm.findFragmentByTag(PROJECTS_FRAGMENT_TAG) == null) {
            shouldShow = false;
            transaction.add(binding.container.getId(), projectsFragment, PROJECTS_FRAGMENT_TAG);
        }
        if (shouldShow) transaction.show(projectsFragment);
        transaction.commit();

        activeFragment = projectsFragment;
        currentNavItemId = R.id.item_projects;
    }

    private void navigateToSketchubFragment() {
        if (projectsStoreFragment == null) {
            projectsStoreFragment = new ProjectsStoreFragment();
        }

        boolean shouldShow = true;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        binding.createNewProject.hide();
        collapseFab(); // tutup sub-FAB saat pindah tab
        if (activeFragment != null) transaction.hide(activeFragment);
        if (fm.findFragmentByTag(PROJECTS_STORE_FRAGMENT_TAG) == null) {
            shouldShow = false;
            transaction.add(binding.container.getId(), projectsStoreFragment,
                    PROJECTS_STORE_FRAGMENT_TAG);
        }
        if (shouldShow) transaction.show(projectsStoreFragment);
        transaction.commit();

        activeFragment = projectsStoreFragment;
        currentNavItemId = R.id.item_sketchub;
    }

    @NonNull
    private BottomSheetDialogView getBottomSheetDialogView() {
        BottomSheetDialogView bottomSheetDialog = new BottomSheetDialogView(this);
        bottomSheetDialog.setTitle("Major changes in v7.0.0");
        bottomSheetDialog.setDescription("""
                There have been major changes since v6.3.0 fix1, \
                and it's very important to know them all if you want your projects to still work.
                
                You can view all changes whenever you want at the About Sketchware Pro screen.""");

        bottomSheetDialog.setPositiveButton("View changes", (dialog, which) -> {
            ConfigActivity.setSetting(ConfigActivity.SETTING_CRITICAL_UPDATE_REMINDER, true);
            Intent launcher = new Intent(this, AboutActivity.class);
            launcher.putExtra("select", "changelog");
            startActivity(launcher);
        });
        bottomSheetDialog.setCancelable(false);
        return bottomSheetDialog;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onResume() {
        super.onResume();
        long freeMegabytes = GB.c();
        if (freeMegabytes < 100 && freeMegabytes > 0) {
            showNoticeNotEnoughFreeStorageSpace();
        }
        if (isStoragePermissionGranted() && storageAccessDenied != null
                && storageAccessDenied.isShown()) {
            storageAccessDenied.dismiss();
        }
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "MainActivity");
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "MainActivity");
        mAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);

        if (needRefreshProjectList) {
            projectsFragment.refreshProjectsList();
            needRefreshProjectList = false;
        }

        GitQuickLook.cleanUp(this);

        if (!ConfigActivity.isSettingEnabled(ConfigActivity.SETTING_CRITICAL_UPDATE_REMINDER)
                && FilesTools.isPermissionGranted(this)) {
            BottomSheetDialogView bottomSheetDialog = getBottomSheetDialogView();
            bottomSheetDialog.getPositiveButton().setEnabled(false);

            CountDownTimer countDownTimer = new CountDownTimer(3000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    bottomSheetDialog.setPositiveButtonText(millisUntilFinished / 1000 + "");
                }

                @Override
                public void onFinish() {
                    bottomSheetDialog.setPositiveButtonText("View changes");
                    bottomSheetDialog.getPositiveButton().setEnabled(true);
                }
            };
            countDownTimer.start();

            if (!isFinishing()) bottomSheetDialog.show();
        }
    }

    private void allFilesAccessCheck() {
        if (Build.VERSION.SDK_INT > 29) {
            File optOutFile = new File(getFilesDir(), ".skip_all_files_access_notice");
            boolean granted = Environment.isExternalStorageManager();
            if (!optOutFile.exists() && !granted) {
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
                dialog.setIcon(R.drawable.ic_mtrl_warning);
                dialog.setTitle("Android 11 storage access");
                dialog.setMessage("Starting with Android 11, Sketchware Pro needs a new permission to avoid "
                        + "taking ages to build projects. Don't worry, we can't do more to storage than "
                        + "with current granted permissions.");
                dialog.setPositiveButton(Helper.getResString(R.string.common_word_settings),
                        (v, which) -> {
                            FileUtil.requestAllFilesAccessPermission(this);
                            v.dismiss();
                        });
                dialog.setNegativeButton("Skip", null);
                dialog.setNeutralButton("Don't show anymore", (v, which) -> {
                    try {
                        if (!optOutFile.createNewFile())
                            throw new IOException("Failed to create file " + optOutFile);
                    } catch (IOException e) {
                        Log.e("MainActivity",
                                "Error while trying to create \"Don't show Android 11 hint\" dialog file: "
                                        + e.getMessage(), e);
                    }
                    v.dismiss();
                });
                dialog.show();
            }
        }
    }

    private void showNoticeNeedStorageAccess() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(Helper.getResString(R.string.common_message_permission_title_storage));
        dialog.setIcon(R.drawable.ic_mtrl_folder);
        dialog.setMessage(
                Helper.getResString(R.string.common_message_permission_need_load_project));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), (v, which) -> {
            v.dismiss();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    9501);
        });
        dialog.show();
    }

    private void showNoticeNotEnoughFreeStorageSpace() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
        dialog.setTitle(
                Helper.getResString(R.string.common_message_insufficient_storage_space_title));
        dialog.setIcon(R.drawable.disc_full_24px);
        dialog.setMessage(
                Helper.getResString(R.string.common_message_insufficient_storage_space));
        dialog.setPositiveButton(Helper.getResString(R.string.common_word_ok), null);
        dialog.show();
    }

    public void s() {
        if (storageAccessDenied == null || !storageAccessDenied.isShown()) {
            storageAccessDenied = Snackbar.make(binding.layoutCoordinator,
                    Helper.getResString(R.string.common_message_permission_denied),
                    Snackbar.LENGTH_INDEFINITE);
            storageAccessDenied.setAction(Helper.getResString(R.string.common_word_settings),
                    v -> {
                        storageAccessDenied.dismiss();
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        Manifest.permission.READ_EXTERNAL_STORAGE},
                                9501);
                    });
            storageAccessDenied.setActionTextColor(Color.YELLOW);
            storageAccessDenied.show();
        }
    }
}