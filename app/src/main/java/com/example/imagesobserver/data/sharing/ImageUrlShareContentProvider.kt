package com.example.imagesobserver.data.sharing

import android.content.ContentProvider
import android.content.ContentUris
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.imagesobserver.constants.ProjectConstants
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Exposes a registered HTTP(S) image URL as [content://] [ProjectConstants.MIME_TEXT_PLAIN]
 * for [android.content.Intent.ACTION_SEND] (grantUriPermissions on the chooser intent).
 */
class ImageUrlShareContentProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let(::installUriMatcher)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? =
        when (uriMatcher.match(uri)) {
            MATCH_SHARE -> ProjectConstants.MIME_TEXT_PLAIN
            else -> null
        }

    override fun insert(uri: Uri, values: android.content.ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (uriMatcher.match(uri) != MATCH_SHARE) {
            throw FileNotFoundException(
                ProjectConstants.IMAGE_URL_SHARE_UNSUPPORTED_URI_FORMAT.format(uri),
            )
        }
        val id = ContentUris.parseId(uri)
        val url = resolveUrl(id)
            ?: throw FileNotFoundException(
                ProjectConstants.IMAGE_URL_SHARE_UNKNOWN_ID_FORMAT.format(id),
            )
        return openTextPipe(url)
    }

    private fun openTextPipe(text: String): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val output = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
        Thread {
            output.use { stream ->
                stream.write(text.toByteArray(StandardCharsets.UTF_8))
            }
        }.start()
        return pipe[0]
    }

    companion object {
        private const val MATCH_SHARE = 1

        private val urlRegistry = ImageUrlShareRegistry()

        private lateinit var uriMatcher: UriMatcher

        fun registerUrl(context: Context, url: String): Uri {
            val id = urlRegistry.register(url)
            return ContentUris.withAppendedId(baseUri(context), id)
        }

        fun resolveUrl(id: Long): String? = urlRegistry.getUrl(id)

        fun baseUri(context: Context): Uri =
            Uri.Builder()
                .scheme(ProjectConstants.CONTENT_URI_SCHEME)
                .authority(authority(context))
                .appendPath(ProjectConstants.SHARE_PATH)
                .build()

        fun authority(context: Context): String =
            context.packageName + ProjectConstants.IMAGE_URL_SHARE_AUTHORITY_SUFFIX

        fun installUriMatcher(context: Context) {
            uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
                addURI(
                    authority(context),
                    "${ProjectConstants.SHARE_PATH}/${ProjectConstants.IMAGE_URL_SHARE_URI_ID_WILDCARD}",
                    MATCH_SHARE,
                )
            }
        }
    }

    override fun attachInfo(context: Context?, info: android.content.pm.ProviderInfo?) {
        super.attachInfo(context, info)
        context?.let(::installUriMatcher)
    }
}

/** In-memory URL registry for short-lived share [content://] URIs. */
private class ImageUrlShareRegistry {

    private val nextId = AtomicLong(0L)
    private val urls = ConcurrentHashMap<Long, String>()

    fun register(url: String): Long {
        val id = nextId.incrementAndGet()
        urls[id] = url
        return id
    }

    fun getUrl(id: Long): String? = urls[id]
}
