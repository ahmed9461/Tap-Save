package io.github.ahmed9461.tapsave.platform.instagram

import io.github.ahmed9461.tapsave.download.*
import io.github.ahmed9461.tapsave.platform.*

class InstagramLinkNormalizer(private val http: HttpTransfer, private val sessionCookie: () -> String? = { null }) {
    fun normalize(input: SharedTarget, cancellation: TransferCancellation): SharedTarget {
        if (InstagramShareParser.parse(input.canonicalUrl) is ShareResult.Target) return input
        fun follow(cookie: String?): SharedTarget {
            val (url, _) = http.page(input.canonicalUrl, cancellation, cookie)
            return (InstagramShareParser.parse(url) as? ShareResult.Target)?.target
                ?: throw SaveFailure(SaveFailure.Reason.METADATA_UNAVAILABLE, "share-link")
        }
        try { return follow(null) } catch (failure: SaveFailure) {
            if (failure.reason != SaveFailure.Reason.AUTH_REQUIRED) throw failure
            cancellation.check()
            val cookie = sessionCookie() ?: throw failure
            return follow(cookie)
        }
    }
}
