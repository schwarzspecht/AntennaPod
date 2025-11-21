package de.danoeh.antennapod.ui.screen.queue;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;

public class QueueTagFilterDialog extends DialogFragment {
    
    public interface OnTagFilterChangedListener {
        void onTagFilterChanged(Set<String> selectedTags);
    }
    
    private OnTagFilterChangedListener listener;
    private Set<String> selectedTags = new HashSet<>();
    private List<String> allTags = new ArrayList<>();
    
    public static QueueTagFilterDialog newInstance(Set<String> selectedTags) {
        QueueTagFilterDialog dialog = new QueueTagFilterDialog();
        dialog.selectedTags = new HashSet<>(selectedTags);
        return dialog;
    }
    
    public void setOnTagFilterChangedListener(OnTagFilterChangedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_tag_filter, null);
        LinearLayout tagContainer = view.findViewById(R.id.tag_container);
        
        // Show loading state
        ProgressBar progressBar = new ProgressBar(getContext());
        tagContainer.addView(progressBar);
        
        // Load tags asynchronously
        loadAllTags(tagContainer, progressBar);
        
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.filter_tags_label)
                .setView(view)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    if (listener != null) {
                        listener.onTagFilterChanged(selectedTags);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.reset, (dialog, which) -> {
                    selectedTags.clear();
                    if (listener != null) {
                        listener.onTagFilterChanged(selectedTags);
                    }
                })
                .create();
    }
    
    private void loadAllTags(LinearLayout tagContainer, ProgressBar progressBar) {
        new Thread(() -> {
            try {
                Set<String> tagSet = new HashSet<>();
                List<Feed> feeds = DBReader.getFeedList();
                for (Feed feed : feeds) {
                    if (feed.getPreferences() != null) {
                        tagSet.addAll(feed.getPreferences().getTags());
                    }
                }
                
                getActivity().runOnUiThread(() -> {
                    allTags.clear();
                    allTags.addAll(tagSet);
                    allTags.sort(String::compareToIgnoreCase);
                    
                    // Remove loading indicator
                    tagContainer.removeView(progressBar);
                    
                    // Add tag checkboxes
                    for (String tag : allTags) {
                        View tagView = LayoutInflater.from(getContext()).inflate(R.layout.tag_filter_item, tagContainer, false);
                        CheckBox checkBox = tagView.findViewById(R.id.tag_checkbox);
                        TextView tagName = tagView.findViewById(R.id.tag_name);
                        
                        tagName.setText(tag);
                        checkBox.setChecked(selectedTags.contains(tag));
                        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                            if (isChecked) {
                                selectedTags.add(tag);
                            } else {
                                selectedTags.remove(tag);
                            }
                        });
                        
                        tagContainer.addView(tagView);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
