package com.github.pwoicik.torrentapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
value class Sha1Hash(val value: String) : Parcelable
