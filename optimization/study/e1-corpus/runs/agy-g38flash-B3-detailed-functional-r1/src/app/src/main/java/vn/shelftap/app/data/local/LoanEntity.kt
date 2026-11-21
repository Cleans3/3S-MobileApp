package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.Loan

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val loanId: String,
    val copyId: String,
    val titleId: String,
    val title: String,
    val barcode: String,
    val outAt: String,
    val dueAt: String,
    val returnedAt: String?,
    val renewals: Int,
    val renewable: Boolean
) {
    fun toDomain(): Loan = Loan(
        loanId = loanId,
        copyId = copyId,
        titleId = titleId,
        title = title,
        barcode = barcode,
        outAt = outAt,
        dueAt = dueAt,
        returnedAt = returnedAt,
        renewals = renewals,
        renewable = renewable
    )

    companion object {
        fun fromDomain(loan: Loan): LoanEntity = LoanEntity(
            loanId = loan.loanId,
            copyId = loan.copyId,
            titleId = loan.titleId,
            title = loan.title,
            barcode = loan.barcode,
            outAt = loan.outAt,
            dueAt = loan.dueAt,
            returnedAt = loan.returnedAt,
            renewals = loan.renewals,
            renewable = loan.renewable
        )
    }
}
