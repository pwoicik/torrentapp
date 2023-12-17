package com.github.pwoicik.torrentapp.ui.util

import android.content.Context
import android.text.format.Formatter
import com.github.pwoicik.torrentapp.domain.model.ByteSize

context(Context)
fun ByteSize.format() = Formatter.formatShortFileSize(this@Context, value)!!
