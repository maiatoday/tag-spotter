package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.storage.Data

actual fun ByteArray.toFirebaseStorageData(): Data = throw IllegalStateException("Firebase Storage is not supported on JVM desktop fallback.")
