package com.example.carrentingtest.ui.verification;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

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

    public Uri getSelfieUri() { return savedStateHandle.get("selfie_uri"); }
    public Uri getLicenseUri() { return savedStateHandle.get("license_uri"); }
}


