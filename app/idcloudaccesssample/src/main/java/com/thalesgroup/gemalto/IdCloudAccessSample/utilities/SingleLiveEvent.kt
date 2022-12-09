package com.thalesgroup.gemalto.IdCloudAccessSample.utilities

import android.util.Log
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import java.util.concurrent.atomic.AtomicBoolean

// MutableLiveData holds last value it is set with. When we go back to previous fragment,
// our LiveData observes are notified again with existing values.
// This is to preserve the state of your fragment and that's the exact purpose of LiveData.
// So, this class prevents the observer to be called multiple times and only be called when there is some data change inside it .

class SingleLiveEvent<T> : MutableLiveData<T>() {

    private val mPending = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {

        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(
            owner,
            object : Observer<T> {
                override fun onChanged(t: T?) {
                    if (mPending.compareAndSet(true, false)) {
                        observer.onChanged(t)
                    }
                }
            }
        )
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        setValue(null)
    }

    companion object {

        private val TAG = "SingleLiveEvent"
    }
}
