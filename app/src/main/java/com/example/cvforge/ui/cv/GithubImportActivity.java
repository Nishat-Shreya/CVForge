package com.example.cvforge.ui.cv;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ActivityGithubImportBinding;
import com.example.cvforge.network.ApiClient;
import com.example.cvforge.network.GithubApi;
import com.example.cvforge.network.model.GithubRepo;
import com.example.cvforge.util.AnimUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Feature 18: the student types a GitHub username, ticks repositories and adds them
 * to the Projects of a CV. The repositories are loaded with Retrofit.
 */
public class GithubImportActivity extends AppCompatActivity {

    /** Id of the CV version that gets the projects. */
    public static final String EXTRA_CV_ID = "cv_id";

    private ActivityGithubImportBinding binding;
    private RepoAdapter adapter;
    private CvRepository cvRepository;
    private final GithubApi githubApi = ApiClient.create(GithubApi.BASE_URL, GithubApi.class);
    private Call<List<GithubRepo>> runningCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGithubImportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        cvRepository = new CvRepository(this);

        binding.toolbar.textTitle.setText(R.string.import_from_github);
        binding.toolbar.btnBack.setOnClickListener(v -> finish());

        adapter = new RepoAdapter(count -> binding.btnAddToCv.setEnabled(count > 0));
        binding.listRepos.setLayoutManager(new LinearLayoutManager(this));
        binding.listRepos.setAdapter(adapter);

        binding.btnFind.setOnClickListener(v -> findRepos());
        binding.editUsername.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                findRepos();
                return true;
            }
            return false;
        });
        binding.btnAddToCv.setOnClickListener(v -> addToCv());

        AnimUtils.slideUpIn(binding.rowSearch, 0);
    }

    // ---------- loading the repositories ----------

    private void findRepos() {
        String username = binding.editUsername.getText().toString().trim();
        if (username.isEmpty()) {
            binding.editUsername.setError(getString(R.string.error_github_username));
            return;
        }

        showLoading();
        // newest updated first, the app drops forks and sorts by stars itself
        runningCall = githubApi.getRepos(username, "updated", 30);
        runningCall.enqueue(new Callback<List<GithubRepo>>() {
            @Override
            public void onResponse(Call<List<GithubRepo>> call, Response<List<GithubRepo>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showRepos(response.body());
                } else {
                    showMessage(getString(R.string.github_error_title), errorText(response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<GithubRepo>> call, Throwable t) {
                if (!call.isCanceled()) {
                    showMessage(getString(R.string.github_error_title), getString(R.string.no_internet));
                }
            }
        });
    }

    private String errorText(int code) {
        if (code == 404) {
            return getString(R.string.github_not_found);
        }
        if (code == 403) {
            return getString(R.string.github_rate_limit);
        }
        return getString(R.string.github_error_code, code);
    }

    private void showRepos(List<GithubRepo> all) {
        List<GithubRepo> repos = new ArrayList<>();
        for (GithubRepo repo : all) {
            if (!repo.fork) {
                repos.add(repo);
            }
        }
        Collections.sort(repos, (a, b) -> Integer.compare(b.stars, a.stars));

        binding.progress.setVisibility(View.GONE);
        adapter.setRepos(repos);
        if (repos.isEmpty()) {
            showMessage(getString(R.string.github_empty_title), getString(R.string.github_empty_message));
        } else {
            binding.empty.emptyState.setVisibility(View.GONE);
        }
    }

    private void showLoading() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.empty.emptyState.setVisibility(View.GONE);
        adapter.setRepos(new ArrayList<>());
    }

    private void showMessage(String title, String message) {
        binding.progress.setVisibility(View.GONE);
        binding.empty.textEmptyTitle.setText(title);
        binding.empty.textEmptyMessage.setText(message);
        binding.empty.emptyState.setVisibility(View.VISIBLE);
    }

    // ---------- adding to the CV ----------

    private void addToCv() {
        String cvId = getIntent().getStringExtra(EXTRA_CV_ID);
        if (cvId == null) {
            Toast.makeText(this, R.string.save_cv_first, Toast.LENGTH_LONG).show();
            return;
        }
        List<GithubRepo> selected = adapter.getSelected();
        binding.btnAddToCv.setEnabled(false);

        cvRepository.loadById(cvId, cv -> {
            if (cv == null) {
                Toast.makeText(this, R.string.save_cv_first, Toast.LENGTH_LONG).show();
                return;
            }
            int added = addProjects(cv, selected);
            cv.updatedAt = System.currentTimeMillis();
            cvRepository.save(cv, new ResultListener() {
                @Override
                public void onSuccess() {
                }

                @Override
                public void onError(String message) {
                    // the CV is still saved on the phone
                }
            });
            Toast.makeText(this, getString(R.string.projects_added, added), Toast.LENGTH_SHORT).show();
            finish();
            AnimUtils.fadeTransition(this);
        });
    }

    /** Adds the repositories as projects marked "From GitHub". Returns how many were new. */
    private int addProjects(Cv cv, List<GithubRepo> repos) {
        int added = 0;
        for (GithubRepo repo : repos) {
            if (alreadyInCv(cv, repo)) {
                continue;
            }
            Cv.Project project = new Cv.Project();
            project.name = repo.name;
            project.description = repo.description;
            if (repo.language != null) {
                project.tags.add(repo.language);
            }
            project.fromGithub = true;
            project.stars = repo.stars;
            project.url = repo.url;
            cv.projects.add(project);
            added++;
        }
        return added;
    }

    private boolean alreadyInCv(Cv cv, GithubRepo repo) {
        for (Cv.Project project : cv.projects) {
            if (repo.url != null && repo.url.equals(project.url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runningCall != null) {
            runningCall.cancel();
        }
    }
}
