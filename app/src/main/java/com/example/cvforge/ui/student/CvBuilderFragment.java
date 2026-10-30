package com.example.cvforge.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.cvforge.R;
import com.example.cvforge.data.CvRepository;
import com.example.cvforge.data.ResultListener;
import com.example.cvforge.data.UserRepository;
import com.example.cvforge.data.local.CvEntity;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.FragmentCvBuilderBinding;
import com.example.cvforge.ui.cv.CvEditorActivity;
import com.example.cvforge.ui.cv.CvSample;
import com.example.cvforge.ui.cv.CvTemplate;
import com.example.cvforge.ui.cv.CvVersionAdapter;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.PdfExporter;
import com.example.cvforge.util.ShareHelper;
import com.example.cvforge.util.StrengthScore;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** CV tab: CV versions, template pills and a live preview of the shown version. */
public class CvBuilderFragment extends Fragment implements CvVersionAdapter.Listener {

    private FragmentCvBuilderBinding binding;
    private CvRepository cvRepository;
    private final UserRepository userRepository = new UserRepository();
    private Cv cv = new Cv();
    private List<Cv> versions = new ArrayList<>();
    private CvVersionAdapter versionAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCvBuilderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cvRepository = new CvRepository(requireContext());

        versionAdapter = new CvVersionAdapter(this);
        binding.listVersions.setAdapter(versionAdapter);

        binding.pillModern.setOnClickListener(v -> chooseTemplate(CvTemplate.MODERN));
        binding.pillClassic.setOnClickListener(v -> chooseTemplate(CvTemplate.CLASSIC));
        binding.pillMinimal.setOnClickListener(v -> chooseTemplate(CvTemplate.MINIMAL));
        binding.btnExportPdf.setOnClickListener(v -> exportPdf());
        binding.btnEditCv.setOnClickListener(v -> openEditor());

        showTemplate(CvTemplate.MODERN);

        // A4: the blocks slide up one after the other
        AnimUtils.slideUpIn(binding.textTitle, 0);
        AnimUtils.slideUpIn(binding.listVersions, 1);
        AnimUtils.slideUpIn(binding.rowTemplates, 2);
        AnimUtils.slideUpIn(binding.cardPreview, 3);
        AnimUtils.slideUpIn(binding.cardStrength, 4);
    }

    /** Reloads the versions every time the tab is shown, so edits from the editor appear. */
    @Override
    public void onResume() {
        super.onResume();
        loadVersions(cv.id);
    }

    private void loadVersions(String selectId) {
        cvRepository.loadAll(userRepository.getCurrentUid(), loaded -> {
            if (binding == null) {
                return;
            }
            versions = loaded;
            show(findById(selectId));
        });
    }

    /** Finds a version by id. Falls back to the newest one, or to null when there are none. */
    private Cv findById(String id) {
        for (Cv version : versions) {
            if (version.id.equals(id)) {
                return version;
            }
        }
        return versions.isEmpty() ? null : versions.get(0);
    }

    private void show(Cv selected) {
        cv = selected == null ? new Cv() : selected;
        versionAdapter.setVersions(versions, cv.id);
        // until the student saves a CV, the preview shows an example
        binding.preview.setCv(selected == null ? CvSample.create() : cv);
        showTemplate(CvTemplate.fromId(cv.template));
        showStrength();
    }

    // ---------- versions ----------

    @Override
    public void onVersionClick(Cv version) {
        show(version);
    }

    @Override
    public void onVersionLongClick(Cv version) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_version_title)
                .setMessage(getString(R.string.delete_version_message, version.title))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteVersion(version))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteVersion(Cv version) {
        cvRepository.delete(version);
        versions.remove(version);
        show(version.id.equals(cv.id) ? findById(null) : cv);
    }

    /** Asks for a name, then makes a copy of the shown CV (or an empty CV if there is none). */
    @Override
    public void onNewVersionClick() {
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint(R.string.version_name_hint);
        input.setSingleLine(true);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.new_version_title)
                .setView(input, padding, padding / 2, padding, 0)
                .setPositiveButton(R.string.create, (d, which) ->
                        createVersion(input.getText().toString().trim()))
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.show();
    }

    private void createVersion(String name) {
        Cv copy = cv.id == null ? new Cv() : CvEntity.fromCv(cv).toCv();
        copy.id = UUID.randomUUID().toString();
        copy.ownerUid = userRepository.getCurrentUid();
        copy.title = name.isEmpty() ? getString(R.string.default_cv_title) : name;
        copy.updatedAt = System.currentTimeMillis();

        cvRepository.save(copy, new ResultListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(String message) {
                // the copy is still saved on the phone
            }
        });
        versions.add(0, copy);
        show(copy);
    }

    /** Makes the PDF on a background thread (PdfDocument), then opens the share sheet. */
    private void exportPdf() {
        if (cv.id == null) {
            Toast.makeText(requireContext(), R.string.save_cv_first, Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnExportPdf.setEnabled(false);
        binding.btnExportPdf.setText(R.string.exporting_pdf);
        PdfExporter.export(requireContext(), cv, CvTemplate.fromId(cv.template), new PdfExporter.Listener() {
            @Override
            public void onDone(File pdf) {
                finishExport(null);
                if (binding != null) {
                    ShareHelper.sharePdf(requireContext(), pdf);
                }
            }

            @Override
            public void onError(String message) {
                finishExport(getString(R.string.pdf_error, message));
            }
        });
    }

    private void finishExport(String message) {
        if (binding == null) {
            return;
        }
        binding.btnExportPdf.setEnabled(true);
        binding.btnExportPdf.setText(R.string.export_pdf);
        if (message != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    /** A3: the strength bar grows to the score of the shown CV. */
    private void showStrength() {
        StrengthScore.Result result = StrengthScore.of(cv);
        binding.textStrengthPercent.setText(getString(R.string.percent, result.score));
        binding.textStrengthTip.setText(result.tipRes == 0 ? R.string.strength_complete : result.tipRes);
        AnimUtils.growBar(binding.barStrength, result.score);
    }

    private void chooseTemplate(CvTemplate template) {
        cv.template = template.id;
        showTemplate(template);
        if (cv.id != null) {
            cv.updatedAt = System.currentTimeMillis();
            cvRepository.saveDraft(cv);
        }
    }

    /** The chosen pill gets the solid accent color, the others a soft tint. */
    private void showTemplate(CvTemplate selected) {
        binding.preview.setTemplate(selected);
        stylePill(binding.pillModern, CvTemplate.MODERN, selected);
        stylePill(binding.pillClassic, CvTemplate.CLASSIC, selected);
        stylePill(binding.pillMinimal, CvTemplate.MINIMAL, selected);
    }

    private void stylePill(TextView pill, CvTemplate template, CvTemplate selected) {
        boolean isSelected = template == selected;
        int background = isSelected ? template.accentColor : template.tintColor;
        int text = isSelected ? R.color.white : template.textColor;
        pill.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), background));
        pill.setTextColor(ContextCompat.getColor(requireContext(), text));
    }

    /** Explicit Intent: opens the CV editor. */
    private void openEditor() {
        Intent intent = new Intent(requireContext(), CvEditorActivity.class);
        if (cv.id != null) {
            intent.putExtra(CvEditorActivity.EXTRA_CV_ID, cv.id);
        }
        startActivity(intent);
        AnimUtils.fadeTransition(requireActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
