package QueuingManagementSystem.queries

const val getCompanyTransactionsByCompanyIdQuery = """
SELECT ct.id,
       ct.company_id,
       ct.transaction_code,
       ct.transaction_name,
       ct.transaction_subtitle,
       ct.requires_crew_validation,
       ct.input_mode,
       ct.sort_order,
       ct.status,
       ct.created_at::text,
       ct.updated_at::text
FROM company_transactions ct
WHERE ct.company_id = ?
ORDER BY ct.sort_order ASC, ct.transaction_name ASC
"""

const val getActiveCompanyTransactionsForKioskQuery = """
SELECT ct.id,
       ct.company_id,
       ct.transaction_code,
       ct.transaction_name,
       ct.transaction_subtitle,
       ct.requires_crew_validation,
       ct.input_mode,
       ct.sort_order
FROM company_transactions ct
JOIN companies c ON c.id = ct.company_id
WHERE ct.company_id = ?
  AND ct.status = 'ACTIVE'
  AND c.status = 'ACTIVE'
ORDER BY ct.sort_order ASC, ct.transaction_name ASC
"""

const val getCompanyTransactionByIdQuery = """
SELECT ct.id,
       ct.company_id,
       ct.transaction_code,
       ct.transaction_name,
       ct.transaction_subtitle,
       ct.requires_crew_validation,
       ct.input_mode,
       ct.sort_order,
       ct.status,
       ct.created_at::text,
       ct.updated_at::text
FROM company_transactions ct
WHERE ct.id = ?
"""

const val postCompanyTransactionQuery = """
INSERT INTO company_transactions(company_id, transaction_code, transaction_name, transaction_subtitle, requires_crew_validation, input_mode, sort_order, status, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
RETURNING id
"""

const val updateCompanyTransactionQuery = """
UPDATE company_transactions
SET company_id = ?,
    transaction_code = ?,
    transaction_name = ?,
    transaction_subtitle = ?,
    requires_crew_validation = ?,
    input_mode = ?,
    sort_order = ?,
    status = ?,
    updated_at = NOW()
WHERE id = ?
"""

const val deactivateCompanyTransactionQuery = """
UPDATE company_transactions
SET status = 'INACTIVE',
    updated_at = NOW()
WHERE id = ?
"""

const val toggleCompanyTransactionStatusQuery = """
UPDATE company_transactions
SET status = ?,
    updated_at = NOW()
WHERE id = ?
"""

const val getCompanyTransactionDetailsByIdQuery = """
SELECT ct.id,
       ct.company_id,
       ct.transaction_name,
       ct.status,
       ct.requires_crew_validation,
       ct.input_mode,
       c.status AS company_status
FROM company_transactions ct
JOIN companies c ON c.id = ct.company_id
WHERE ct.id = ?
"""
