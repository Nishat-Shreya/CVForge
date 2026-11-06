package com.example.cvforge.ui.post;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cvforge.R;
import com.example.cvforge.data.model.Application;
import com.example.cvforge.data.model.Cv;
import com.example.cvforge.databinding.ItemApplicantBinding;
import com.example.cvforge.util.AnimUtils;
import com.example.cvforge.util.MatchScore;

import java.util.ArrayList;
import java.util.List;

/** The applicants of one internship, with the buttons that fit the tab they are in. */
public class ApplicantAdapter extends RecyclerView.Adapter<ApplicantAdapter.ViewHolder> {

    public interface Listener {
        void onOpenCv(Application application);

        /** The main button of the card: Shortlist, Interview or Offer. */
        void onPrimaryAction(Application application);

        void onReject(Application application);
    }

    private final List<Application> applicants = new ArrayList<>();
    /** The skills the internship asks for, used for the match ring. */
    private List<String> requiredSkills = new ArrayList<>();
    private final Listener listener;

    public ApplicantAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setApplicants(List<Application> newApplicants, List<String> requiredSkills) {
        applicants.clear();
        applicants.addAll(newApplicants);
        this.requiredSkills = requiredSkills == null ? new ArrayList<>() : requiredSkills;
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return applicants.isEmpty();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemApplicantBinding binding = ItemApplicantBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(applicants.get(position));
        // A4: the cards slide up one after the other
        AnimUtils.slideUpIn(holder.itemView, Math.min(position, 6));
    }

    @Override
    public int getItemCount() {
        return applicants.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemApplicantBinding binding;

        ViewHolder(ItemApplicantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Application application) {
            Context context = itemView.getContext();
            Cv cv = application.cv == null ? new Cv() : application.cv;

            binding.textName.setText(application.studentName);
            binding.avatar.textInitials.setText(initials(application.studentName));
            binding.textSchool.setText(schoolLine(cv));
            showMatch(context, cv);
            showSkills(context, cv);
            showButtons(context, application);

            binding.getRoot().setOnClickListener(v -> listener.onOpenCv(application));
            binding.btnPrimary.setOnClickListener(v -> listener.onPrimaryAction(application));
            binding.btnReject.setOnClickListener(v -> listener.onReject(application));
        }

        private String initials(String name) {
            if (name == null || name.trim().isEmpty()) {
                return "?";
            }
            String[] parts = name.trim().split("\\s+");
            String second = parts.length > 1 ? parts[1].substring(0, 1) : "";
            return (parts[0].substring(0, 1) + second).toUpperCase();
        }

        /** "CSE · KUET": the degree and the school of the first education entry. */
        private String schoolLine(Cv cv) {
            if (cv.education == null || cv.education.isEmpty()) {
                return cv.headline == null ? "" : cv.headline;
            }
            Cv.Education education = cv.education.get(0);
            String degree = education.degree == null ? "" : education.degree;
            String school = education.school == null ? "" : education.school;
            return degree.isEmpty() ? school : school.isEmpty() ? degree : degree + " · " + school;
        }

        /** A2: the ring fills. Teal for 90 and up, sky for 80 and up, amber below. */
        private void showMatch(Context context, Cv cv) {
            int percent = MatchScore.compare(cv.skills, requiredSkills).percent;
            int colorRes = percent >= 90 ? R.color.teal : percent >= 80 ? R.color.sky : R.color.amber;
            binding.ringMatch.setRingColor(ContextCompat.getColor(context, colorRes));
            binding.ringMatch.animateTo(percent, 1300);
        }

        private void showSkills(Context context, Cv cv) {
            binding.groupSkills.removeAllViews();
            if (cv.skills == null) {
                return;
            }
            for (int i = 0; i < cv.skills.size() && i < 4; i++) {
                String skill = cv.skills.get(i);
                boolean required = MatchScore.compare(java.util.Collections.singletonList(skill), requiredSkills).percent > 0;
                addChip(context, skill, required ? R.color.teal_tint : R.color.violet_tint,
                        required ? R.color.teal_text : R.color.violet_text);
            }
        }

        private void addChip(Context context, String text, int backgroundRes, int textRes) {
            TextView chip = (TextView) LayoutInflater.from(context).inflate(R.layout.include_status_chip, binding.groupSkills, false);
            chip.setText(text);
            chip.setBackgroundTintList(ContextCompat.getColorStateList(context, backgroundRes));
            chip.setTextColor(ContextCompat.getColor(context, textRes));
            binding.groupSkills.addView(chip);
        }

        /** New: Shortlist. Shortlisted: Interview. Interview: Offer. Reject is always there. */
        private void showButtons(Context context, Application application) {
            int labelRes;
            int colorRes;
            switch (application.status) {
                case Application.STATUS_SHORTLISTED:
                    labelRes = R.string.action_interview;
                    colorRes = R.color.amber;
                    break;
                case Application.STATUS_INTERVIEW:
                    labelRes = R.string.action_offer;
                    colorRes = R.color.teal;
                    break;
                default:
                    labelRes = R.string.action_shortlist;
                    colorRes = R.color.teal;
                    break;
            }
            binding.btnPrimary.setText(labelRes);
            binding.btnPrimary.setBackgroundTintList(ContextCompat.getColorStateList(context, colorRes));
        }
    }
}
