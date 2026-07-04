package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.storage.Data
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

@OptIn(ExperimentalForeignApi::class)
actual fun ByteArray.toFirebaseStorageData(): Data {
    val nsData = if (this.isEmpty()) {
        NSData()
    } else {
        this.usePinned { pinned ->
            NSData.dataWithBytes(
                bytes = pinned.addressOf(0),
                length = this.size.toULong()
            )
        }
    }
    return Data(nsData)
}
