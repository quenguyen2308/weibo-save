package com.weibosave.util

import android.content.Context

object FolderPreference {
    private const val PREFS_NAME = "weibosave_prefs"
    private const val KEY_FOLDER_URI = "save_folder_uri"

    fun getFolderUri(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FOLDER_URI, null)

    fun setFolderUri(context: Context, uriString: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_FOLDER_URI, uriString).apply()
    }

    fun clearFolderUri(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_FOLDER_URI).apply()
    }
}
