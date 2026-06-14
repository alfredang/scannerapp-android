package com.tertiaryinfotech.scannerapp.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Document metadata. Page images/thumbnails/PDFs live on disk; only filenames are stored. */
@Entity(tableName = "documents")
data class ScanDocumentEntity(
    @PrimaryKey val id: String,
    val name: String = "Untitled Scan",
    val createdAt: Long = 0L,
    val modifiedAt: Long = 0L,
    val pageCount: Int = 0,
    val combinedOcrText: String = ""
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ScanDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class ScanPageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val order: Int = 0,
    val originalFileName: String = "",
    val processedFileName: String = "",
    val thumbnailFileName: String = "",
    val filterRaw: String = "original",
    val rotationDegrees: Int = 0,
    val ocrText: String = ""
)

/** A document with its pages (Room @Relation). Pages are sorted by order at read time. */
data class DocumentWithPages(
    @Embedded val document: ScanDocumentEntity,
    @Relation(parentColumn = "id", entityColumn = "documentId")
    val pages: List<ScanPageEntity>
) {
    val orderedPages: List<ScanPageEntity> get() = pages.sortedBy { it.order }
}
