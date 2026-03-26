package marlow.systems.queuingsystem.controllers

import marlow.systems.queuingsystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.KioskModel
import marlow.systems.queuingsystem.models.KioskQueueTypeAssignmentRequest
import marlow.systems.queuingsystem.models.KioskRequest
import marlow.systems.queuingsystem.queries.*

class KioskController {
    fun createKiosk(request: KioskRequest): Int { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(postKioskQuery).use { s -> s.setInt(1, request.department_id); s.setString(2, request.name); s.setBoolean(3, request.is_active); s.executeQuery().use { r -> if (r.next()) return r.getInt("id") } } }; return 0 }
    fun updateKiosk(request: KioskRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(updateKioskQuery).use { s -> s.setString(1, request.name); s.setBoolean(2, request.is_active); s.setInt(3, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getKiosks(): MutableList<KioskModel> { val list = mutableListOf<KioskModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(getKiosksQuery).use { s -> s.executeQuery().use { rs -> while (rs.next()) list.add(KioskModel(rs.getInt("id"), rs.getInt("department_id"), rs.getString("name"), rs.getBoolean("is_active"))) } } }; return list }
    fun assignQueueTypes(request: KioskQueueTypeAssignmentRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(deleteKioskQueueTypesByKioskQuery).use { s -> s.setInt(1, request.kiosk_id); s.executeUpdate() }; c.prepareStatement(postKioskQueueTypeQuery).use { s -> request.queue_type_ids.forEach { qid -> s.setInt(1, request.kiosk_id); s.setInt(2, qid); s.addBatch() }; s.executeBatch() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
}
