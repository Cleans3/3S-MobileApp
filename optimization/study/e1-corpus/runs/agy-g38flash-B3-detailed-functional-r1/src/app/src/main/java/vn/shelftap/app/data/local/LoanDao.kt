package vn.shelftap.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans")
    suspend fun getAllLoans(): List<LoanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(loans: List<LoanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanEntity)

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Transaction
    suspend fun replaceLoans(loans: List<LoanEntity>) {
        clearLoans()
        insertLoans(loans)
    }
}
