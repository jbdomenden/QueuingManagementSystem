package QueuingManagementSystem.auth.services

import QueuingManagementSystem.auth.models.AuthPrincipal
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date

class JwtService(
    private val secret: String,
    private val issuer: String,
    private val audience: String,
    private val expirationMinutes: Long = 480L
) {
    fun generateToken(principal: AuthPrincipal): String {
        val now = Date()
        val expires = Date(now.time + expirationMinutes * 60_000)
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", principal.userId)
            .withClaim("email", principal.email)
            .withClaim("role", principal.role)
            .withClaim("companyId", principal.companyId)
            .withClaim("departmentId", principal.departmentId)
            .withIssuedAt(now)
            .withExpiresAt(expires)
            .sign(Algorithm.HMAC256(secret))
    }

    fun verifyToken(token: String): DecodedJWT? = try {
        JWT.require(Algorithm.HMAC256(secret)).withIssuer(issuer).withAudience(audience).build().verify(token)
    } catch (_: Exception) {
        null
    }
}
