package com.tertiaryinfotech.scannerapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {

    @Transaction
    @Query("SELECT * FROM documents ORDER BY createdAt DESC")
    fun observeDocuments(): Flow<List<DocumentWithPages>>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id")
    fun observeDocument(id: String): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocument(id: String): DocumentWithPages?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScanDocumentEntity)

    @Update
    suspend fun updateDocument(document: ScanDocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun deleteDocument(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<ScanPageEntity>)

    @Update
    suspend fun updatePage(page: ScanPageEntity)

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun documentCount(): Int
}
