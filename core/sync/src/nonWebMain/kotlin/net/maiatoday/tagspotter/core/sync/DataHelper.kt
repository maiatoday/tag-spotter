package net.maiatoday.tagspotter.core.sync

import dev.gitlive.firebase.storage.Data

expect fun ByteArray.toFirebaseStorageData(): Data
