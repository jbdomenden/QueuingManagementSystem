package QueuingManagementSystem.queries

const val getActiveCompaniesForKioskQuery = """
SELECT id,
       company_code,
       company_short_name,
       company_full_name,
       display_size,
       sort_order
FROM companies
WHERE status = 'ACTIVE'
ORDER BY CASE WHEN display_size = 'BIG' THEN 0 ELSE 1 END, sort_order ASC, company_short_name ASC
"""

const val getCompaniesQuery = """
SELECT id,
       company_code,
       company_short_name,
       company_full_name,
       display_size,
       sort_order,
       status,
       created_at::text,
       updated_at::text
FROM companies
ORDER BY CASE WHEN display_size = 'BIG' THEN 0 ELSE 1 END, sort_order ASC, company_short_name ASC
"""

const val getCompanyByIdQuery = """
SELECT id,
       company_code,
       company_short_name,
       company_full_name,
       display_size,
       sort_order,
       status,
       created_at::text,
       updated_at::text
FROM companies
WHERE id = ?
"""

const val postCompanyQuery = """
INSERT INTO companies(company_code, company_short_name, company_full_name, display_size, sort_order, status, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
RETURNING id
"""

const val updateCompanyQuery = """
UPDATE companies
SET company_code = ?,
    company_short_name = ?,
    company_full_name = ?,
    display_size = ?,
    sort_order = ?,
    status = ?,
    updated_at = NOW()
WHERE id = ?
"""

const val deactivateCompanyQuery = """
UPDATE companies
SET status = 'INACTIVE',
    updated_at = NOW()
WHERE id = ?
"""
