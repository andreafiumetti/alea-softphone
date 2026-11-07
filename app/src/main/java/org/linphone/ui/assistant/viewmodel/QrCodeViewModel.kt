/*
 * Copyright (c) 2010-2023 Belledonne Communications SARL.
 * ALEA: branch enroll /join/{token} sul QR scanner.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.ui.assistant.viewmodel

import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.alea.AleaEnrollClient
import org.linphone.alea.AleaEnrollStore
import org.linphone.alea.AleaJoinLink
import org.linphone.alea.AleaSipAccountInstaller
import org.linphone.core.ConfiguringState
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.GlobalState
import org.linphone.core.tools.Log
import org.linphone.ui.GenericViewModel
import org.linphone.utils.Event
import org.linphone.utils.LinphoneUtils

class QrCodeViewModel
    @UiThread
    constructor() : GenericViewModel() {
    companion object {
        private const val TAG = "[Qr Code Scanner ViewModel]"
    }

    val remoteProvisioningSuccessfulEvent = MutableLiveData<Event<Boolean>>()

    val onErrorEvent = MutableLiveData<Event<Boolean>>()

    private val enrollExecutor = Executors.newSingleThreadExecutor()

    private val coreListener = object : CoreListenerStub() {
        @WorkerThread
        override fun onConfiguringStatus(core: Core, status: ConfiguringState, message: String?) {
            Log.i("$TAG Configuring state is [$status]")
            if (status == ConfiguringState.Failed) {
                Log.e("$TAG Failure applying remote provisioning: $message")
                showRedToast(R.string.remote_provisioning_config_failed_toast, R.drawable.warning_circle)
                onErrorEvent.postValue(Event(true))
            }
        }

        @WorkerThread
        override fun onGlobalStateChanged(core: Core, state: GlobalState?, message: String) {
            if (state == GlobalState.On) {
                if (core.accountList.isEmpty()) {
                    Log.w("$TAG Provisioning was successful but no account has been configured yet, staying in assistant")
                    remoteProvisioningSuccessfulEvent.postValue(Event(false))
                } else {
                    Log.i("$TAG At least an account exists in Core, leaving assistant")
                    remoteProvisioningSuccessfulEvent.postValue(Event(true))
                }
            }
        }

        @WorkerThread
        override fun onQrcodeFound(core: Core, result: String?) {
            Log.i("$TAG QR Code found: [$result]")
            if (result == null) {
                showRedToast(R.string.assistant_qr_code_invalid_toast, R.drawable.warning_circle)
                return
            }

            val alea = AleaJoinLink.parse(result)
            if (alea != null) {
                Log.i("$TAG ALEA join link detected api=${alea.apiBase}")
                core.nativePreviewWindowId = null
                core.isVideoPreviewEnabled = false
                core.isQrcodeVideoPreviewEnabled = false
                startAleaEnroll(alea.apiBase, alea.token)
                return
            }

            val url = LinphoneUtils.getRemoteProvisioningUrlFromUri(result)
            if (url == null) {
                Log.e("$TAG The content of the QR Code [$result] doesn't seem to be a valid web URL")
                showRedToast(R.string.assistant_qr_code_invalid_toast, R.drawable.warning_circle)
                return
            }

            Log.i(
                "$TAG Setting QR code URL [$url], restarting the Core outside of iterate() loop to apply configuration changes"
            )
            core.nativePreviewWindowId = null
            core.isVideoPreviewEnabled = false
            core.isQrcodeVideoPreviewEnabled = false
            core.provisioningUri = url

            coreContext.postOnCoreThread { c ->
                Log.i("$TAG Stopping Core")
                c.stop()
                Log.i("$TAG Core has been stopped, restarting it")
                c.start()
                Log.i("$TAG Core has been restarted")
            }
        }
    }

    init {
        coreContext.postOnCoreThread { core ->
            core.addListener(coreListener)

            val coreGlobalState = core.globalState
            if (coreGlobalState != GlobalState.On) {
                Log.e("$TAG Core isn't ON (current state is [$coreGlobalState]), video preview won't work!")
            }
        }
    }

    @UiThread
    override fun onCleared() {
        enrollExecutor.shutdownNow()
        coreContext.postOnCoreThread { core ->
            core.removeListener(coreListener)
        }
        super.onCleared()
    }

    private fun startAleaEnroll(apiBase: String, token: String) {
        enrollExecutor.execute {
            try {
                val result = AleaEnrollClient.enroll(apiBase, token)
                AleaEnrollStore.save(coreContext.context, result)
                coreContext.postOnCoreThread { core ->
                    when (val out = AleaSipAccountInstaller.install(core, result)) {
                        is AleaSipAccountInstaller.Outcome.Ok -> {
                            Log.i("$TAG ALEA account ready, leaving assistant")
                            org.linphone.alea.AleaDirectorySync.syncIfNeeded(coreContext.context)
                            org.linphone.alea.AleaPushRegistrar.registerAsync(coreContext.context)
                            showGreenToast(
                                R.string.alea_enroll_success_toast,
                                R.drawable.check
                            )
                            remoteProvisioningSuccessfulEvent.postValue(Event(true))
                        }
                        is AleaSipAccountInstaller.Outcome.Error -> {
                            Log.e("$TAG ALEA install failed: ${out.message}")
                            showRedToast(
                                R.string.alea_enroll_failed_toast,
                                R.drawable.warning_circle
                            )
                            onErrorEvent.postValue(Event(true))
                        }
                    }
                }
            } catch (e: AleaEnrollClient.EnrollException) {
                Log.e("$TAG ALEA enroll failed: ${e.message}")
                showRedToast(R.string.alea_enroll_failed_toast, R.drawable.warning_circle)
                onErrorEvent.postValue(Event(true))
            } catch (e: Exception) {
                Log.e("$TAG ALEA enroll error: ${e.message}")
                showRedToast(R.string.alea_enroll_failed_toast, R.drawable.warning_circle)
                onErrorEvent.postValue(Event(true))
            }
        }
    }

    @UiThread
    fun setBackCamera() {
        coreContext.postOnCoreThread { core ->
            Log.i("$TAG Reloading video devices, might be needed if CAMERA permission was granted very recently")
            core.reloadVideoDevices()

            if (!coreContext.setBackCamera()) {
                for (camera in core.videoDevicesList) {
                    if (camera != "StaticImage: Static picture") {
                        Log.w("$TAG No back facing camera found, using first one available [$camera]")
                        coreContext.core.videoDevice = camera
                        return@postOnCoreThread
                    }
                }

                Log.e("$TAG No camera device found!")
            }
        }
    }
}
