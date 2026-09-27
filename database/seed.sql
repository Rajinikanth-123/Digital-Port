-- Development-only seed. Safe for a disposable local database; it replaces
-- existing rows so every requested count remains deterministic.
USE digital_port_shipping;
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE audit_logs; TRUNCATE TABLE payments; TRUNCATE TABLE invoice_items;
TRUNCATE TABLE invoices; TRUNCATE TABLE documents; TRUNCATE TABLE customs_clearances;
TRUNCATE TABLE shipping_schedules; TRUNCATE TABLE berth_allocations;
TRUNCATE TABLE port_containers; TRUNCATE TABLE containers; TRUNCATE TABLE cargo;
TRUNCATE TABLE shipments; TRUNCATE TABLE berths; TRUNCATE TABLE vessels;
TRUNCATE TABLE terminals; TRUNCATE TABLE ports; TRUNCATE TABLE user_roles; TRUNCATE TABLE users; TRUNCATE TABLE roles;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO roles (id,name,description) VALUES
 (1,'ADMIN','Full system administration'),(2,'PORT_MANAGER','Port and berth operations'),
 (3,'SHIPPING_AGENT','Shipment and document operations'),(4,'VIEWER','Read-only access');
-- BCrypt hash for the development-only password: password
INSERT INTO users (id,name,full_name,email,password,password_hash,role) VALUES
 (1,'System Admin','System Admin','admin@demo.invalid','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','ADMIN'),
 (2,'Port Manager','Port Manager','manager@demo.invalid','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','PORT_MANAGER'),
 (3,'Shipping Agent','Shipping Agent','agent@demo.invalid','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','SHIPPING_AGENT'),
 (4,'Read Only','Read Only','viewer@demo.invalid','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','VIEWER');
INSERT INTO user_roles (user_id,role_id) VALUES (1,1),(2,2),(3,3),(4,4);
INSERT INTO ports (id,name,code,country,timezone) VALUES
 (1,'Colombo International Port','CMB','Sri Lanka','Asia/Colombo'),
 (2,'Singapore Port','SGP','Singapore','Asia/Singapore'),
 (3,'Rotterdam Gateway Port','RTM','Netherlands','Europe/Amsterdam');
INSERT INTO terminals (id,port_id,name,code,terminal_type,capacity) VALUES
 (1,1,'East Container Terminal','CMB-ECT','CONTAINER',12000),(2,1,'South Asia Gateway','CMB-SAG','CONTAINER',9000),
 (3,2,'Pasir Panjang Terminal','SGP-PPT','CONTAINER',18000),(4,2,'Tuas Bulk Terminal','SGP-TBT','BULK',7000),
 (5,3,'Maasvlakte Terminal','RTM-MVT','CONTAINER',22000);
INSERT INTO vessels (id,name,imo_number,vessel_type,flag,capacity,operator_name,status,length_m,draft_m) VALUES
 (1,'MV Ocean Meridian','IMO9784321','Container Ship','Panama',8500,'Oceanic Lines','ACTIVE',300,12.4),
 (2,'Pacific Dawn','IMO9658210','Container Ship','Singapore',6200,'Pacific Trade','ACTIVE',280,11.2),
 (3,'Aurora Clipper','IMO9912044','Container Ship','Norway',9800,'Aurora Marine','ACTIVE',330,13.1),
 (4,'Blue Horizon','IMO9421108','Bulk Carrier','Liberia',12000,'Horizon Shipping','ACTIVE',290,12.8),
 (5,'Seabreeze','IMO9301122','Ro-Ro','Malta',4200,'Seabreeze Group','ACTIVE',220,9.8),
 (6,'Nordic Star','IMO9876501','Container Ship','Denmark',7600,'Nordic Cargo','ACTIVE',310,12.0),
 (7,'Coral Voyager','IMO9012345','Tanker','Bahamas',15000,'Coral Energy','MAINTENANCE',250,13.5),
 (8,'Harbor Spirit','IMO9765432','General Cargo','Cyprus',5300,'Harbor Freight','ACTIVE',205,9.1);
INSERT INTO berths (id,terminal_id,name,max_length_m,status) VALUES
 (1,1,'ECT-01',340,'OCCUPIED'),(2,1,'ECT-02',340,'AVAILABLE'),(3,2,'SAG-01',300,'OCCUPIED'),
 (4,3,'PPT-01',360,'AVAILABLE'),(5,3,'PPT-02',360,'OCCUPIED'),(6,4,'TBT-01',280,'AVAILABLE'),
 (7,5,'MVT-01',400,'OCCUPIED'),(8,5,'MVT-02',400,'AVAILABLE'),(9,2,'SAG-02',300,'AVAILABLE'),(10,4,'TBT-02',280,'AVAILABLE');
INSERT INTO shipments (id,tracking_number,reference_number,origin,destination,cargo_details,shipper,consignee,estimated_arrival,status,container_count,vessel_id) VALUES
 (1,'DPS-2026-0001','SHP-0001','Colombo','Rotterdam','Electronic components','Lanka Exports','North Sea Trading',DATE_ADD(CURRENT_DATE,INTERVAL 5 DAY),'IN_TRANSIT',2,1),
 (2,'DPS-2026-0002','SHP-0002','Singapore','Hamburg','Textiles and garments','Asia Textiles','Euro Fashion',DATE_ADD(CURRENT_DATE,INTERVAL 12 DAY),'CUSTOMS',2,2),
 (3,'DPS-2026-0003','SHP-0003','Busan','Valencia','Automotive parts','Busan Cargo','Iberia Motors',DATE_ADD(CURRENT_DATE,INTERVAL 3 DAY),'REGISTERED',2,3),
 (4,'DPS-2026-0004','SHP-0004','Dubai','Rotterdam','Machinery','Gulf Traders','Delta Works',DATE_ADD(CURRENT_DATE,INTERVAL 8 DAY),'DELIVERED',1,4),
 (5,'DPS-2026-0005','SHP-0005','Colombo','Singapore','Tea and spices','Ceylon Goods','Merlion Retail',DATE_ADD(CURRENT_DATE,INTERVAL 6 DAY),'IN_TRANSIT',2,5),
 (6,'DPS-2026-0006','SHP-0006','Mumbai','Colombo','Pharmaceutical supplies','West India Trade','Island Health',DATE_ADD(CURRENT_DATE,INTERVAL 2 DAY),'CUSTOMS',2,6),
 (7,'DPS-2026-0007','SHP-0007','Rotterdam','New York','Industrial equipment','Euro Freight','Atlantic Works',DATE_ADD(CURRENT_DATE,INTERVAL 10 DAY),'IN_TRANSIT',1,8),
 (8,'DPS-2026-0008','SHP-0008','Singapore','Colombo','Consumer electronics','Lion Trading','Ceylon Retail',DATE_ADD(CURRENT_DATE,INTERVAL 4 DAY),'REGISTERED',1,2),
 (9,'DPS-2026-0009','SHP-0009','Valencia','Colombo','Ceramic tiles','Mediterranean Cargo','Island Build',DATE_ADD(CURRENT_DATE,INTERVAL 9 DAY),'DELIVERED',1,5),
 (10,'DPS-2026-0010','SHP-0010','Colombo','Dubai','Food products','South Asia Foods','Gulf Markets',DATE_ADD(CURRENT_DATE,INTERVAL 7 DAY),'IN_TRANSIT',1,1);
INSERT INTO cargo (shipment_id,description,cargo_type,weight_kg,quantity) SELECT id, CONCAT('Cargo for ',tracking_number),'GENERAL',1000 + id * 125,1 FROM shipments;
INSERT INTO containers (container_number,size,type,size_code,type_code,weight_kg,location,shipment_id,status) VALUES
 ('MSCU1000001','40FT','DRY','40FT','DRY',21000,'CMB-ECT-YARD-A1',1,'IN_TRANSIT'),('MSCU1000002','40FT','REEFER','40FT','REEFER',18500,'CMB-ECT-YARD-A2',1,'IN_TRANSIT'),('MSCU1000003','20FT','DRY','20FT','DRY',12000,'SGP-PPT-YARD-B1',2,'CUSTOMS'),('MSCU1000004','40FT','DRY','40FT','DRY',22000,'SGP-PPT-YARD-B2',2,'CUSTOMS'),('MSCU1000005','40FT','DRY','40FT','DRY',20500,'BUSAN-DEPOT',3,'AVAILABLE'),('MSCU1000006','20FT','TANK','20FT','TANK',14000,'DUBAI-YARD',4,'DELIVERED'),('MSCU1000007','40FT','DRY','40FT','DRY',19000,'CMB-SAG-YARD-C1',5,'IN_TRANSIT'),('MSCU1000008','20FT','REEFER','20FT','REEFER',11000,'MUMBAI-YARD',6,'CUSTOMS'),('MSCU1000009','40FT','DRY','40FT','DRY',23000,'RTM-MVT-YARD-D1',7,'IN_TRANSIT'),('MSCU1000010','20FT','DRY','20FT','DRY',10000,'SGP-TBT-YARD-E1',8,'AVAILABLE'),('MSCU1000011','40FT','DRY','40FT','DRY',21600,'RTM-MVT-YARD-D2',9,'DELIVERED'),('MSCU1000012','40FT','REEFER','40FT','REEFER',19800,'CMB-ECT-YARD-A3',10,'IN_TRANSIT'),('MSCU1000013','20FT','DRY','20FT','DRY',9000,'CMB-ECT-YARD-A4',3,'AVAILABLE'),('MSCU1000014','40FT','DRY','40FT','DRY',24000,'SGP-PPT-YARD-B3',4,'DELIVERED'),('MSCU1000015','20FT','DRY','20FT','DRY',8500,'CMB-SAG-YARD-C2',5,'IN_TRANSIT');
INSERT INTO port_containers (container_number,size,type,status,shipment_id) SELECT container_number,size,type,status,shipment_id FROM containers;
INSERT INTO berth_allocations (id,berth_id,berth,vessel_id,start_time,end_time,status) VALUES
 (1,1,'ECT-01',1,DATE_ADD(NOW(),INTERVAL 1 HOUR),DATE_ADD(NOW(),INTERVAL 9 HOUR),'SCHEDULED'),
 (2,3,'SAG-01',2,DATE_ADD(NOW(),INTERVAL 2 HOUR),DATE_ADD(NOW(),INTERVAL 10 HOUR),'SCHEDULED'),
 (3,5,'PPT-02',3,DATE_ADD(NOW(),INTERVAL 3 HOUR),DATE_ADD(NOW(),INTERVAL 13 HOUR),'SCHEDULED'),
 (4,7,'MVT-01',4,DATE_ADD(NOW(),INTERVAL 4 HOUR),DATE_ADD(NOW(),INTERVAL 16 HOUR),'SCHEDULED'),
 (5,1,'ECT-01',5,DATE_ADD(NOW(),INTERVAL 12 HOUR),DATE_ADD(NOW(),INTERVAL 20 HOUR),'SCHEDULED');
INSERT INTO shipping_schedules (vessel_id,berth_id,arrival_estimated,departure_estimated,status) SELECT vessel_id,berth_id,DATE_ADD(NOW(),INTERVAL id DAY),DATE_ADD(NOW(),INTERVAL (id+1) DAY),'SCHEDULED' FROM berth_allocations;
INSERT INTO shipping_schedules (vessel_id,berth_id,arrival_estimated,departure_estimated,status) VALUES (6,2,DATE_ADD(NOW(),INTERVAL 6 DAY),DATE_ADD(NOW(),INTERVAL 7 DAY),'SCHEDULED'),(7,4,DATE_ADD(NOW(),INTERVAL 8 DAY),DATE_ADD(NOW(),INTERVAL 9 DAY),'SCHEDULED'),(8,6,DATE_ADD(NOW(),INTERVAL 10 DAY),DATE_ADD(NOW(),INTERVAL 11 DAY),'SCHEDULED'),(1,9,DATE_ADD(NOW(),INTERVAL 12 DAY),DATE_ADD(NOW(),INTERVAL 13 DAY),'SCHEDULED'),(2,10,DATE_ADD(NOW(),INTERVAL 14 DAY),DATE_ADD(NOW(),INTERVAL 15 DAY),'SCHEDULED');
INSERT INTO customs_clearances (shipment_id,declaration_number,status,document_details,submitted_at) VALUES
 (1,'CUS-2026-0001','CLEARED','Commercial invoice and packing list',DATE_SUB(NOW(),INTERVAL 5 DAY)),(2,'CUS-2026-0002','PENDING','Invoice under review',DATE_SUB(NOW(),INTERVAL 1 DAY)),(3,'CUS-2026-0003','PENDING','Origin certificate pending',NOW()),(4,'CUS-2026-0004','CLEARED','Complete documentation',DATE_SUB(NOW(),INTERVAL 8 DAY)),(5,'CUS-2026-0005','PENDING','Inspection scheduled',NOW());
INSERT INTO documents (shipment_id,document_type,file_name,storage_key,mime_type,file_size_bytes,uploaded_by) SELECT id,'BILL_OF_LADING',CONCAT(tracking_number,'.pdf'),CONCAT('demo/',tracking_number,'.pdf'),'application/pdf',245760,1 FROM shipments;
INSERT INTO invoices (invoice_number,customer_name,shipment_id,charges,taxes,due_date,payment_status) SELECT CONCAT('INV-2026-',LPAD(id,4,'0')),consignee,id,1000 + id * 250,150 + id * 25,DATE_ADD(CURRENT_DATE,INTERVAL 30 DAY),IF(id <= 5,'PAID','PENDING') FROM shipments;
INSERT INTO invoice_items (invoice_id,description,quantity,unit_price) SELECT id,'Port handling and documentation',1,1000 + id * 250 FROM invoices;
INSERT INTO payments (invoice_id,amount,paid_at,payment_method,reference) SELECT id,total,NOW(),'BANK_TRANSFER',CONCAT('PAY-2026-',LPAD(id,4,'0')) FROM invoices WHERE id <= 5;
