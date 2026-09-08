-- Nullable because they are meaningless on an order that was not rejected.
-- A CHECK tying them to order_status = 'REJECTED' was considered and rejected:
-- it would fire during the transition itself, since the status and the reason
-- are written by two different statements.
ALTER TABLE orders
    ADD COLUMN order_rejection_reason VARCHAR(32),
    ADD COLUMN order_rejection_note VARCHAR(255);
