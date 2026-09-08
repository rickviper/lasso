package com.ullas.circlesearch

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Serves app-private captures/crops for the share sheet without publishing
 * anything to MediaStore (which would make them Gallery-visible). Read-only,
 * single-file, path-traversal guarded. No dependency, no permission.
 */
class CropProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.ullas.circlesearch.crops"
        private const val FILES = 1
        private val MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "*", FILES)
        }

        fun uriFor(name: String): String = "content://$AUTHORITY/$name"
    }

    override fun onCreate(): Boolean = true

    private fun fileFor(uri: Uri): File {
        if (MATCHER.match(uri) != FILES) throw FileNotFoundException("bad uri: $uri")
        val name = uri.lastPathSegment ?: throw FileNotFoundException("bad uri: $uri")
        require(name.matches(Regex("[A-Za-z0-9_.-]+")) && !name.contains("..")) {
            "rejected filename: $name"
        }
        val dir = File(context!!.filesDir, "captures")
        val file = File(dir, name).canonicalFile
        require(file.path.startsWith(dir.canonicalPath + File.separator)) {
            "escaped directory: $name"
        }
        if (!file.isFile) throw FileNotFoundException("missing: $name")
        return file
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (mode != "r") throw FileNotFoundException("read-only: $uri")
        return ParcelFileDescriptor.open(
            fileFor(uri), ParcelFileDescriptor.MODE_READ_ONLY
        )
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val file = try {
            fileFor(uri)
        } catch (e: Exception) {
            return null
        }
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            addRow(arrayOf(file.name, file.length()))
        }
    }

    override fun getType(uri: Uri): String = "image/png"

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int =
        throw UnsupportedOperationException("read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = throw UnsupportedOperationException("read-only")
}
