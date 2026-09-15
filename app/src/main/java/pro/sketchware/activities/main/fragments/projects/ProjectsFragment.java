package pro.sketchware.activities.main.fragments.projects;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DiffUtil;

import com.besome.sketch.adapters.ProjectsAdapter;
import com.besome.sketch.design.DesignActivity;
import com.besome.sketch.editor.manage.library.ProjectComparator;
import com.besome.sketch.projects.MyProjectSettingActivity;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
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
import dev.chrisbanes.insetter.Insetter;
import extensions.anbui.daydream.project.RestoreProject;
import mod.hey.studios.project.ProjectTracker;
import mod.hey.studios.project.backup.BackupRestoreManager;
import pro.sketchware.R;
import pro.sketchware.activities.main.activities.MainActivity;
import pro.sketchware.databinding.MyprojectsBinding;
import pro.sketchware.utility.UI;

//DR
public class ProjectsFragment extends DA {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final List<HashMap<String, Object>> projectsList = new ArrayList<>();
    private MyprojectsBinding binding;
    private ProjectsAdapter projectsAdapter;
    public final ActivityResultLauncher<Intent> openProjectSettings = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
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
    private EditText searchEditText;
    private TextWatcher searchTextWatcher;
    private ExtendedFloatingActionButton fabMain;
    private ExtendedFloatingActionButton createFab;
    private ExtendedFloatingActionButton restoreFab;
    private boolean fabMenuExpanded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setEnterTransition(new MaterialFadeThrough());
        setReturnTransition(new MaterialFadeThrough());
        setExitTransition(new MaterialFadeThrough());
        setReenterTransition(new MaterialFadeThrough());
    }

    @Override
    public void b(int requestCode) {
    }

    public void toDesignActivity(String sc_id) {
        Intent intent = new Intent(requireContext(), DesignActivity.class);
        ProjectTracker.setScId(sc_id);
        intent.putExtra("sc_id", sc_id);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        requireActivity().startActivity(intent);
    }

    @Override
    public void c(int requestCode) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
        startActivity(intent);
    }

    @Override
    public void d() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
    }

    @Override
    public void e() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).s();
        }
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
        detachSearchListener();
        binding = null; // avoid memory leaks
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        preference = new DB(requireContext(), "project");

        fabMain = requireActivity().findViewById(R.id.fab_main);
        createFab = requireActivity().findViewById(R.id.create_new_project);
        restoreFab = requireActivity().findViewById(R.id.restore_project);

        fabMain.setOnClickListener(v -> toggleFabMenu());
        createFab.setOnClickListener(v -> {
            collapseFabMenu();
            toProjectSettingsActivity();
        });
        restoreFab.setOnClickListener(v -> {
            collapseFabMenu();
            restoreProject();
        });

        Insetter.builder().margin(WindowInsetsCompat.Type.navigationBars()).applyToView(fabMain);
        Insetter.builder().margin(WindowInsetsCompat.Type.navigationBars()).applyToView(createFab);
        Insetter.builder().margin(WindowInsetsCompat.Type.navigationBars()).applyToView(restoreFab);

        binding.swipeRefresh.setOnRefreshListener(this::refreshProjectsList);

        projectsAdapter = new ProjectsAdapter(this, projectsList);
        binding.myprojects.setAdapter(projectsAdapter);
        binding.myprojects.setHasFixedSize(true);

        binding.myprojects.post(this::refreshProjectsList); // wait for RecyclerView to be ready
        UI.addSystemWindowInsetToPadding(binding.specialActionContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.loadingContainer, true, false, true, true);
        UI.addSystemWindowInsetToPadding(binding.titleContainer, true, false, true, false);
        UI.addSystemWindowInsetToPadding(binding.myprojects, true, false, true, true);

        binding.nestedScroll.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY != oldScrollY && fabMenuExpanded) {
                collapseFabMenu();
            }
        });

        // Sorting UI removed: the list always defaults to newest project (highest id) on
        // top, oldest (lowest id) at the bottom. See refreshProjectsList().
        binding.iconSort.setVisibility(View.GONE);

        // The old inline "restore" row is removed in favor of the Restore FAB above.
        // Restore hanya ada di FAB sekarang, bukan di special action container
        binding.specialActionContainer.setVisibility(View.GONE);

        RestoreProject.setupDropFileTo(getActivity(), binding.getRoot());

        searchEditText = requireActivity().findViewById(R.id.search_edit_text);
        ImageView searchIcon = requireActivity().findViewById(R.id.search_icon);
        if (searchIcon != null) {
            searchIcon.setOnClickListener(v -> {
                if (searchEditText == null) return;
                searchEditText.requestFocus();
                InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
            });
        }
        attachSearchListener();
    }

    private void attachSearchListener() {
        if (searchEditText == null) return;
        if (searchTextWatcher == null) {
            searchTextWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (projectsAdapter == null || binding == null) return;
                    String query = s.toString();
                    projectsAdapter.filterData(query);
                    binding.titleContainer.setVisibility(query.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            };
        }
        searchEditText.addTextChangedListener(searchTextWatcher);
    }

    private void detachSearchListener() {
        if (searchEditText != null && searchTextWatcher != null) {
            searchEditText.removeTextChangedListener(searchTextWatcher);
        }
    }

    /**
     * Buka menu FAB: tampilkan opsi Create & Restore dengan animasi, dan putar icon fab utama.
     */
    private void toggleFabMenu() {
        if (fabMenuExpanded) {
            collapseFabMenu();
        } else {
            expandFabMenu();
        }
    }

    private void expandFabMenu() {
        if (fabMenuExpanded || fabMain == null) return;
        fabMenuExpanded = true;
        fabMain.setIconResource(R.drawable.ic_mtrl_close);
        fabMain.shrink();
        showFabOption(createFab);
        showFabOption(restoreFab);
    }

    /**
     * Tutup menu FAB. Dipanggil saat: pilih salah satu opsi, scroll list,
     * pindah tab, atau klik di luar area FAB (lihat MainActivity#dispatchTouchEvent).
     */
    public void collapseFabMenu() {
        if (!fabMenuExpanded || fabMain == null) return;
        fabMenuExpanded = false;
        fabMain.setIconResource(R.drawable.ic_mtrl_add);
        fabMain.extend();
        hideFabOption(createFab);
        hideFabOption(restoreFab);
    }

    public boolean isFabMenuExpanded() {
        return fabMenuExpanded;
    }

    private void showFabOption(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setScaleX(0f);
        view.setScaleY(0f);
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200).start();
    }

    private void hideFabOption(View view) {
        if (view == null) return;
        view.animate().cancel();
        view.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (getActivity() == null) return;
        if (hidden) {
            collapseFabMenu();
            detachSearchListener();
        } else {
            attachSearchListener();
        }
    }

    public void refreshProjectsList() {
        // Check if the fragment is still attached to the activity
        if (!isAdded()) return;

        // Don't load project list without having permissions
        if (!c()) {
            if (binding.swipeRefresh.isRefreshing()) binding.swipeRefresh.setRefreshing(false);
            ((MainActivity) requireActivity()).s(); // ask for permissions
            return;
        }

        executorService.execute(() -> {
            List<HashMap<String, Object>> loadedProjects = lC.a();
            loadedProjects.sort(new ProjectComparator(ProjectComparator.SORT_BY_ID | ProjectComparator.SORT_ORDER_DESCENDING, preference.a("pinnedProject", "-1")));

            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProjectDiffCallback(projectsList, loadedProjects));

            requireActivity().runOnUiThread(() -> {
                if (binding.swipeRefresh.isRefreshing()) binding.swipeRefresh.setRefreshing(false);
                if (binding.loadingContainer.getVisibility() == View.VISIBLE) {
                    binding.loadingContainer.setVisibility(View.GONE);
                    binding.myprojects.setVisibility(View.VISIBLE);
                }
                projectsList.clear();
                projectsList.addAll(loadedProjects);
                diffResult.dispatchUpdatesTo(projectsAdapter);
                if (searchEditText != null)
                    projectsAdapter.filterData(searchEditText.getText().toString());
            });
        });
    }

    private void addProject(String sc_id) {
        executorService.execute(() -> {
            HashMap<String, Object> newProject = lC.b(sc_id);
            if (newProject != null) {
                requireActivity().runOnUiThread(() -> {
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
                int index = IntStream.range(0, projectsList.size()).filter(i -> projectsList.get(i).get("sc_id").equals(sc_id)).findFirst().orElse(-1);
                if (index != -1) {
                    projectsList.set(index, updatedProject);
                    requireActivity().runOnUiThread(() -> projectsAdapter.notifyDataSetChanged());
                }
            }
        });
    }

    private static class ProjectDiffCallback extends DiffUtil.Callback {
        private final List<HashMap<String, Object>> oldList;
        private final List<HashMap<String, Object>> newList;

        public ProjectDiffCallback(List<HashMap<String, Object>> oldList, List<HashMap<String, Object>> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            String oldId = (String) oldList.get(oldItemPosition).get("sc_id");
            String newId = (String) newList.get(newItemPosition).get("sc_id");
            return oldId.equals(newId);
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            HashMap<String, Object> oldItem = oldList.get(oldItemPosition);
            HashMap<String, Object> newItem = newList.get(newItemPosition);
            return oldItem.equals(newItem);
        }
    }
}
