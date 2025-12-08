ALTER TABLE usuario ADD COLUMN data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_usuario_cleanup ON usuario(email_verificado, data_criacao);
