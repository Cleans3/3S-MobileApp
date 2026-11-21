package vn.shelftap.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import vn.shelftap.app.data.model.HistoryItem
import vn.shelftap.app.data.model.Loan
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.model.TitleDetail

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey
    val loanId: String,
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
    fun toLoan(): Loan = Loan(
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
        fun fromLoan(loan: Loan): LoanEntity = LoanEntity(
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

@Entity(tableName = "history_items")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val titleId: String,
    val title: String,
    val outAt: String,
    val returnedAt: String?
) {
    fun toHistoryItem(): HistoryItem = HistoryItem(
        titleId = titleId,
        title = title,
        outAt = outAt,
        returnedAt = returnedAt
    )

    companion object {
        fun fromHistoryItem(item: HistoryItem): HistoryEntity = HistoryEntity(
            titleId = item.titleId,
            title = item.title,
            outAt = item.outAt,
            returnedAt = item.returnedAt
        )
    }
}

@Entity(tableName = "member_profile")
data class MemberProfileEntity(
    @PrimaryKey
    val memberId: String,
    val fullName: String,
    val email: String,
    val memberType: String,
    val status: String,
    val historyEnabled: Boolean,
    val mustChangePassword: Boolean
) {
    fun toMemberProfile(): MemberProfile = MemberProfile(
        memberId = memberId,
        fullName = fullName,
        email = email,
        memberType = memberType,
        status = status,
        historyEnabled = historyEnabled,
        mustChangePassword = mustChangePassword
    )

    companion object {
        fun fromMemberProfile(profile: MemberProfile): MemberProfileEntity = MemberProfileEntity(
            memberId = profile.memberId,
            fullName = profile.fullName,
            email = profile.email,
            memberType = profile.memberType,
            status = profile.status,
            historyEnabled = profile.historyEnabled,
            mustChangePassword = profile.mustChangePassword
        )
    }
}

@Entity(tableName = "title_details")
data class TitleDetailEntity(
    @PrimaryKey
    val titleId: String,
    val isbn: String,
    val title: String,
    val authors: List<String>,
    val subjects: List<String>,
    val coverUrl: String?,
    val copiesAvailable: Int,
    val copiesTotal: Int
) {
    fun toTitleDetail(): TitleDetail = TitleDetail(
        titleId = titleId,
        isbn = isbn,
        title = title,
        authors = authors,
        subjects = subjects,
        coverUrl = coverUrl,
        copiesAvailable = copiesAvailable,
        copiesTotal = copiesTotal
    )

    companion object {
        fun fromTitleDetail(titleDetail: TitleDetail): TitleDetailEntity = TitleDetailEntity(
            titleId = titleDetail.titleId,
            isbn = titleDetail.isbn,
            title = titleDetail.title,
            authors = titleDetail.authors,
            subjects = titleDetail.subjects,
            coverUrl = titleDetail.coverUrl,
            copiesAvailable = titleDetail.copiesAvailable,
            copiesTotal = titleDetail.copiesTotal
        )
    }
}
