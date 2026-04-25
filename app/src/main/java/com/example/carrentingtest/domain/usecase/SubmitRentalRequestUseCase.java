package com.example.carrentingtest.domain.usecase;

import androidx.annotation.NonNull;

import com.example.carrentingtest.data.repository.RentalRequestRepository;
import com.example.carrentingtest.data.repository.UserRepository;
import com.example.carrentingtest.domain.RentalRequestStatus;
import com.example.carrentingtest.models.Car;
import com.example.carrentingtest.models.RentalRequest;
import com.example.carrentingtest.pricing.PricingService;
import com.example.carrentingtest.verification.VerificationGuard;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Date;

public class SubmitRentalRequestUseCase {

    public static final String ERROR_VERIFICATION_REQUIRED = "verification_required";

    private final FirebaseAuth auth;
    private final UserRepository userRepository;
    private final RentalRequestRepository rentalRequestRepository;

    public SubmitRentalRequestUseCase() {
        this(FirebaseAuth.getInstance(), new UserRepository(), new RentalRequestRepository());
    }

    SubmitRentalRequestUseCase(@NonNull FirebaseAuth auth,
                               @NonNull UserRepository userRepository,
                               @NonNull RentalRequestRepository rentalRequestRepository) {
        this.auth = auth;
        this.userRepository = userRepository;
        this.rentalRequestRepository = rentalRequestRepository;
    }

    public Task<RentalRequest> execute(@NonNull Car selectedCar,
                                       @NonNull String companyId,
                                       @NonNull String additionalRequests,
                                       @NonNull Date startDate,
                                       @NonNull Date endDate) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new IllegalStateException("User not logged in."));
        }

        if (PricingService.quote(selectedCar, startDate, endDate) == null) {
            return Tasks.forException(new IllegalStateException("Unable to calculate pricing for this rental period."));
        }

        if (selectedCar.isMaintenance()) {
            return Tasks.forException(new IllegalStateException("This car is currently in maintenance."));
        }

        return userRepository.getById(currentUser.getUid())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(
                                task.getException() != null
                                        ? task.getException()
                                        : new IllegalStateException("Failed to retrieve user data."));
                    }
                    return buildAndPersistRequest(
                            currentUser,
                            task.getResult(),
                            selectedCar,
                            companyId,
                            additionalRequests,
                            startDate,
                            endDate);
                });
    }

    public static boolean isVerificationRequired(@NonNull Throwable throwable) {
        return ERROR_VERIFICATION_REQUIRED.equals(throwable.getMessage());
    }

    private Task<RentalRequest> buildAndPersistRequest(@NonNull FirebaseUser firebaseUser,
                                                       DocumentSnapshot userDocument,
                                                       @NonNull Car selectedCar,
                                                       @NonNull String companyId,
                                                       @NonNull String additionalRequests,
                                                       @NonNull Date startDate,
                                                       @NonNull Date endDate) {
        if (userDocument == null || !userDocument.exists()) {
            return Tasks.forException(new IllegalStateException("Failed to retrieve user data."));
        }

        if (!VerificationGuard.canBook(userDocument.getString("verification_status"))) {
            return Tasks.forException(new IllegalStateException(ERROR_VERIFICATION_REQUIRED));
        }

        return rentalRequestRepository.getApprovedForCar(companyId, selectedCar.getDocumentId())
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(
                                task.getException() != null
                                        ? task.getException()
                                        : new IllegalStateException("Failed to check booking availability."));
                    }
                    if (task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            RentalRequest approvedRequest = document.toObject(RentalRequest.class);
                            if (approvedRequest != null && datesOverlap(
                                    startDate,
                                    endDate,
                                    approvedRequest.getStartDate(),
                                    approvedRequest.getEndDate())) {
                                return Tasks.forException(new IllegalStateException(
                                        "This car already has an approved booking for those dates."));
                            }
                        }
                    }
                    return persistRequest(
                            firebaseUser,
                            userDocument,
                            selectedCar,
                            companyId,
                            additionalRequests,
                            startDate,
                            endDate);
                });
    }

    private Task<RentalRequest> persistRequest(@NonNull FirebaseUser firebaseUser,
                                               @NonNull DocumentSnapshot userDocument,
                                               @NonNull Car selectedCar,
                                               @NonNull String companyId,
                                               @NonNull String additionalRequests,
                                               @NonNull Date startDate,
                                               @NonNull Date endDate) {
        RentalRequest request = new RentalRequest();
        request.setCarId(selectedCar.getDocumentId());
        request.setCarModel(selectedCar.getModel());
        request.setUserId(firebaseUser.getUid());
        request.setUserName(resolveUserName(firebaseUser, userDocument));
        request.setUserDriverLicense(userDocument.getString("driverLicense"));
        request.setUserPhone(resolveUserPhone(firebaseUser, userDocument));
        request.setAdditionalRequests(additionalRequests);
        request.setStatus(RentalRequestStatus.PENDING.getStorageValue());
        request.setCompanyId(companyId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        PricingService.applyPricing(request, PricingService.quote(selectedCar, startDate, endDate));

        return rentalRequestRepository.create(request)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        return Tasks.forException(
                                task.getException() != null
                                        ? task.getException()
                                        : new IllegalStateException("Failed to persist rental request."));
                    }
                    return Tasks.forResult(request);
                });
    }

    private boolean datesOverlap(@NonNull Date firstStart,
                                 @NonNull Date firstEnd,
                                 Date secondStart,
                                 Date secondEnd) {
        if (secondStart == null || secondEnd == null) {
            return false;
        }
        return firstStart.before(secondEnd) && secondStart.before(firstEnd);
    }

    private String resolveUserName(@NonNull FirebaseUser firebaseUser, @NonNull DocumentSnapshot userDocument) {
        String userName = userDocument.getString("name");
        return userName != null ? userName : firebaseUser.getEmail();
    }

    private String resolveUserPhone(@NonNull FirebaseUser firebaseUser, @NonNull DocumentSnapshot userDocument) {
        String userPhone = userDocument.getString("phone");
        if (userPhone == null || userPhone.trim().isEmpty()) {
            return firebaseUser.getPhoneNumber();
        }
        return userPhone;
    }
}
