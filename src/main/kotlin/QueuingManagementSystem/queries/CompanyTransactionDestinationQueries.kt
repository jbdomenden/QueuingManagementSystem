package QueuingManagementSystem.queries

const val getActiveCompanyTransactionDestinationsForKioskQuery = """
SELECT id, company_transaction_id, destination_code, destination_name, destination_subtitle, queue_type_id, sort_order
FROM company_transaction_destinations
WHERE company_transaction_id = ?
  AND status = 'ACTIVE'
ORDER BY sort_order ASC, destination_name ASC
"""

const val getCompanyTransactionDestinationsByTransactionIdQuery = """
SELECT id, company_transaction_id, destination_code, destination_name, destination_subtitle, queue_type_id,
       sort_order, status, created_at::text, updated_at::text
FROM company_transaction_destinations
WHERE company_transaction_id = ?
ORDER BY sort_order ASC, destination_name ASC
"""

const val getCompanyTransactionDestinationByIdQuery = """
SELECT id, company_transaction_id, destination_code, destination_name, destination_subtitle, queue_type_id,
       sort_order, status, created_at::text, updated_at::text
FROM company_transaction_destinations
WHERE id = ?
"""

const val postCompanyTransactionDestinationQuery = """
INSERT INTO company_transaction_destinations(company_transaction_id, destination_code, destination_name, destination_subtitle, queue_type_id, sort_order, status, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
RETURNING id
"""

const val updateCompanyTransactionDestinationQuery = """
UPDATE company_transaction_destinations
SET company_transaction_id = ?,
    destination_code = ?,
    destination_name = ?,
    destination_subtitle = ?,
    queue_type_id = ?,
    sort_order = ?,
    status = ?,
    updated_at = NOW()
WHERE id = ?
"""

const val toggleCompanyTransactionDestinationStatusQuery = """
UPDATE company_transaction_destinations
SET status = ?, updated_at = NOW()
WHERE id = ?
"""

const val deactivateCompanyTransactionDestinationQuery = """
UPDATE company_transaction_destinations
SET status = 'INACTIVE', updated_at = NOW()
WHERE id = ?
"""
