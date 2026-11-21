package vn.shelftap.app.data

import vn.shelftap.app.data.local.TitleEntity
import vn.shelftap.app.data.local.TitleSearchEntity
import vn.shelftap.app.model.Title

private const val LIST_SEPARATOR = '\u001F'

internal fun List<String>.joinForStorage(): String = joinToString(LIST_SEPARATOR.toString())

internal fun String.splitFromStorage(): List<String> =
    if (isEmpty()) emptyList() else split(LIST_SEPARATOR)

internal fun Title.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors.joinForStorage(),
    year = year,
    subjects = subjects.joinForStorage(),
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
)

internal fun Title.toSearchEntity(): TitleSearchEntity = TitleSearchEntity(
    titleId = id,
    title = title,
    authors = authors.joinForStorage(),
    isbn = isbn,
    subjects = subjects.joinForStorage(),
)

internal fun TitleEntity.toDomain(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors.splitFromStorage(),
    year = year,
    subjects = subjects.splitFromStorage(),
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
)
