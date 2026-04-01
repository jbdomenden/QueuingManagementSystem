package QueuingManagementSystem.queries

const val getManagedUsersQuery = """
SELECT u.id, u.email, u.full_name, u.role, u.is_active, u.force_password_change, u.company_id, u.department_id
FROM queue_users u
ORDER BY u.id
"""

const val getManagedUserByIdQuery = """
SELECT u.id, u.email, u.full_name, u.role, u.is_active, u.force_password_change, u.company_id, u.department_id
FROM queue_users u
WHERE u.id = ?
LIMIT 1
"""

const val createManagedUserQuery = """
INSERT INTO queue_users(email, full_name, password_hash, role, is_active, force_password_change, company_id, department_id)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)
RETURNING id
"""

const val updateManagedUserQuery = """
UPDATE queue_users
SET full_name = ?, role = ?, is_active = ?, force_password_change = ?, company_id = ?, department_id = ?, updated_at = NOW()
WHERE id = ?
"""

const val updateManagedUserStatusQuery = "UPDATE queue_users SET is_active = ?, updated_at = NOW() WHERE id = ?"
const val updateManagedUserRoleQuery = "UPDATE queue_users SET role = ?, updated_at = NOW() WHERE id = ?"
const val upsertUserAccessQuery = """
INSERT INTO user_access(user_id, access_key, is_allowed)
VALUES (?, ?, ?)
ON CONFLICT (user_id, access_key)
DO UPDATE SET is_allowed = EXCLUDED.is_allowed, updated_at = NOW()
"""

const val getUserAccessQuery = "SELECT access_key, is_allowed FROM user_access WHERE user_id = ?"

const val getAssetsQuery = """
SELECT id, asset_tag, asset_name, asset_type, device_key, ip_address, mac_address, status, assigned_department_id, assigned_company_id, location, notes
FROM assets
ORDER BY id
"""

const val getAssetByIdQuery = """
SELECT id, asset_tag, asset_name, asset_type, device_key, ip_address, mac_address, status, assigned_department_id, assigned_company_id, location, notes
FROM assets
WHERE id = ?
LIMIT 1
"""

const val createAssetQuery = """
INSERT INTO assets(asset_tag, asset_name, asset_type, device_key, ip_address, mac_address, status, assigned_department_id, assigned_company_id, location, notes)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
RETURNING id
"""

const val updateAssetQuery = """
UPDATE assets
SET asset_tag = ?, asset_name = ?, asset_type = ?, device_key = ?, ip_address = ?, mac_address = ?, status = ?, assigned_department_id = ?, assigned_company_id = ?, location = ?, notes = ?, updated_at = NOW()
WHERE id = ?
"""

const val updateAssetStatusQuery = "UPDATE assets SET status = ?, updated_at = NOW() WHERE id = ?"
const val deleteAssetQuery = "DELETE FROM assets WHERE id = ?"
