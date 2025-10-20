// java
package com.example.carrentingtest.admin;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.carrentingtest.storage.StoragePaths;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UploadPaymentProofWorker extends Worker {
    private static final String TAG = "UploadProofWorker";
    public static final String UNIQUE_WORK = "pending_payment_proofs_work";

    public UploadPaymentProofWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        List<PendingProofStore.PendingProof> list = PendingProofStore.listAll(ctx);
        if (list.isEmpty()) {
            Log.i(TAG, "No pending proofs found.");
            return Result.success();
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Ensure we are authenticated (anonymous sign-in). If your storage rules require a specific auth,
        // replace this with the appropriate auth flow.
        try {
            if (auth.getCurrentUser() == null) {
                Log.i(TAG, "Signing in anonymously for worker.");
                Tasks.await(auth.signInAnonymously());
                if (auth.getCurrentUser() == null) {
                    Log.w(TAG, "Anonymous sign-in returned null user.");
                } else {
                    Log.i(TAG, "Worker signed in as uid=" + auth.getCurrentUser().getUid());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to sign in anonymously in worker. Will retry.", e);
            return Result.retry();
        }

        boolean anyFailed = false;

        for (PendingProofStore.PendingProof p : list) {
            File f = new File(p.filePath);
            if (!f.exists()) {
                Log.w(TAG, "Pending file missing, removing entry: " + p.filePath);
                PendingProofStore.remove(ctx, p.filePath);
                continue;
            }

            try {
                Uri fileUri = Uri.fromFile(f);
                StorageReference ref = storage.getReference().child(StoragePaths.paymentProofPath(p.requestId));
                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setContentType(p.mime == null ? "image/jpeg" : p.mime)
                        .build();

                Log.i(TAG, "Uploading pending proof for requestId=" + p.requestId + " file=" + p.filePath);
                // upload and wait
                Tasks.await(ref.putFile(fileUri, metadata));
                Uri download = Tasks.await(ref.getDownloadUrl());
                Log.i(TAG, "Upload succeeded, downloadUrl=" + download);

                // update Firestore
                Tasks.await(db.collection("rental_requests").document(p.requestId)
                        .update("paymentProofUrl", download.toString()));
                Log.i(TAG, "Firestore updated for requestId=" + p.requestId);

                // success: remove pending and delete file
                PendingProofStore.remove(ctx, p.filePath);
                if (!f.delete()) {
                    Log.w(TAG, "Failed to delete local pending file: " + p.filePath);
                } else {
                    Log.i(TAG, "Deleted local pending file: " + p.filePath);
                }
            } catch (Exception e) {
                // log per-item failure and continue to attempt others; mark anyFailed to trigger retry
                Log.e(TAG, "Failed to upload pending proof: " + p.filePath + " requestId=" + p.requestId, e);
                anyFailed = true;
            }
        }

        if (anyFailed) {
            Log.i(TAG, "Some pending uploads failed, scheduling retry.");
            return Result.retry();
        } else {
            Log.i(TAG, "All pending uploads processed successfully.");
            return Result.success();
        }
    }

    public static void enqueueWork(Context ctx) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(UploadPaymentProofWorker.class)
                .setConstraints(constraints)
                .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();

        WorkManager.getInstance(ctx).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.KEEP, req);
        Log.i(TAG, "Enqueued UploadPaymentProofWorker (" + UNIQUE_WORK + ")");
    }
}