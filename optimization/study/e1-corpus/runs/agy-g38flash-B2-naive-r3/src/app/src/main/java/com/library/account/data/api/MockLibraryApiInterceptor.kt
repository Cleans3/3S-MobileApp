package com.library.account.data.api

import com.google.gson.Gson
import com.library.account.data.model.BorrowedBook
import com.library.account.data.model.LoginRequest
import com.library.account.data.model.LoginResponse
import com.library.account.data.model.MemberProfile
import com.library.account.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

/**
 * Mock interceptor for the library REST API.
 * When enabled or when live network is unavailable, this interceptor simulates
 * the server responses faithfully according to the REST API schema.
 */
class MockLibraryApiInterceptor(
    private val sessionManager: SessionManager
) : Interceptor {

    private val gson = Gson()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // Check if mock mode is explicitly enabled
        if (sessionManager.isMockModeEnabled()) {
            return handleMockRequest(request, path)
        }

        return try {
            val response = chain.proceed(request)
            if (response.isSuccessful || response.code in 400..499) {
                response
            } else {
                // If remote server returns 5xx or error, fallback to mock simulation
                handleMockRequest(request, path)
            }
        } catch (e: IOException) {
            // Live server is unreachable or offline -> simulate REST API seamlessly
            handleMockRequest(request, path)
        }
    }

    private fun handleMockRequest(request: okhttp3.Request, path: String): Response {
        // Simulate network latency (250ms)
        try {
            Thread.sleep(250)
        } catch (_: InterruptedException) {}

        when {
            path.endsWith("/api/v1/auth/login") && request.method == "POST" -> {
                val bodyString = request.body?.let {
                    val buffer = Buffer()
                    it.writeTo(buffer)
                    buffer.readUtf8()
                } ?: ""

                val loginReq = try {
                    gson.fromJson(bodyString, LoginRequest::class.java)
                } catch (e: Exception) {
                    null
                }

                if (loginReq != null && loginReq.email.isNotBlank() && loginReq.password.isNotBlank()) {
                    // Accept any valid password or standard demo password
                    if (loginReq.password.length >= 6) {
                        val responseObj = LoginResponse(
                            token = "jwt_lib_token_" + System.currentTimeMillis() + "_auth_valid",
                            tokenType = "Bearer",
                            expiresIn = 86400L,
                            memberId = "MEM-78291",
                            email = loginReq.email,
                            fullName = if (loginReq.email.contains("alex", ignoreCase = true)) "Alex Morgan" else "Eleanor Vance",
                            memberNumber = "LIB-2024-88492"
                        )
                        return buildJsonResponse(request, 200, "OK", gson.toJson(responseObj))
                    } else {
                        return buildJsonResponse(request, 401, "Unauthorized", """{"error":"Invalid credentials"}""")
                    }
                } else {
                    return buildJsonResponse(request, 400, "Bad Request", """{"error":"Email and password required"}""")
                }
            }

            path.endsWith("/api/v1/members/me") && request.method == "GET" -> {
                val authHeader = request.header("Authorization")
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return buildJsonResponse(request, 401, "Unauthorized", """{"error":"Authentication required"}""")
                }

                val storedEmail = sessionManager.getUserEmail() ?: "member@library.org"
                val storedName = sessionManager.getUserName() ?: "Eleanor Vance"
                val storedMemberNo = sessionManager.getMemberNumber() ?: "LIB-2024-88492"

                val profile = MemberProfile(
                    id = "MEM-78291",
                    memberNumber = storedMemberNo,
                    fullName = storedName,
                    email = storedEmail,
                    phoneNumber = "+1 (555) 349-8812",
                    membershipType = "General Resident (Adult)",
                    membershipStatus = "ACTIVE",
                    expiryDate = "2027-12-31",
                    joinedDate = "2021-04-15",
                    homeBranch = "Central Metropolitan Library",
                    activeLoansCount = 3,
                    holdsReadyCount = 1,
                    outstandingFees = 0.00,
                    borrowedBooks = listOf(
                        BorrowedBook(
                            id = "book-01",
                            title = "The Midnight Library",
                            author = "Matt Haig",
                            isbn = "978-0525559474",
                            borrowDate = "2026-11-05",
                            dueDate = "2026-11-26",
                            daysRemaining = 5,
                            isOverdue = false,
                            renewalsRemaining = 2,
                            coverColorHex = "#1565C0"
                        ),
                        BorrowedBook(
                            id = "book-02",
                            title = "Clean Architecture: A Craftsman's Guide",
                            author = "Robert C. Martin",
                            isbn = "978-0134494166",
                            borrowDate = "2026-11-01",
                            dueDate = "2026-11-22",
                            daysRemaining = 1,
                            isOverdue = false,
                            renewalsRemaining = 1,
                            coverColorHex = "#2E7D32"
                        ),
                        BorrowedBook(
                            id = "book-03",
                            title = "Klara and the Sun",
                            author = "Kazuo Ishiguro",
                            isbn = "978-0593318171",
                            borrowDate = "2026-10-18",
                            dueDate = "2026-11-15",
                            daysRemaining = -6,
                            isOverdue = true,
                            renewalsRemaining = 0,
                            coverColorHex = "#C62828"
                        )
                    )
                )

                return buildJsonResponse(request, 200, "OK", gson.toJson(profile))
            }

            path.endsWith("/api/v1/auth/logout") && request.method == "POST" -> {
                return buildJsonResponse(request, 200, "OK", """{"status":"logged_out"}""")
            }

            else -> {
                return buildJsonResponse(request, 404, "Not Found", """{"error":"Endpoint not found"}""")
            }
        }
    }

    private fun buildJsonResponse(
        request: okhttp3.Request,
        code: Int,
        message: String,
        jsonBody: String
    ): Response {
        val body = jsonBody.toResponseBody("application/json".toMediaTypeOrNull())
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .body(body)
            .build()
    }
}
