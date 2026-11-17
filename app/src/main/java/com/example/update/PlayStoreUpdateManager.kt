package com.example.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UpdateUiState {
    data object Idle : UpdateUiState()
    data object Checking : UpdateUiState()
    data class UpdateAvailable(
        val availableVersionCode: Int,
        val stalenessDays: Int?,
        val isImmediatePreferred: Boolean,
        val appUpdateInfo: AppUpdateInfo
    ) : UpdateUiState()

    data class Downloading(val progressPercent: Int) : UpdateUiState()
    data object Downloaded : UpdateUiState()
    data object UpToDate : UpdateUiState()
    data object Unavailable : UpdateUiState()
}

class PlayStoreUpdateManager(context: Context) {

    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(context.applicationContext)

    private val _updateState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val updateState: StateFlow<UpdateUiState> = _updateState.asStateFlow()

    private val _isBannerDismissed = MutableStateFlow(false)
    val isBannerDismissed: StateFlow<Boolean> = _isBannerDismissed.asStateFlow()

    private var isListenerRegistered = false

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                val downloaded = state.bytesDownloaded()
                val percent = if (total > 0L) {
                    ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                _updateState.value = UpdateUiState.Downloading(percent)
            }

            InstallStatus.DOWNLOADED -> {
                _isBannerDismissed.value = false
                _updateState.value = UpdateUiState.Downloaded
            }

            InstallStatus.CANCELED,
            InstallStatus.FAILED -> {
                _updateState.value = UpdateUiState.Idle
            }

            InstallStatus.INSTALLED -> {
                _updateState.value = UpdateUiState.UpToDate
            }

            else -> Unit
        }
    }

    fun registerListener() {
        if (!isListenerRegistered) {
            appUpdateManager.registerListener(installStateUpdatedListener)
            isListenerRegistered = true
        }
    }

    fun unregisterListener() {
        if (isListenerRegistered) {
            appUpdateManager.unregisterListener(installStateUpdatedListener)
            isListenerRegistered = false
        }
    }

    fun checkForUpdates(
        isManualCheck: Boolean = false,
        onResumeImmediateIfNeeded: ((AppUpdateInfo) -> Unit)? = null
    ) {
        if (isManualCheck) {
            _isBannerDismissed.value = false
            _updateState.value = UpdateUiState.Checking
        }

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                when {
                    appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                        _isBannerDismissed.value = false
                        _updateState.value = UpdateUiState.Downloaded
                    }

                    appUpdateInfo.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                            onResumeImmediateIfNeeded?.invoke(appUpdateInfo)
                        }
                    }

                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        val immediatePreferred = appUpdateInfo.updatePriority() >= HIGH_PRIORITY_THRESHOLD &&
                            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        val flexibleAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                        val immediateAllowed = appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)

                        if (flexibleAllowed || immediateAllowed) {
                            _updateState.value = UpdateUiState.UpdateAvailable(
                                availableVersionCode = appUpdateInfo.availableVersionCode(),
                                stalenessDays = appUpdateInfo.clientVersionStalenessDays(),
                                isImmediatePreferred = immediatePreferred,
                                appUpdateInfo = appUpdateInfo
                            )
                        } else if (isManualCheck) {
                            _updateState.value = UpdateUiState.UpToDate
                        }
                    }

                    else -> {
                        if (isManualCheck) {
                            _updateState.value = UpdateUiState.UpToDate
                        } else if (_updateState.value is UpdateUiState.UpdateAvailable) {
                            _updateState.value = UpdateUiState.Idle
                        }
                    }
                }
            }
            .addOnFailureListener {
                // In debug/sideloaded builds or devices without Play Store, fail silently on auto-check.
                if (isManualCheck) {
                    _updateState.value = UpdateUiState.Unavailable
                }
            }
    }

    fun startUpdate(
        appUpdateInfo: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        preferImmediate: Boolean = false
    ) {
        val updateType = when {
            preferImmediate && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                AppUpdateType.IMMEDIATE
            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ->
                AppUpdateType.FLEXIBLE
            appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ->
                AppUpdateType.IMMEDIATE
            else -> return
        }

        try {
            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (_: Exception) {
            _updateState.value = UpdateUiState.Unavailable
        }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun dismissBanner() {
        _isBannerDismissed.value = true
    }

    fun clearManualStatus() {
        if (_updateState.value is UpdateUiState.UpToDate || _updateState.value is UpdateUiState.Unavailable) {
            _updateState.value = UpdateUiState.Idle
        }
    }

    companion object {
        private const val HIGH_PRIORITY_THRESHOLD = 4
    }
}
