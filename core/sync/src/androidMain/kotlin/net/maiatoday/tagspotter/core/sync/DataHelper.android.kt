package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.storage.Data

actual fun ByteArray.toFirebaseStorageData(): Data = Data(this)
