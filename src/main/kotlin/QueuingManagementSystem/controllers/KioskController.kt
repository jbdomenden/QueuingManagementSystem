package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.KioskModel
import QueuingManagementSystem.models.KioskQueueTypeAssignmentRequest
import QueuingManagementSystem.models.KioskRequest
import QueuingManagementSystem.queries.*

class KioskController {
    fun createKiosk(request: QueuingManagementSystem.models.KioskRequest): Int { QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        QueuingManagementSystem.queries.postKioskQuery
    ).use { s -> s.setInt(1, request.department_id); s.setString(2, request.name); s.setBoolean(3, request.is_active); s.executeQuery().use { r -> if (r.next()) return r.getInt("id") } } }; return 0 }
    fun updateKiosk(request: QueuingManagementSystem.models.KioskRequest): Boolean { QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        QueuingManagementSystem.queries.updateKioskQuery
    ).use { s -> s.setString(1, request.name); s.setBoolean(2, request.is_active); s.setInt(3, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getKiosks(): MutableList<QueuingManagementSystem.models.KioskModel> { val list = mutableListOf<QueuingManagementSystem.models.KioskModel>(); QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        QueuingManagementSystem.queries.getKiosksQuery
    ).use { s -> s.executeQuery().use { rs -> while (rs.next()) list.add(
        QueuingManagementSystem.models.KioskModel(
            rs.getInt("id"),
            rs.getInt("department_id"),
            rs.getString("name"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun assignQueueTypes(request: QueuingManagementSystem.models.KioskQueueTypeAssignmentRequest): Boolean { QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        QueuingManagementSystem.queries.deleteKioskQueueTypesByKioskQuery
    ).use { s -> s.setInt(1, request.kiosk_id); s.executeUpdate() }; c.prepareStatement(QueuingManagementSystem.queries.postKioskQueueTypeQuery).use { s -> request.queue_type_ids.forEach { qid -> s.setInt(1, request.kiosk_id); s.setInt(2, qid); s.addBatch() }; s.executeBatch() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
}
