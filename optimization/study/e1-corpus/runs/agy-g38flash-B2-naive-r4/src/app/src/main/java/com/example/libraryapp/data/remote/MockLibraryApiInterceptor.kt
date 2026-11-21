package com.example.libraryapp.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * MockInterceptor simulates the Library REST API responses when operating offline or in demo mode.
 * Handles:
 * - POST /api/v1/auth/login
 * - GET /api/v1/members/me
 * - POST /api/v1/auth/logout
 * - GET /api/v1/members/me/loans
 */
class MockLibraryApiInterceptor(
    private val enabled: Boolean = false
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enabled) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val path = request.url.encodedPath

        return when {
            path.endsWith("api/v1/auth/login") && request.method == "POST" -> {
                val mockResponse = """
                {
                    "access_token": "mock-jwt-token-${System.currentTimeMillis()}",
                    "refresh_token": "mock-refresh-token",
                    "token_type": "Bearer",
                    "expires_in": 3600,
                    "member": {
                        "id": "mem_98234",
                        "card_number": "LIB-2024-8841",
                        "email": "reader@citylibrary.org",
                        "first_name": "Eleanor",
                        "last_name": "Vance",
                        "membership_tier": "Gold Patron",
                        "member_since": "2021-03-15",
                        "status": "Active",
                        "active_loans_count": 3,
                        "holds_count": 1,
                        "outstanding_fines": 0.00,
                        "loans": [
                            {
                                "id": "loan_1",
                                "book_title": "To Kill a Mockingbird",
                                "author": "Harper Lee",
                                "isbn": "978-0061120084",
                                "due_date": "2026-12-01",
                                "is_overdue": false
                            },
                            {
                                "id": "loan_2",
                                "book_title": "Clean Architecture",
                                "author": "Robert C. Martin",
                                "isbn": "978-0134494166",
                                "due_date": "2026-11-28",
                                "is_overdue": false
                            },
                            {
                                "id": "loan_3",
                                "book_title": "The Pragmatic Programmer",
                                "author": "Andrew Hunt, David Thomas",
                                "isbn": "978-0201616224",
                                "due_date": "2026-11-20",
                                "is_overdue": true
                            }
                        ]
                    }
                }
                """.trimIndent()

                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(request)
                    .body(mockResponse.toResponseBody("application/json".toMediaTypeOrNull()))
                    .addHeader("content-type", "application/json")
                    .build()
            }

            path.endsWith("api/v1/members/me") && request.method == "GET" -> {
                val authHeader = request.header("Authorization")
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return Response.Builder()
                        .code(401)
                        .message("Unauthorized")
                        .protocol(Protocol.HTTP_1_1)
                        .request(request)
                        .body("{\"message\":\"Unauthorized access\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                }

                val mockProfile = """
                {
                    "id": "mem_98234",
                    "card_number": "LIB-2024-8841",
                    "email": "reader@citylibrary.org",
                    "first_name": "Eleanor",
                    "last_name": "Vance",
                    "membership_tier": "Gold Patron",
                    "member_since": "2021-03-15",
                    "status": "Active",
                    "active_loans_count": 3,
                    "holds_count": 1,
                    "outstanding_fines": 0.00,
                    "loans": [
                        {
                            "id": "loan_1",
                            "book_title": "To Kill a Mockingbird",
                            "author": "Harper Lee",
                            "isbn": "978-0061120084",
                            "due_date": "2026-12-01",
                            "is_overdue": false
                        },
                        {
                            "id": "loan_2",
                            "book_title": "Clean Architecture",
                            "author": "Robert C. Martin",
                            "isbn": "978-0134494166",
                            "due_date": "2026-11-28",
                            "is_overdue": false
                        },
                        {
                            "id": "loan_3",
                            "book_title": "The Pragmatic Programmer",
                            "author": "Andrew Hunt, David Thomas",
                            "isbn": "978-0201616224",
                            "due_date": "2026-11-20",
                            "is_overdue": true
                        }
                    ]
                }
                """.trimIndent()

                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(request)
                    .body(mockProfile.toResponseBody("application/json".toMediaTypeOrNull()))
                    .addHeader("content-type", "application/json")
                    .build()
            }

            path.endsWith("api/v1/auth/logout") && request.method == "POST" -> {
                Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(request)
                    .body("{}".toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }

            else -> chain.proceed(request)
        }
    }
}
