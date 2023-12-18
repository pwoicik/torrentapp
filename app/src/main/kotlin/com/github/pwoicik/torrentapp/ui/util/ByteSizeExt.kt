@file:Suppress("unused")

package com.github.pwoicik.torrentapp.ui.util

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.github.pwoicik.torrentapp.domain.model.ByteSize

fun ByteSize.formatSize(context: Context) = Formatter.formatShortFileSize(context, value)!!

fun ByteSize.formatSpeed(context: Context) = formatSize(context) + "/s"

context(Context)
fun ByteSize.formatSpeed() = formatSpeed(this@Context)

context(Context)
fun ByteSize.formatSize() = formatSize(this@Context)

@Composable
fun ByteSize.formatSize() = formatSize(LocalContext.current)

@Composable
fun ByteSize.formatSpeed() = formatSpeed(LocalContext.current)

fun Long.toByteSize() = ByteSize(this)
