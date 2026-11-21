package vn.shelftap.app.util

sealed interface Resource<out T> {
    data class Success<out T>(val data: T, val isSavedData: Boolean = false) : Resource<T>
    data class Error(val message: String, val isSavedData: Boolean = false) : Resource<Nothing>
    data object Loading : Resource<Nothing>
}

sealed interface RenewResult {
    data class Success(val updatedLoan: vn.shelftap.app.data.model.Loan) : RenewResult
    data class RenewalNotAllowed(val message: String = "This loan cannot be renewed") : RenewResult
    data class Error(val message: String) : RenewResult
}
