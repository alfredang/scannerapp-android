package com.tertiaryinfotech.scannerapp.service

import android.content.Context
import android.graphics.Bitmap
import com.tertiaryinfotech.scannerapp.data.DocumentWithPages
import com.tertiaryinfotech.scannerapp.data.ScanDao
import com.tertiaryinfotech.scannerapp.data.ScanDocumentEntity
import com.tertiaryinfotech.scannerapp.data.ScanPageEntity
import com.tertiaryinfotech.scannerapp.imaging.ImageProcessor
import com.tertiaryinfotech.scannerapp.model.FilterType
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestrates page-asset files on disk and document/page metadata in Room.
 * Mirrors the iOS StorageService: metadata in the DB, images/thumbnails as files
 * under filesDir/Scans/<documentId>/.
 */
class StorageService(
    private val context: Context,
    private val dao: ScanDao
) {
    private fun scansRoot(): File = File(context.filesDir, "Scans").apply { mkdirs() }
    private fun docDir(documentId: String): File = File(scansRoot(), documentId).apply { mkdirs() }

    fun documentDirectory(documentId: String): File = docDir(documentId)
    fun fileFor(documentId: String, name: String): File = File(docDir(documentId), name)

    /** Persist a freshly captured/edited working document. Returns the new document id. */
    suspend fun persist(working: WorkingDocument): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val docId = UUID.randomUUID().toString()
        val dir = docDir(docId)
        val pages = working.pages.mapIndexed { index, wp ->
            val original = "page_${index}_original.jpg"
            val processed = "page_${index}_processed.jpg"
            val thumb = "page_${index}_thumb.jpg"
            BitmapIo.writeJpeg(wp.original, File(dir, original))
            BitmapIo.writeJpeg(wp.processed, File(dir, processed))
            BitmapIo.writeJpeg(ImageProcessor.makeThumbnail(wp.processed), File(dir, thumb))
            ScanPageEntity(
                id = UUID.randomUUID().toString(),
                documentId = docId,
                order = index,
                originalFileName = original,
                processedFileName = processed,
                thumbnailFileName = thumb,
                filterRaw = wp.filter.raw,
                rotationDegrees = wp.rotationDegrees,
                ocrText = wp.ocrText
            )
        }
        val combined = pages.joinToString("\n") { it.ocrText }
        val doc = ScanDocumentEntity(
            id = docId,
            name = working.name,
            createdAt = now,
            modifiedAt = now,
            pageCount = pages.size,
            combinedOcrText = combined
        )
        dao.insertDocument(doc)
        dao.insertPages(pages)
        docId
    }

    suspend fun rename(documentId: String, newName: String) = withContext(Dispatchers.IO) {
        val name = newName.trim()
        if (name.isEmpty()) return@withContext
        val current = dao.getDocument(documentId)?.document ?: return@withContext
        dao.updateDocument(current.copy(name = name, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun delete(documentId: String) = withContext(Dispatchers.IO) {
        docDir(documentId).deleteRecursively()
        dao.deleteDocument(documentId)
    }

    suspend fun duplicate(source: DocumentWithPages): String = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val newId = UUID.randomUUID().toString()
        val srcDir = docDir(source.document.id)
        val dstDir = docDir(newId)
        val pages = source.orderedPages.map { p ->
            listOf(p.originalFileName, p.processedFileName, p.thumbnailFileName).forEach { fn ->
                if (fn.isNotEmpty()) {
                    val from = File(srcDir, fn)
                    if (from.exists()) from.copyTo(File(dstDir, fn), overwrite = true)
                }
            }
            p.copy(id = UUID.randomUUID().toString(), documentId = newId)
        }
        dao.insertDocument(
            source.document.copy(
                id = newId,
                name = "${source.document.name} copy",
                createdAt = now,
                modifiedAt = now
            )
        )
        dao.insertPages(pages)
        newId
    }

    /** Run OCR over each page's processed image and save the recognized text. */
    suspend fun runOcr(documentId: String): Boolean = withContext(Dispatchers.IO) {
        val doc = dao.getDocument(documentId) ?: return@withContext false
        val dir = docDir(documentId)
        val updated = doc.orderedPages.map { page ->
            val bmp = BitmapIo.decodeFile(File(dir, page.processedFileName))
                ?: BitmapIo.decodeFile(File(dir, page.originalFileName))
            val text = if (bmp != null) OcrService.recognize(bmp) else ""
            page.copy(ocrText = text)
        }
        updated.forEach { dao.updatePage(it) }
        val combined = updated.sortedBy { it.order }.joinToString("\n") { it.ocrText }
        dao.updateDocument(doc.document.copy(combinedOcrText = combined, modifiedAt = System.currentTimeMillis()))
        true
    }

    /** Load processed bitmaps (in order) for export/preview of a saved document. */
    suspend fun loadProcessedBitmaps(source: DocumentWithPages): List<Bitmap> = withContext(Dispatchers.IO) {
        val dir = docDir(source.document.id)
        source.orderedPages.mapNotNull { page ->
            BitmapIo.decodeFile(File(dir, page.processedFileName))
                ?: BitmapIo.decodeFile(File(dir, page.originalFileName))
        }
    }

    fun processedFile(documentId: String, page: ScanPageEntity): File =
        File(docDir(documentId), page.processedFileName)

    fun thumbnailFile(documentId: String, page: ScanPageEntity): File =
        File(docDir(documentId), page.thumbnailFileName)

    @Suppress("unused")
    fun defaultFilter(): FilterType = FilterType.ORIGINAL
}
