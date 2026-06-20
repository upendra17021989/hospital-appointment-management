WITH ranked_active_receipts AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY hospital_id, consultation_payment_id
            ORDER BY receipt_date_time DESC, created_at DESC, id DESC
        ) AS rn
    FROM consultation_receipts
    WHERE UPPER(receipt_status) <> 'VOIDED'
)
UPDATE consultation_receipts cr
SET
    receipt_status = 'VOIDED',
    voided_at = COALESCE(cr.voided_at, NOW())
FROM ranked_active_receipts ranked
WHERE cr.id = ranked.id
  AND ranked.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_consultation_receipts_one_active_per_payment
    ON consultation_receipts (hospital_id, consultation_payment_id)
    WHERE UPPER(receipt_status) <> 'VOIDED';
