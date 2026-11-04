-- Add lens selection to existing installations without resetting prescription data.
USE db_lab;

SET @lens_migration = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'prescriptions' AND column_name = 'lens_sku'),
    'SELECT ''lens_sku already exists'' AS migration_status',
    'ALTER TABLE prescriptions ADD COLUMN lens_sku VARCHAR(255) NULL');
PREPARE lens_migration_statement FROM @lens_migration;
EXECUTE lens_migration_statement;
DEALLOCATE PREPARE lens_migration_statement;

SET @lens_migration = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'prescriptions' AND column_name = 'lens_name'),
    'SELECT ''lens_name already exists'' AS migration_status',
    'ALTER TABLE prescriptions ADD COLUMN lens_name VARCHAR(255) NULL');
PREPARE lens_migration_statement FROM @lens_migration;
EXECUTE lens_migration_statement;
DEALLOCATE PREPARE lens_migration_statement;
