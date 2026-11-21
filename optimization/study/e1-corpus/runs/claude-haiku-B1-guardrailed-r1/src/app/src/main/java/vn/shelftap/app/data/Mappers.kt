package vn.shelftap.app.data

import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.remote.TitleDto
import vn.shelftap.app.domain.Title

internal fun TitleDto.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    // Only HTTPS cover URLs are stored; anything else is treated as "no cover".
    coverUrl = coverUrl?.takeIf { it.startsWith("https://", ignoreCase = true) },
    copiesAvailable = copiesAvailable,
    sortTitle = title.lowercase(),
)

internal fun TitleEntity.toDomain(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors,
    year = year,
    subjects = subjects,
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
)
