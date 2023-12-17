package com.github.pwoicik.torrentapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
value class MagnetUri(val uri: String) : Parcelable
