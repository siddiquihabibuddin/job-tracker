package com.jobtracker.android.core.data.db

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jobtracker.android.core.domain.model.AppStatus
import com.jobtracker.android.core.domain.model.Application

@Immutable
@Entity(tableName = "applications")
data class ApplicationEntity(
    @PrimaryKey val id: String,
    val company: String,
    val role: String,
    val status: AppStatus,
    val source: String?,
    val location: String?,
    val salaryMin: Double?,
    val salaryMax: Double?,
    val currency: String?,
    val nextFollowUpOn: String?,
    val tags: List<String>,
    val createdAt: String?,
    val updatedAt: String?,
    val appliedAt: String?,
    val jobLink: String?,
    val resumeUploaded: String?,
    val gotCall: Boolean,
    val rejectDate: String?,
    val loginDetails: String?,
) {
    fun toDomain(): Application = Application(
        id = id,
        company = company,
        role = role,
        status = status,
        source = source,
        location = location,
        salaryMin = salaryMin,
        salaryMax = salaryMax,
        currency = currency,
        nextFollowUpOn = nextFollowUpOn,
        tags = tags,
        createdAt = createdAt,
        updatedAt = updatedAt,
        appliedAt = appliedAt,
        jobLink = jobLink,
        resumeUploaded = resumeUploaded,
        gotCall = gotCall,
        rejectDate = rejectDate,
        loginDetails = loginDetails,
    )

    companion object {
        fun from(app: Application): ApplicationEntity = ApplicationEntity(
            id = app.id,
            company = app.company,
            role = app.role,
            status = app.status,
            source = app.source,
            location = app.location,
            salaryMin = app.salaryMin,
            salaryMax = app.salaryMax,
            currency = app.currency,
            nextFollowUpOn = app.nextFollowUpOn,
            tags = app.tags,
            createdAt = app.createdAt,
            updatedAt = app.updatedAt,
            appliedAt = app.appliedAt,
            jobLink = app.jobLink,
            resumeUploaded = app.resumeUploaded,
            gotCall = app.gotCall,
            rejectDate = app.rejectDate,
            loginDetails = app.loginDetails,
        )
    }
}
