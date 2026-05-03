package com.jobtracker.android.core.di

import android.content.Context
import com.jobtracker.android.core.auth.AuthRepository
import com.jobtracker.android.core.auth.SessionManager
import com.jobtracker.android.core.data.db.AppDatabase
import com.jobtracker.android.core.data.prefs.TokenStore
import com.jobtracker.android.core.network.ApiModule
import com.jobtracker.android.core.network.BaseUrlProvider
import com.jobtracker.android.feature.applications.ApplicationsRepository
import com.jobtracker.android.feature.applications.create.SmartCreateRepository
import com.jobtracker.android.feature.applications.detail.NotesAndActivityRepository
import com.jobtracker.android.feature.dashboard.StatsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val tokenStore: TokenStore = TokenStore(appContext)
    val sessionManager: SessionManager = SessionManager(tokenStore)
    val baseUrlProvider: BaseUrlProvider = BaseUrlProvider(appContext)

    private val database: AppDatabase = AppDatabase.create(appContext)

    private val apiModule: ApiModule = ApiModule(
        sessionManager = sessionManager,
        baseUrlProvider = baseUrlProvider,
    )

    val authRepository: AuthRepository = AuthRepository(
        api = apiModule.authApi,
        sessionManager = sessionManager,
    )

    val applicationsRepository: ApplicationsRepository = ApplicationsRepository(
        api = apiModule.applicationsApi,
        dao = database.applicationDao(),
    )

    val statsRepository: StatsRepository = StatsRepository(
        api = apiModule.statsApi,
    )

    val notesAndActivityRepository: NotesAndActivityRepository = NotesAndActivityRepository(
        notesApi = apiModule.notesApi,
        activityApi = apiModule.activityApi,
    )

    val smartCreateRepository: SmartCreateRepository = SmartCreateRepository(
        api = apiModule.smartCreateApi,
    )
}
