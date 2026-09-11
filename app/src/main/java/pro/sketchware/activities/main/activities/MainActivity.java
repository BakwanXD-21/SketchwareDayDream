package pro.sketchware.activities.main.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.WindowInsetsCompat;
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
import extensions.anbui.daydream.git.GitQuickLook;
import extensions.anbui.daydream.setup.DRSetup;
import mod.hey.studios.project.backup.BackupFactory;
import mod.hey.studios.project.backup.BackupRestoreManager;
import mod.hey.studios.util.Helper;
import mod.tyron.backup.SingleCopyTask;
import pro.sketchware.R;
import pro.sketchware.activities.main.fragments.projects.ProjectsFragment;
import pro.sketchware.activities.main.fragments.projects_store.ProjectsStoreFragment;
import pro.sketchware.databinding.MainBinding;
import pro.sketchware.utility.DataResetter;
import pro.sketchware.utility.FileUtil;
import pro.sketchware.utility.SketchwareUtil;
import pro.sketchware.utility.UI;

//DR
public class MainActivity extends BasePermissionAppCompatActivity {
    private static final String PROJECTS_FRAGMENT_TAG = "projects_fragment";
    private static final String PROJECTS_STORE_FRAGMENT_TAG = "projects_store_fragment";

    // Lebar drawer dalam dp — harus sama dengan main.xml android:layout_width="280dp"
    private static final float DRAWER_WIDTH_DP = 280f;

    private DB u;
    private Snackbar storageAccessDenied;
    private MainBinding binding;
    private boolean isDrawerOpen = false;
    private float drawerWidthPx;

    private final OnBackPressedCallback closeDrawer = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            closeDrawer();
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

    // ── Permission callbacks ────────────────────────────────────────────────

    @Override
    // Dipanggil setelah izin storage diberikan (request code 9501)
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
    public void l() {}

    @Override
    public void m() {}

    public void n() {
        if (activeFragment instanceof ProjectsFragment) {
            projectsFragment.refreshProjectsList();
        }
    }

    // ── onActivityResult ────────────────────────────────────────────────────

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
                    if (!(data.getStringExtra("save_as_new_id") == null
                            ? "" : data.getStringExtra("save_as_new_id")).isEmpty()
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
        // tidak ada drawerToggle lagi, tidak perlu sync
    }

    // ── onCreate ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Splash screen — harus dipanggil SEBELUM super.onCreate
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        enableEdgeToEdgeNoContrast();

        binding = MainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar hanya untuk icon hamburger (tidak pakai ActionBarDrawerToggle)
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);

        // Ganti icon hamburger menjadi "menu" dan handle klik untuk open/close drawer
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (isDrawerOpen) closeDrawer();
            else openDrawer();
        });

        // Konversi lebar drawer ke pixel
        drawerWidthPx = DRAWER_WIDTH_DP * getResources().getDisplayMetrics().density;

        // Pastikan drawer dimulai dari posisi tersembunyi
        binding.leftDrawer.setTranslationX(-drawerWidthPx);

        // Back press: tutup drawer
        getOnBackPressedDispatcher().addCallback(this, closeDrawer);

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

        // Hilangkan underline SearchView
        View searchPlate = binding.searchView.findViewById(androidx.appcompat.R.id.search_plate);
        if (searchPlate != null) searchPlate.setBackgroundColor(Color.TRANSPARENT);

        // SearchView → filter ProjectsFragment langsung, tanpa perlu ketuk icon
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
        } else {
            allFilesAccessCheck();
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Uri data = getIntent().getData();
            if (data != null) {
                new SingleCopyTask(this, new SingleCopyTask.CallBackTask() {
                    @Override
                    public void onCopyPreExecute() {}

                    @Override
                    public void onCopyProgressUpdate(int progress) {}

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

    // ── Drawer open/close dengan animasi slide (gaya DeepSeek) ─────────────

    private void openDrawer() {
        if (isDrawerOpen) return;
        isDrawerOpen = true;
        closeDrawer.setEnabled(true);

        binding.leftDrawer.setVisibility(View.VISIBLE);
        binding.leftDrawer.animate()
                .translationX(0f)
                .setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();

        // Geser konten utama ke kanan sejauh lebar drawer agar sejajar
        binding.layoutCoordinator.animate()
                .translationX(drawerWidthPx)
                .setDuration(280)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
    }

    private void closeDrawer() {
        if (!isDrawerOpen) return;
        isDrawerOpen = false;
        closeDrawer.setEnabled(false);

        binding.leftDrawer.animate()
                .translationX(-drawerWidthPx)
                .setDuration(240)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .start();

        binding.layoutCoordinator.animate()
                .translationX(0f)
                .setDuration(240)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .start();
    }

    // ── FAB expand/collapse dengan animasi pop ──────────────────────────────

    private void setupFab() {
        Insetter.builder()
                .margin(WindowInsetsCompat.Type.navigationBars())
                .applyToView(binding.createNewProject);
        Insetter.builder()
                .margin(WindowInsetsCompat.Type.navigationBars())
                .applyToView(binding.fabRestore);
        Insetter.builder()
                .margin(WindowInsetsCompat.Type.navigationBars())
                .applyToView(binding.fabCreate);

        binding.createNewProject.setOnClickListener(v -> toggleFab());

        binding.fabRestore.setOnClickListener(v -> {
            collapseFab();
            if (projectsFragment != null) projectsFragment.restoreProject();
        });

        binding.fabCreate.setOnClickListener(v -> {
            collapseFab();
            if (projectsFragment != null) projectsFragment.toProjectSettingsActivity();
        });

        binding.fabOverlay.setOnClickListener(v -> collapseFab());
    }

    private void toggleFab() {
        if (isFabExpanded) collapseFab();
        else expandFab();
    }

    private void expandFab() {
        isFabExpanded = true;

        // Tampilkan overlay
        binding.fabOverlay.setVisibility(View.VISIBLE);
        binding.fabOverlay.setAlpha(0f);
        binding.fabOverlay.animate().alpha(1f).setDuration(200).start();

        // Animasikan sub-FAB dengan delay bertahap (stagger) + overshoot
        animateSubFabIn(binding.fabRestore, 0);
        animateSubFabIn(binding.fabCreate, 60);
    }

    private void animateSubFabIn(View fab, long delayMs) {
        fab.setVisibility(View.VISIBLE);
        fab.setAlpha(0f);
        fab.setScaleX(0.5f);
        fab.setScaleY(0.5f);
        fab.setTranslationY(40f);

        fab.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(300)
                .setStartDelay(delayMs)
                .setInterpolator(new OvershootInterpolator(1.4f))
                .start();
    }

    private void collapseFab() {
        if (!isFabExpanded) return;
        isFabExpanded = false;

        // Fade out overlay
        binding.fabOverlay.animate()
                .alpha(0f)
                .setDuration(180)
                .withEndAction(() -> binding.fabOverlay.setVisibility(View.GONE))
                .start();

        // Animate sub-FAB keluar
        animateSubFabOut(binding.fabCreate, 0);
        animateSubFabOut(binding.fabRestore, 40);
    }

    private void animateSubFabOut(View fab, long delayMs) {
        fab.animate()
                .alpha(0f)
                .scaleX(0.5f)
                .scaleY(0.5f)
                .translationY(20f)
                .setDuration(200)
                .setStartDelay(delayMs)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    fab.setVisibility(View.GONE);
                    fab.setTranslationY(0f);
                })
                .start();
    }

    // ────────────────────────────────────────────────────────────────────────

    private Fragment getFragmentForNavId(int navItemId) {
        if (navItemId == R.id.item_projects) return projectsFragment;
        else if (navItemId == R.id.item_sketchub) return projectsStoreFragment;
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
        collapseFab();
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

    // ── onResume — changelog bottomsheet DIHAPUS ────────────────────────────

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
            if (projectsFragment != null) projectsFragment.refreshProjectsList();
            needRefreshProjectList = false;
        }

        GitQuickLook.cleanUp(this);

        // ── Changelog BottomSheet DIHAPUS ──
        // Tidak ada lagi popup "Major changes in v7.0.0" yang mengganggu saat pertama buka app.
        // Jika ingin menampilkan changelog, gunakan menu About saja.
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

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
