package com.github.pwoicik.torrentapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Magnet(
    val uri: MagnetUri,
    val name: String,
    val hash: Sha1Hash,
) : Parcelable
