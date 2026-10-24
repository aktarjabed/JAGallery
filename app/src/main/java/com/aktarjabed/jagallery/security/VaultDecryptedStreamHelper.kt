package com.aktarjabed.jagallery.security

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream

object VaultDecryptedStreamHelper {
    fun openDecryptedPipe(
        encryptedFile: File,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
        decryptBlock: (InputStream, OutputStream) -> Unit
    ): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        try {
            scope.launch(Dispatchers.IO) {
                ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { output ->
                    runCatching {
                        FileInputStream(encryptedFile).use { input ->
                            decryptBlock(input, output)
                            output.flush()
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            runCatching { writeSide.close() }
            runCatching { readSide.close() }
            throw e
        }
        return readSide
    }
}
