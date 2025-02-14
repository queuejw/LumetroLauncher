package ru.queuejw.mpl.content.settings.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.queuejw.mpl.Application.Companion.PREFS
import ru.queuejw.mpl.Application.Companion.customFont
import ru.queuejw.mpl.Application.Companion.isUpdateDownloading
import ru.queuejw.mpl.BuildConfig
import ru.queuejw.mpl.R
import ru.queuejw.mpl.content.data.bsod.BSOD
import ru.queuejw.mpl.content.settings.SettingsActivity
import ru.queuejw.mpl.databinding.SettingsUpdatesBinding
import ru.queuejw.mpl.helpers.ui.WPDialog
import ru.queuejw.mpl.helpers.update.UpdateDataParser
import ru.queuejw.mpl.helpers.update.UpdateWorker
import ru.queuejw.mpl.helpers.utils.Utils
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class UpdateSettingsFragment : Fragment() {

    private var _binding: SettingsUpdatesBinding? = null
    private val binding get() = _binding!!

    private var db: BSOD? = null
    private var manager: DownloadManager? = null
    private var downloadId: Long? = null

    private var mainDispatcher = Dispatchers.Main

    private var fragmentActive = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsUpdatesBinding.inflate(inflater, container, false)
        (requireActivity() as SettingsActivity).setText(getString(R.string.launcher_update))
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setupFont()
        setOnClickers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        fragmentActive = false
    }

    override fun onResume() {
        super.onResume()
        fragmentActive = true
        refreshUi()
    }

    private fun init() {
        db = BSOD.getData(requireActivity())
    }

    private fun setupFont() {
        customFont?.let {
            binding.updateStatus.typeface = it
            binding.checkingUpdatesSub.typeface = it
            binding.progessText.typeface = it
            binding.updateInfo.typeface = it
            binding.cancelButton.typeface = it
            binding.checkForUpdatesBtn.typeface = it
            binding.UpdateNotifyCheckBox.typeface = it
            binding.AutoUpdateCheckBox.typeface = it

        }
    }

    private fun setOnClickers() {
        binding.AutoUpdateCheckBox.setOnCheckedChangeListener { _, isChecked ->
            PREFS.isAutoUpdateEnabled = isChecked
            refreshUi()
        }
        binding.UpdateNotifyCheckBox.setOnCheckedChangeListener { _, isChecked ->
            PREFS.isUpdateNotificationEnabled = isChecked
            if (isChecked) UpdateWorker.scheduleWork(requireActivity()) else UpdateWorker.stopWork(
                requireActivity()
            )
            refreshUi()
        }
        binding.updateInfo.setOnClickListener {
            WPDialog(requireActivity()).setTopDialog(true)
                .setTitle(getString(R.string.details))
                .setMessage(getUpdateMessage())
                .setPositiveButton(getString(android.R.string.ok), null).show()
        }
        binding.checkForUpdatesBtn.setOnClickListener {
            if (!Utils.checkStoragePermissions(requireActivity())) {
                PREFS.updateState = 5
                refreshUi()
                showPermsDialog()
                return@setOnClickListener
            }
            when (PREFS.updateState) {
                4 -> {
                    try {
                        val file = File(
                            Environment.getExternalStorageDirectory().toString() + "/Download/",
                            "MPL_update.apk"
                        )
                        val uri = FileProvider.getUriForFile(
                            requireActivity(),
                            requireActivity().packageName + ".provider",
                            file
                        )
                        PREFS.prefs.edit { putBoolean("updateInstalled", true) }
                        openFile(uri, requireActivity())
                    } catch (e: Exception) {
                        Log.i("InstallAPK", "error: $e")
                        PREFS.updateState = 5
                        refreshUi()
                        Utils.saveError(e.toString(), db!!)
                    }
                    return@setOnClickListener
                }

                6, 7 -> {
                    checkDownload()
                }

                else -> {
                    PREFS.updateState = 1
                    refreshUi()
                    checkForUpdates()
                }
            }
        }
        binding.cancelButton.setOnClickListener {
            val ver = if (UpdateDataParser.verCode == null) {
                PREFS.versionCode
            } else {
                UpdateDataParser.verCode
            }
            if (ver == Utils.VERSION_CODE) {
                PREFS.updateState = 3
            } else {
                PREFS.updateState = 0
            }
            if (PREFS.updateState == 1) {
                return@setOnClickListener
            }
            isUpdateDownloading = false
            manager?.remove(downloadId!!)
            deleteUpdateFile(requireActivity())
            refreshUi()
        }
        if (PREFS.prefs.getBoolean(
                "permsDialogUpdateScreenEnabled",
                true
            ) && !Utils.checkStoragePermissions(requireActivity())
        ) showPermsDialog()
    }

    private fun showPermsDialog() {
        val dialog = WPDialog(requireActivity()).setTopDialog(true)
            .setTitle(getString(R.string.perms_req))
            .setCancelable(true)
            .setMessage(getString(R.string.perms_req_tip))
        dialog.setNegativeButton(getString(R.string.yes)) {
            getPermission()
            WPDialog(requireActivity()).dismiss()
            dialog.dismiss()
        }
            .setNeutralButton(getString(R.string.hide)) {
                hideDialogForever()
                dialog.dismiss()
            }
            .setPositiveButton(getString(R.string.no)) {
                dialog.dismiss()
            }
        dialog.show()
    }

    private fun hideDialogForever() {
        PREFS.prefs.edit { putBoolean("permsDialogUpdateScreenEnabled", false) }
    }

    private fun getUpdateMessage(): String {
        return if (UpdateDataParser.updateMsg == null) {
            PREFS.updateMessage
        } else {
            UpdateDataParser.updateMsg!!
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).setData(
                String.format("package:%s", requireActivity().packageName).toUri()
            )
            startActivity(intent)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                1507
            )
        }
    }

    private fun refreshUi() {
        if(!fragmentActive) {
            return
        }
        binding.AutoUpdateCheckBox.apply {
            isChecked = PREFS.isAutoUpdateEnabled
            isEnabled = PREFS.isUpdateNotificationEnabled
        }
        binding.UpdateNotifyCheckBox.isChecked = PREFS.isUpdateNotificationEnabled
        when (PREFS.updateState) {
            1 -> {
                //checking for updates state
                binding.checkForUpdatesBtn.visibility = View.GONE
                binding.checkingUpdatesSub.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.checking_for_updates)
                }
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.GONE
                binding.cancelButton.visibility = View.VISIBLE
            }

            2 -> {
                // dowloading state
                binding.checkingUpdatesSub.visibility = View.GONE
                binding.checkForUpdatesBtn.visibility = View.GONE
                binding.updateIndicator.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.VISIBLE
                val progressString = if (isUpdateDownloading) {
                    binding.progress.progress = PREFS.updateProgressLevel
                    getString(R.string.preparing_to_install, PREFS.updateProgressLevel) + "%"
                } else {
                    getString(R.string.preparing_to_install, 0) + "%"
                }
                binding.progessText.text = progressString
                binding.updateInfo.visibility = View.GONE
            }

            3 -> {
                // up to date
                binding.checkingUpdatesSub.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.up_to_date)
                }
                binding.checkForUpdatesBtn.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.check_for_updates)
                }
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.GONE
                binding.cancelButton.visibility = View.GONE
            }

            4 -> {
                // ready to install
                binding.checkingUpdatesSub.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.ready_to_install)
                }
                binding.checkForUpdatesBtn.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.install)
                }
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.VISIBLE
            }

            5 -> {
                // error
                binding.checkingUpdatesSub.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.update_failed)
                }
                binding.checkForUpdatesBtn.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.retry)
                }
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.GONE
                binding.cancelButton.visibility = View.GONE
            }

            6 -> {
                // ready for download
                binding.checkingUpdatesSub.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.ready_to_download)
                }
                binding.checkForUpdatesBtn.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.download)
                }
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.VISIBLE
                binding.cancelButton.visibility = View.VISIBLE
            }

            8 -> {
                // current version is newer
                binding.checkingUpdatesSub.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.update_failed_version_bigger_than_server)
                }
                binding.checkForUpdatesBtn.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.retry)
                }
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.GONE
                binding.cancelButton.visibility = View.GONE
            }

            0 -> {
                // default
                binding.checkForUpdatesBtn.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.check_for_updates)
                }
                binding.checkingUpdatesSub.visibility = View.GONE
                binding.updateIndicator.visibility = View.GONE
                binding.updateInfo.visibility = View.GONE
                binding.cancelButton.visibility = View.GONE
            }
        }
        if (!BuildConfig.UPDATES_ACITVE) {
            binding.checkForUpdatesBtn.visibility = View.GONE
            binding.checkingUpdatesSub.apply {
                visibility = View.VISIBLE
                text = getString(R.string.updates_disabled)
            }
        }
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                downloadXmlActivity()
                withContext(mainDispatcher) {
                    refreshUi()
                    checkUpdateInfo()
                }
            } catch (e: Exception) {
                Log.e("CheckForUpdates", e.toString())
                Utils.saveError(e.toString(), db!!)
                withContext(mainDispatcher) {
                    refreshUi()
                }
            } finally {
                cancel()
            }
        }
    }

    private fun checkUpdateInfo() {
        CoroutineScope(Dispatchers.IO).launch {
            if (UpdateDataParser.verCode == null) {
                PREFS.updateState = 5
                return@launch
            }
            if (UpdateDataParser.verCode == Utils.VERSION_CODE) {
                PREFS.updateState = 3
            } else if (Utils.VERSION_CODE > UpdateDataParser.verCode!!) {
                PREFS.updateState = 8
            } else if (UpdateDataParser.verCode!! > Utils.VERSION_CODE) {
                PREFS.updateState = 6
            }
            withContext(mainDispatcher) {
                if(fragmentActive) {
                    binding.progress.isIndeterminate = false
                    refreshUi()
                }
            }
            cancel()
        }
    }

    private fun checkDownload() {
        Log.i("CheckForUpdates", "download release")
        downloadFile("MPL", URL_RELEASE_FILE)
    }

    @SuppressLint("Range")
    private fun downloadFile(fileName: String, url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                deleteUpdateFile(requireActivity())
            } catch (e: IOException) {
                Utils.saveError(e.toString(), db!!)
                PREFS.updateState = 5
                withContext(mainDispatcher) {
                    if(fragmentActive) {
                        refreshUi()
                        WPDialog(requireActivity()).setTopDialog(true)
                            .setTitle(getString(R.string.error))
                            .setMessage(getString(R.string.downloading_error))
                            .setPositiveButton(getString(android.R.string.ok), null).show()
                    }
                }
                cancel()
                return@launch
            }
            try {
                val request = DownloadManager.Request(url.toUri())
                request.setDescription(getString(R.string.update_notification))
                request.setTitle(fileName)
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "MPL_V80.apk"
                )
                manager = requireActivity().getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                downloadId = manager?.enqueue(request)
                isUpdateDownloading = true
                PREFS.updateState = 2
                withContext(mainDispatcher) {
                    refreshUi()
                }
                val q = DownloadManager.Query()
                q.setFilterById(downloadId!!)
                var cursor: Cursor?
                while (isUpdateDownloading) {
                    cursor = manager!!.query(q)
                    if (cursor != null && cursor.moveToFirst()) {
                        val downloaded =
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total =
                            cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val progress: Int = ((downloaded * 100L / total)).toInt()
                        val progressString =
                            getString(R.string.preparing_to_install, progress) + "%"
                        PREFS.updateProgressLevel = progress
                        withContext(mainDispatcher) {
                            if(fragmentActive) {
                                binding.progessText.text = progressString
                                binding.progress.setProgress(progress, true)
                            }
                        }
                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                            isUpdateDownloading = false
                            PREFS.updateState = 4
                            withContext(mainDispatcher) {
                                refreshUi()
                            }
                        }
                        if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_FAILED) {
                            isUpdateDownloading = false
                            PREFS.updateState = 5
                            withContext(mainDispatcher) {
                                refreshUi()
                            }
                        }
                        cursor.close()
                    } else {
                        cursor.close()
                        isUpdateDownloading = false
                        PREFS.updateState = 0
                        withContext(mainDispatcher) {
                            requireActivity().recreate()
                        }
                    }
                }
            } catch (e: Exception) {
                Utils.saveError(e.toString(), db!!)
                if (downloadId != null) {
                    manager?.remove(downloadId!!)
                }
                isUpdateDownloading = false
                PREFS.updateState = 5
                withContext(mainDispatcher) {
                    refreshUi()
                    if(fragmentActive) {
                    WPDialog(requireActivity()).setTopDialog(true)
                        .setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.downloading_error))
                        .setPositiveButton(getString(android.R.string.ok), null).show()
                        }
                }
            } finally {
                cancel()
            }
        }
    }

    private suspend fun downloadXmlActivity() {
        Log.i("CheckForUpdates", "download xml")
        val url = URL(URL)
        val connection = withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpURLConnection
        connection.connectTimeout = 15000
        try {
            val input = connection.inputStream
            val parser = UpdateDataParser()
            parser.parse(input)
            withContext(Dispatchers.IO) {
                input.close()
            }
        } catch (e: Exception) {
            Log.e("CheckForUpdates", "something went wrong: $e")
            PREFS.updateState = 5
            withContext(mainDispatcher) {
                refreshUi()
            }
        }
    }

    companion object {
        const val URL: String =
            "https://github.com/queuejw/mpl_updates/releases/download/release/update.xml"
        const val URL_RELEASE_FILE: String =
            "https://github.com/queuejw/mpl_updates/releases/download/release/MPL_V80.apk"

        fun downloadXml(link: String) {
            Log.i("CheckForUpdates", "download xml")
            val url = URL(link)
            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                val input = connection.inputStream
                val parser = UpdateDataParser()
                parser.parse(input)
                input.close()
            } catch (e: Exception) {
                Log.e("CheckForUpdates", "something went wrong: $e")
            }
        }

        fun isUpdateAvailable(): Boolean {
            if (UpdateDataParser.verCode == null || Utils.VERSION_CODE > UpdateDataParser.verCode!! || !BuildConfig.UPDATES_ACITVE) {
                return false
            }
            val boolean: Boolean = if (UpdateDataParser.verCode == Utils.VERSION_CODE) {
                Log.i("CheckForUpdates", "up-to-date")
                false
            } else {
                Log.i("CheckForUpdates", "Update Available")
                true
            }
            return boolean
        }

        fun openFile(fileUri: Uri, activity: Activity) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(fileUri, activity.contentResolver.getType(fileUri))
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e("installAPK", e.toString())
            }
        }

        fun deleteUpdateFile(context: Context) {
            try {
                val file = File(
                    Environment.getExternalStorageDirectory().toString() + "/Download/",
                    "MPL_V80.apk"
                )
                val uri =
                    FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                context.contentResolver.delete(uri, null, null)
            } catch (e: IOException) {
                Log.e("Update", e.toString())
            }
        }
    }
}