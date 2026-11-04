-- Apply once to db_order when schema updates are managed through migrations.
-- Local development currently uses Hibernate ddl-auto=update.
ALTER TABLE orders
    ADD COLUMN rx_od_sph DECIMAL(4,2) NULL,
    ADD COLUMN rx_od_cyl DECIMAL(4,2) NULL,
    ADD COLUMN rx_od_axis INT NULL,
    ADD COLUMN rx_od_add DECIMAL(4,2) NULL,
    ADD COLUMN rx_os_sph DECIMAL(4,2) NULL,
    ADD COLUMN rx_os_cyl DECIMAL(4,2) NULL,
    ADD COLUMN rx_os_axis INT NULL,
    ADD COLUMN rx_os_add DECIMAL(4,2) NULL,
    ADD COLUMN rx_pd DECIMAL(4,1) NULL,
    ADD COLUMN rx_source VARCHAR(30) NULL,
    ADD COLUMN rx_customer_confirmed_at DATETIME(6) NULL;
