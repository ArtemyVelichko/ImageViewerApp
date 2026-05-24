package com.example.imagesobserver.constants

/**
 * App-wide constants (non-secrets). Add new project-level values here as needed.
 */
object ProjectConstants {

    /**
     * When `true`, OkHttp logs full request/response bodies ([okhttp3.logging.HttpLoggingInterceptor])
     * and [com.example.imagesobserver.data.api.NetworkLogInterceptor] logs one-line summaries via Timber.
     * When `false`, network logging is off (default).
     */
    const val RETROFIT_DEBUG_ENABLED = false

    /** Max attempts for [com.example.imagesobserver.util.smartRetry] on the images list API. */
    const val SMART_RETRY_ATTEMPTS = 3

    /** Private cache file name for the remote images list ([android.content.Context.getFilesDir]). */
    const val REMOTE_IMAGES_LIST_CACHE_FILE_NAME = "images_manifest_cache.txt"

    /** Subdirectory under [android.content.Context.getFilesDir] for full-size cached images. */
    const val IMAGE_CACHE_ORIGINALS_DIR_NAME = "image_cache/originals"

    /** Subdirectory under [android.content.Context.getFilesDir] for grid thumbnail files. */
    const val IMAGE_CACHE_THUMBNAILS_DIR_NAME = "image_cache/thumbnails"

    const val IMAGE_CACHE_THUMBNAIL_JPEG_QUALITY = 85

    /** Max total on-disk size for originals + thumbnails ([IMAGE_CACHE_ORIGINALS_DIR_NAME] + thumbnails). */
    const val IMAGE_CACHE_MAX_BYTES = 100L * 1024 * 1024

    /** When over [IMAGE_CACHE_MAX_BYTES], evict oldest files until usage ≤ cap × this ratio. */
    const val IMAGE_CACHE_EVICT_TARGET_RATIO = 0.8f

    /** Multiplier applied to one-finger pan delta on the zoomed detail image. */
    const val IMAGE_PAN_DRAG_SENSITIVITY = 4f

    /** ContentProvider authority suffix for sharing image URLs via [content://]. */
    const val IMAGE_URL_SHARE_AUTHORITY_SUFFIX = ".imageurl.share"

    const val MIME_TEXT_PLAIN = "text/plain"

    /** ContentProvider path segment for share URIs. */
    const val SHARE_PATH = "share"

    /** [NetworkLogInterceptor] success line format: method, URL, status code, duration ms. */
    const val NETWORK_LOG_HTTP_SUCCESS_FORMAT = "HTTP %s %s -> %d (%d ms)"

    /** [NetworkLogEventListenerFactory] failure line format: method, URL, duration ms. */
    const val NETWORK_LOG_HTTP_FAILED_FORMAT = "HTTP %s %s failed after %d ms"
}
