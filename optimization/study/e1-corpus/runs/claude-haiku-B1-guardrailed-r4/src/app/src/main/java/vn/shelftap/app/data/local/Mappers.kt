package vn.shelftap.app.data.local

import vn.shelftap.app.model.Title

/** Unit separator: never appears in titles, names or subjects. */
internal const val LIST_SEPARATOR = "\u001F"

fun TitleEntity.toTitle(): Title = Title(
    id = id,
    isbn = isbn,
    title = title,
    authors = decodeList(authors),
    year = year,
    subjects = decodeList(subjects),
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
)

fun Title.toEntity(): TitleEntity = TitleEntity(
    id = id,
    isbn = isbn,
    title = title,
    authors = authors.joinToString(LIST_SEPARATOR),
    year = year,
    subjects = subjects.joinToString(LIST_SEPARATOR),
    coverUrl = coverUrl,
    copiesAvailable = copiesAvailable,
)

internal fun TitleEntity.toSearchRow(): TitleSearchEntity = TitleSearchEntity(
    titleId = id,
    title = title,
    authors = authors.replace(LIST_SEPARATOR, " "),
    isbn = isbn,
    subjects = subjects.replace(LIST_SEPARATOR, " "),
)

private fun decodeList(raw: String): List<String> =
    if (raw.isEmpty()) emptyList() else raw.split(LIST_SEPARATOR)
