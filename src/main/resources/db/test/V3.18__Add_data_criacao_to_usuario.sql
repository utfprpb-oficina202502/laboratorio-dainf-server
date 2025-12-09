-- Add data_criacao column allowing NULL with default
ALTER TABLE usuario ADD COLUMN data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Backfill existing rows with current timestamp
UPDATE usuario SET data_criacao = CURRENT_TIMESTAMP WHERE data_criacao IS NULL;

-- Alter column to NOT NULL after backfill
ALTER TABLE usuario ALTER COLUMN data_criacao SET NOT NULL;

-- Create index for cleanup operations
CREATE INDEX IF NOT EXISTS idx_usuario_cleanup ON usuario(email_verificado, data_criacao);
