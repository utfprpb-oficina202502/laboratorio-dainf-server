-- Remove phone number masks/formatting from fornecedor table
-- Stores only digits to ensure consistent data format
UPDATE fornecedor
SET telefone = REGEXP_REPLACE(telefone, '[^0-9]', '', 'g')
WHERE telefone IS NOT NULL;