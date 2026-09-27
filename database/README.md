# MySQL database setup

## Files

- `schema.sql` creates the `digital_port_shipping` database and all normalized
  operational, financial, security, and audit tables.
- `seed.sql` inserts deterministic development-only data: 3 ports, 5
  terminals, 8 vessels, 10 berths, 5 berth allocations, 15 containers, 10
  shipments, 10 cargo records, 10 schedules, 5 customs records, 10 invoices,
  10 invoice items, and 5 payments.

The seed uses fictional `@demo.invalid` accounts and a development BCrypt
password hash. Never run it against a production database.

## MySQL Workbench execution

1. Start MySQL 8.0 and open MySQL Workbench.
2. Open `schema.sql`, select all, and click the lightning-bolt **Execute**
   button. Refresh the **Schemas** panel and confirm `digital_port_shipping`.
3. Open `seed.sql`, execute it against the selected schema, and refresh the
   tables. The script truncates development data first so counts are repeatable.
4. Run the verification queries below. Workbench should show the expected
   counts and joins.

PowerShell alternative from the repository root:

```powershell
mysql -u root -p < .\database\schema.sql
mysql -u root -p digital_port_shipping < .\database\seed.sql
mysql -u root -p digital_port_shipping -e "SELECT COUNT(*) AS shipments FROM shipments;"
```

## Verification queries

```sql
USE digital_port_shipping;
SELECT table_name, table_rows
FROM information_schema.tables
WHERE table_schema = DATABASE()
ORDER BY table_name;

SELECT 'ports' AS entity, COUNT(*) AS total FROM ports
UNION ALL SELECT 'terminals', COUNT(*) FROM terminals
UNION ALL SELECT 'vessels', COUNT(*) FROM vessels
UNION ALL SELECT 'berths', COUNT(*) FROM berths
UNION ALL SELECT 'berth_allocations', COUNT(*) FROM berth_allocations
UNION ALL SELECT 'containers', COUNT(*) FROM containers
UNION ALL SELECT 'shipments', COUNT(*) FROM shipments
UNION ALL SELECT 'cargo', COUNT(*) FROM cargo
UNION ALL SELECT 'shipping_schedules', COUNT(*) FROM shipping_schedules
UNION ALL SELECT 'customs_clearances', COUNT(*) FROM customs_clearances
UNION ALL SELECT 'invoices', COUNT(*) FROM invoices
UNION ALL SELECT 'invoice_items', COUNT(*) FROM invoice_items
UNION ALL SELECT 'payments', COUNT(*) FROM payments;

SELECT b.id, p.code AS port, t.code AS terminal, b.name,
       CASE WHEN b.status = 'AVAILABLE' THEN 'AVAILABLE' ELSE 'OCCUPIED' END AS availability
FROM berths b JOIN terminals t ON t.id = b.terminal_id JOIN ports p ON p.id = t.port_id
ORDER BY p.code, t.code, b.name;

SELECT COUNT(*) AS available_berths FROM berths WHERE status = 'AVAILABLE';
SELECT COUNT(*) AS occupied_berths FROM berths WHERE status <> 'AVAILABLE';
SELECT COUNT(*) AS pending_customs FROM customs_clearances WHERE status = 'PENDING';
SELECT COUNT(*) AS invoices_total, SUM(total) AS invoiced_total FROM invoices;
SELECT COUNT(*) AS payments_total, COALESCE(SUM(amount),0) AS paid_total FROM payments;

SELECT s.tracking_number, s.status AS shipment_status, v.name AS vessel,
       c.container_number, c.status AS container_status
FROM shipments s LEFT JOIN vessels v ON v.id = s.vessel_id
LEFT JOIN containers c ON c.shipment_id = s.id
ORDER BY s.id, c.id;

SELECT ba.id, b.name AS berth, v.name AS vessel, ba.start_time, ba.end_time, ba.status
FROM berth_allocations ba JOIN berths b ON b.id = ba.berth_id
JOIN vessels v ON v.id = ba.vessel_id ORDER BY ba.start_time;
```

## JPA and berth conflict protection

The existing Spring Boot application can point to this schema with
`DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Keep `spring.jpa.hibernate.ddl-auto`
as `validate` in production after the schema has been applied. The existing
`BerthAllocationService.create` method is transactional and checks an
allocation window before saving. For the normalized `berth_id` model, the
repository query should implement:

```sql
WHERE berth_id = :berthId
  AND status NOT IN ('CANCELLED', 'COMPLETED')
  AND start_time < :requestedEnd
  AND end_time > :requestedStart
```

Run that check inside a `@Transactional` service method, validate
`requestedEnd > requestedStart`, and save only when no row is returned. At
higher concurrency, serialize allocation attempts per berth (for example with
a pessimistic lock on the berth row) because a normal pre-check alone cannot
prevent two simultaneous transactions from both succeeding.
