UPDATE fornecedor
SET telefone = REGEXP_REPLACE(telefone, '[^0-9]', '');
