package vn.shelftap.app.ui.catalogue

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.Flow
import vn.shelftap.app.ShelfTapApp
import vn.shelftap.app.model.Title

class CatalogueViewModel(application: Application) : AndroidViewModel(application) {

    val titles: Flow<PagingData<Title>> =
        (application as ShelfTapApp).container.repository.catalogue().cachedIn(viewModelScope)
}
