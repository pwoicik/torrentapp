package com.github.pwoicik.torrentapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@JvmInline
value class MagnetUri @UriMustBeValid constructor(val uri: String) : Parcelable

@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
annotation class UriMustBeValid
