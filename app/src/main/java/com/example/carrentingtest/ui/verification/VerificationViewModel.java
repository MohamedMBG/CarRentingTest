package com.example.carrentingtest.ui.verification;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.carrentingtest.verification.data.LivenessAction;

public class VerificationViewModel extends ViewModel {
    private final SavedStateHandle savedStateHandle;
    private final MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);

    public VerificationViewModel() {
        this.savedStateHandle = new SavedStateHandle();
    }

    public LiveData<Boolean> getIsSubmitting() { return isSubmitting; }

    public void setSubmitting(boolean submitting) { isSubmitting.postValue(submitting); }

    public void setSelfieUri(Uri uri) { savedStateHandle.set("selfie_uri", uri); }
    public void setLicenseUri(Uri uri) { savedStateHandle.set("license_uri", uri); }
    public void setLivenessAction(LivenessAction action) { savedStateHandle.set("liveness_action", action.name()); }

    public Uri getSelfieUri() { return savedStateHandle.get("selfie_uri"); }
    public Uri getLicenseUri() { return savedStateHandle.get("license_uri"); }
    public LivenessAction getLivenessAction() {
        String value = savedStateHandle.get("liveness_action");
        if (value == null) {
            LivenessAction action = LivenessAction.random();
            setLivenessAction(action);
            return action;
        }
        return LivenessAction.from(value);
    }
}


