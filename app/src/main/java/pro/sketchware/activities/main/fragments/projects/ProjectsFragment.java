package pro.sketchware.activities.main.fragments.projects;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;

import com.besome.sketch.adapters.ProjectsAdapter;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.library.ProjectComparator;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialFadeThrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import a.a.a.DA;
import a.a.a.DB;
import a.a.a.lC;
import extensions.anbui.daydream.project.RestoreProject;
import mod.hey.studios.project.ProjectTracker;
import mod.hey.studios.project.backup.BackupRestoreManager;
import pro.sketchware.R;
import pro.sketchware.activities.main.activities.MainActivity;
import pro.sketchware.databinding.MyprojectsBinding;
import pro.sketchware.utility.UI;

public class ProjectsFragment extends DA {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<HashMap<String, Object>> projectsList = new ArrayList<>();
    private MyprojectsBinding binding;
    private ProjectsAdapter projectsAdapter;

    public final ActivityResultLauncher<Intent> openProjectSettings = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String sc_id = data.getStringExtra("sc_id");
                        if (data.getBooleanExtra("is_new", false)) {
                            toDesignActivity(sc_id);
                            addProject(sc_id);
                        } else {
                            updateProject(sc_id);
                        }
                    }
                }
            });

    private DB preference;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
    }

    @Override
    public void b(int requestCode) {}

    @Override
    public void c(int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    @Override
    public void d() {
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).s();
    }

    @Override
    public void e() {
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).s();
    }

    public void toDesignActivity(String sc_id) {
        Intent intent = new Intent(requireContext(), DesignActivity.class);
        ProjectTracker.setScId(sc_id);
        intent.putExtra("sc_id", sc_id);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
    }

    public void toProjectSettingsActivity() {
        Intent intent = new Intent(getActivity(), MyProjectSettingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openProjectSettings.launch(intent);
    }

    public void restoreProject() {
        new BackupRestoreManager(getActivity(), this).restore();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        binding = MyprojectsBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        preference = new DB(requireContext(), "project");

        binding.swipeRefresh.setOnRefreshListener(this::refreshProjectsList);

        projectsAdapter = new ProjectsAdapter(this, projectsList);
        binding.myprojects.setAdapter(projectsAdapter);
        binding.myprojects.setHasFixedSize(true);

        // ── Tidak ada ItemDecoration tambahan — jarak antar item kembali ke default ──

        // Sembunyikan special action container (Restore / Clone card di atas list)
        if (binding.specialAction != null) {
            binding.specialAction.getRoot().setVisibility(View.GONE);
            binding.specialActionContainer.setVisibility(View.GONE);
        }

        UI.addSystemWindowInsetToPadding(binding.specialActionContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.loadingContainer, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.titleContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.myprojects, true, false, true, true);

        // Sort button — gunakan icon category (@drawable/ic_baseline_category_24 atau
        // ic_mtrl_category, sesuaikan dengan yang tersedia di project)
        binding.iconSort.setImageResource(R.drawable.ic_mtrl_sort); // ganti ke ic_baseline_category_24 jika ada
        binding.iconSort.setOnClickListener(v -> showProjectSortingDialog());

        // ── Load langsung saat buka app, selama izin sudah diberikan ──
        // Tidak perlu menyentuh SearchView terlebih dahulu.
        binding.myprojects.post(this::refreshProjectsList);
    }

    // ── Search filter (dipanggil dari MainActivity SearchView) ───────────────

    public void filterFromSearch(String query) {
        if (binding == null || projectsAdapter == null) return;
        projectsAdapter.filterData(query);
        if (query == null || query.isEmpty()) {
            binding.titleContainer.setVisibility(View.VISIBLE);
        } else {
            binding.titleContainer.setVisibility(View.GONE);
        }
    }

    // ── Load projects ────────────────────────────────────────────────────────

    public void refreshProjectsList() {
        if (!isAdded()) return;

        // Jika belum ada izin, tampilkan snackbar minta izin lalu berhenti
        if (!c()) {
            if (binding != null && binding.swipeRefresh.isRefreshing()) {
                binding.swipeRefresh.setRefreshing(false);
            }
            if (binding != null && binding.loadingContainer.getVisibility() == View.VISIBLE) {
                binding.loadingContainer.setVisibility(View.GONE);
            }
            ((MainActivity) requireActivity()).s();
            return;
        }

        // Punya izin → langsung muat project list
        executorService.execute(() -> {
            List<HashMap<String, Object>> loadedProjects = lC.a();
            loadedProjects.sort(
                    new ProjectComparator(preference.d("sortBy"), preference.a("pinnedProject", "-1")));

            DiffUtil.DiffResult diffResult =
                    DiffUtil.calculateDiff(new ProjectDiffCallback(projectsList, loadedProjects));

            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                if (binding.swipeRefresh.isRefreshing()) binding.swipeRefresh.setRefreshing(false);
                if (binding.loadingContainer.getVisibility() == View.VISIBLE) {
                    binding.loadingContainer.setVisibility(View.GONE);
                    binding.myprojects.setVisibility(View.VISIBLE);
                }
                projectsList.clear();
                projectsList.addAll(loadedProjects);
                diffResult.dispatchUpdatesTo(projectsAdapter);
                // Terapkan filter search yang sedang aktif (jika ada)
                projectsAdapter.filterData("");
            });
        });
    }

    private void addProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> newProject = lC.b(sc_id);
            if (newProject != null) {
                requireActivity().runOnUiThread(() -> {
                    if (binding == null) return;
                    projectsList.add(0, newProject);
                    projectsAdapter.notifyDataSetChanged();
                    binding.myprojects.scrollToPosition(0);
                });
            }
        });
    }

    private void updateProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> updatedProject = lC.b(sc_id);
            if (updatedProject != null) {
                int index = IntStream.range(0, projectsList.size())
                        .filter(i -> projectsList.get(i).get("sc_id").equals(sc_id))
                        .findFirst().orElse(-1);
                if (index != -1) {
                    projectsList.set(index, updatedProject);
                    requireActivity().runOnUiThread(() -> projectsAdapter.notifyDataSetChanged());
                }
            }
        });
    }

    // ── Sort dialog — 4 pilihan bersih: A→Z, Z→A, Oldest, Newest ────────────

    private void showProjectSortingDialog() {
        // Label bersih tanpa simbol jelek
        final String[] sortLabels = {"A → Z", "Z → A", "Oldest first", "Newest first"};

        // Mapping ke nilai ProjectComparator:
        //   A→Z  = SORT_BY_NAME | SORT_ORDER_ASCENDING
        //   Z→A  = SORT_BY_NAME | SORT_ORDER_DESCENDING
        //   Old  = SORT_BY_ID   | SORT_ORDER_ASCENDING
        //   New  = SORT_BY_ID   | SORT_ORDER_DESCENDING
        final int[] sortValues = {
                ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_ASCENDING,
                ProjectComparator.SORT_BY_NAME | ProjectComparator.SORT_ORDER_DESCENDING,
                ProjectComparator.SORT_BY_ID   | ProjectComparator.SORT_ORDER_ASCENDING,
                ProjectComparator.SORT_BY_ID   | ProjectComparator.SORT_ORDER_DESCENDING
        };

        int currentValue = preference.a("sortBy", ProjectComparator.DEFAULT);

        // Cari indeks yang cocok dengan nilai saat ini
        int checkedItem = 0; // default A→Z
        for (int i = 0; i < sortValues.length; i++) {
            if (sortValues[i] == currentValue) {
                checkedItem = i;
                break;
            }
        }

        final int[] selected = {checkedItem};

        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Sort projects")
                // Icon category untuk header dialog — mengganti icon •aZ• jelek
                .setIcon(R.drawable.ic_mtrl_sort) // ganti ke ic_baseline_category_24 jika drawable tersedia
                .setSingleChoiceItems(sortLabels, checkedItem, (dialog, which) -> {
                    selected[0] = which;
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    preference.a("sortBy", sortValues[selected[0]], true);
                    dialog.dismiss();
                    refreshProjectsList();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── DiffUtil ─────────────────────────────────────────────────────────────

    private static class ProjectDiffCallback extends DiffUtil.Callback {
        private final List<HashMap<String, Object>> oldList;
        private final List<HashMap<String, Object>> newList;

        public ProjectDiffCallback(List<HashMap<String, Object>> oldList,
                List<HashMap<String, Object>> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList.size(); }
        @Override public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            String oldId = (String) oldList.get(oldPos).get("sc_id");
            String newId = (String) newList.get(newPos).get("sc_id");
            return oldId != null && oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).equals(newList.get(newPos));
        }
    }
}
