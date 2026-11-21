package com.example.libraryapp.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject

/**
 * Mock interceptor for local standalone execution or when connecting to demo/test endpoints.
 * Simulates the Library REST API responses for sign-in and profile requests.
 */
class MockLibraryInterceptor(private var enabled: Boolean = true) : Interceptor {

    fun setEnabled(isEnabled: Boolean) {
        this.enabled = isEnabled
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // If mock interceptor is disabled, proceed with normal remote call
        if (!enabled) {
            return chain.proceed(request)
        }

        // Mock Login: POST /api/v1/auth/login
        if (path.endsWith("auth/login") && request.method.equals("POST", ignoreCase = true)) {
            val bodyString = request.body?.let {
                val buffer = okio.Buffer()
                it.writeTo(buffer)
                buffer.readUtf8()
            } ?: ""

            val json = try {
                JSONObject(bodyString)
            } catch (e: Exception) {
                JSONObject()
            }

            val email = json.optString("email", "")
            val password = json.optString("password", "")

            // Simple demo validation rule: any valid email + password with at least 4 characters
            if (email.isNotBlank() && password.length >= 4) {
                val jsonResponse = """
                {
                    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.library_member_demo_token_xyz987",
                    "tokenType": "Bearer",
                    "expiresIn": 86400,
                    "member": {
                        "id": "mem_98412",
                        "memberNumber": "LIB-2024-8841",
                        "firstName": "Eleanor",
                        "lastName": "Vance",
                        "email": "$email",
                        "phone": "+1 (555) 234-5678",
                        "membershipTier": "Gold Scholar",
                        "status": "Active",
                        "joinedDate": "2022-09-15",
                        "expiryDate": "2026-12-31",
                        "borrowedBooksCount": 3,
                        "maxLoans": 10,
                        "outstandingFines": 0.00
                    }
                }
                """.trimIndent()

                return Response.Builder()
                    .code(200)
                    .message("OK")
                    .protocol(Protocol.HTTP_1_1)
                    .request(request)
                    .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            } else {
                val errorResponse = """
                {
                    "error": "Unauthorized",
                    "message": "Invalid member credentials or password too short"
                }
                """.trimIndent()

                return Response.Builder()
                    .code(401)
                    .message("Unauthorized")
                    .protocol(Protocol.HTTP_1_1)
                    .request(request)
                    .body(errorResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }
        }

        // Mock Logout: POST /api/v1/auth/logout
        if (path.endsWith("auth/logout") && request.method.equals("POST", ignoreCase = true)) {
            val jsonResponse = """
            {
                "success": true,
                "message": "Signed out successfully"
            }
            """.trimIndent()

            return Response.Builder()
                .code(200)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        // Mock Profile: GET /api/v1/members/me
        if (path.endsWith("members/me") && request.method.equals("GET", ignoreCase = true)) {
            val authHeader = request.header("Authorization")
            if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
                val errorResponse = """
                {
                    "error": "Unauthorized",
                    "message": "Missing or invalid authorization token"
                }
                """.trimIndent()
                return Response.Builder()
                    .code(401)
                    .message("Unauthorized")
                    .protocol(Protocol.HTTP_1_1)
                    .request(request)
                    .body(errorResponse.toResponseBody("application/json".toMediaType()))
                    .build()
            }

            val jsonResponse = """
            {
                "member": {
                    "id": "mem_98412",
                    "memberNumber": "LIB-2024-8841",
                    "firstName": "Eleanor",
                    "lastName": "Vance",
                    "email": "eleanor.vance@citylibrary.org",
                    "phone": "+1 (555) 234-5678",
                    "membershipTier": "Gold Scholar",
                    "status": "Active",
                    "joinedDate": "2022-09-15",
                    "expiryDate": "2026-12-31",
                    "borrowedBooksCount": 3,
                    "maxLoans": 10,
                    "outstandingFines": 0.00
                },
                "activeLoans": [
                    {
                        "id": "loan_101",
                        "title": "Clean Architecture: A Craftsman's Guide to Software Structure",
                        "author": "Robert C. Martin",
                        "isbn": "978-0134494166",
                        "borrowDate": "2024-10-10",
                        "dueDate": "2024-11-28",
                        "isOverdue": false,
                        "coverUrl": null
                    },
                    {
                        "id": "loan_102",
                        "title": "Design Patterns: Elements of Reusable Object-Oriented Software",
                        "author": "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides",
                        "isbn": "978-0201633610",
                        "borrowDate": "2024-10-12",
                        "dueDate": "2024-11-20",
                        "isOverdue": true,
                        "coverUrl": null
                    },
                    {
                        "id": "loan_103",
                        "title": "Kotlin in Action, Second Edition",
                        "author": "Sebastian Aigner, Roman Elizarov, Svetlana Isakova, Dmitry Jemerov",
                        "isbn": "978-1617299605",
                        "borrowDate": "2024-10-25",
                        "dueDate": "2024-12-05",
                        "isOverdue": false,
                        "coverUrl": null
                    }
                ],
                "loanHistoryCount": 42,
                "reservationsCount": 1
            }
            """.trimIndent()

            return Response.Builder()
                .code(200)
                .message("OK")
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .body(jsonResponse.toResponseBody("application/json".toMediaType()))
                .build()
        }

        return chain.proceed(request)
    }
}
