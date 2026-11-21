package com.example.libraryapp

import android.app.Application
import com.example.libraryapp.data.AccountRepository
import com.example.libraryapp.data.api.ApiClient
import com.example.libraryapp.data.session.SessionStore

class LibraryApplication : Application() {

    val accountRepository: AccountRepository by lazy {
        AccountRepository(
            api = ApiClient.create(),
            session = SessionStore(this)
        )
    }
}
