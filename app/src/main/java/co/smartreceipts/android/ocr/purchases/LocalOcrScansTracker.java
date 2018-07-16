package co.smartreceipts.android.ocr.purchases;

import android.content.SharedPreferences;
import android.support.annotation.NonNull;

import com.google.common.base.Preconditions;

import org.reactivestreams.Subscriber;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import co.smartreceipts.android.di.scopes.ApplicationScope;
import co.smartreceipts.android.utils.log.Logger;
import dagger.Lazy;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

@ApplicationScope
public class LocalOcrScansTracker {

    private static final String KEY_AVAILABLE_SCANS = "key_int_available_ocr_scans";

    private final Lazy<SharedPreferences> sharedPreferences;
    private final BehaviorSubject<Integer> remainingScansSubject;
    private final AtomicBoolean haveWeCalledTheLocalScansStreamYet = new AtomicBoolean(false);

    @Inject
    public LocalOcrScansTracker(@NonNull Lazy<SharedPreferences> sharedPreferences) {
        this.sharedPreferences = Preconditions.checkNotNull(sharedPreferences);
        this.remainingScansSubject = BehaviorSubject.create();
    }

    /**
     * @return the remaining Ocr scan count that is allowed for this user. Please note that is
     * this not the authority for this (ie it's not the server), this may not be fully accurate, so we
     * may still get a remote error after a scan. Additionally, please note that this {@link Observable}
     * will only call {@link Subscriber#onNext(Object)} with the latest value (and never onComplete or
     * onError) to allow us to continually get the updated value
     */
    @NonNull
    public Observable<Integer> getRemainingScansStream() {
        if (!haveWeCalledTheLocalScansStreamYet.getAndSet(true)) {
            // The first time we call this method, supply the remaining count to it
            remainingScansSubject.onNext(getRemainingScans());
        }
        return remainingScansSubject;
    }

    /**
     * @return the locally tracked (ie possibly inaccurate) remaining scans count
     */
    public int getRemainingScans() {
        return sharedPreferences.get().getInt(KEY_AVAILABLE_SCANS, 0);
    }

    public void setRemainingScans(int remainingScans) {
        Logger.info(this, "Setting scans remaining as {}.", remainingScans);
        sharedPreferences.get().edit().putInt(KEY_AVAILABLE_SCANS, remainingScans).apply();
        remainingScansSubject.onNext(remainingScans);
    }

    public void decrementRemainingScans() {
        if (getRemainingScans() > 0) {
            sharedPreferences.get().edit().putInt(KEY_AVAILABLE_SCANS, getRemainingScans() - 1).apply();
            remainingScansSubject.onNext(getRemainingScans());
        }
    }
}
