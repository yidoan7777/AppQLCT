package com.example.appqlct.fragment.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appqlct.R;
import com.example.appqlct.helper.FirebaseHelper;
import com.example.appqlct.helper.NotificationHelper;
import com.example.appqlct.helper.SharedPreferencesHelper;
import com.example.appqlct.model.Feedback;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Date;

/**
 * FeedbackFragment - Gửi phản hồi/đánh giá ứng dụng
 * Cho phép đánh giá sao và gửi nhận xét
 */
public class FeedbackFragment extends Fragment {
    private RatingBar ratingBar;
    private TextInputEditText etFeedback;
    private MaterialButton btnSubmit;
    private FirebaseHelper firebaseHelper;
    private SharedPreferencesHelper prefsHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        initViews(view);
        initHelpers();

        return view;
    }

    private void initViews(View view) {
        ratingBar = view.findViewById(R.id.ratingBar);
        etFeedback = view.findViewById(R.id.etFeedback);
        btnSubmit = view.findViewById(R.id.btnSubmit);

        btnSubmit.setOnClickListener(v -> submitFeedback());
    }

    private void initHelpers() {
        firebaseHelper = new FirebaseHelper();
        prefsHelper = new SharedPreferencesHelper(requireContext());
    }

    /**
     * Gửi feedback lên Firestore
     */
    private void submitFeedback() {
        float rating = ratingBar.getRating();
        String comment = etFeedback.getText().toString().trim();
        String userId = prefsHelper.getUserId();

        if (rating == 0) {
            NotificationHelper.addInfoNotification(requireContext(), userId, 
                    "Vui lòng chọn đánh giá");
            return;
        }

        if (comment.isEmpty()) {
            etFeedback.setError(getString(R.string.required_field));
            return;
        }

        Feedback feedback = new Feedback(
                null, // id sẽ được tạo bởi Firestore
                userId,
                rating,
                comment,
                new Date()
        );

        firebaseHelper.addFeedback(feedback, task -> {
            if (!isAdded() || getContext() == null) return;
            if (task.isSuccessful()) {
                NotificationHelper.addSuccessNotification(getContext(), userId, 
                        "Cảm ơn bạn đã gửi phản hồi!");
                // Reset form
                ratingBar.setRating(0);
                etFeedback.setText("");
            } else {
                NotificationHelper.addErrorNotification(getContext(), userId, 
                        getString(R.string.error));
            }
        });
    }
}

