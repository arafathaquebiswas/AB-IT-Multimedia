package com.abit.multimedia.broadcast.battery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

/**
 * Gives the battery screen (Activity 2, battery option) its live battery status.
 * Because it is a ViewModel, the same LiveData survives rotation.
 */
class BatteryViewModel(application: Application) : AndroidViewModel(application) {

    /** Battery updates; the receiver is only registered while this is observed by a visible screen. */
    val batteryStatus: LiveData<BatteryStatus> = BatteryStatusLiveData(application)
}
