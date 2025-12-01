-- Migration V3.16 - Add Missing Audit Mod Columns
--
-- OBJETIVO: Adicionar colunas _mod faltantes nas tabelas de auditoria
-- para corresponder aos nomes das propriedades das entidades
-- (Nova versão após correção de sintaxe SQL)

-- EMPRESTIMO_DEVOLUCAO_ITEM_AUD - Adicionar colunas faltantes
ALTER TABLE emprestimo_devolucao_item_aud ADD COLUMN IF NOT EXISTS status_devolucao_mod BOOLEAN;
ALTER TABLE emprestimo_devolucao_item_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;
ALTER TABLE emprestimo_devolucao_item_aud ADD COLUMN IF NOT EXISTS emprestimo_mod BOOLEAN;

-- EMPRESTIMO_ITEM_AUD - Adicionar colunas faltantes
ALTER TABLE emprestimo_item_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;
ALTER TABLE emprestimo_item_aud ADD COLUMN IF NOT EXISTS emprestimo_mod BOOLEAN;

-- EMPRESTIMO_AUD - Adicionar colunas faltantes (FKs e coleções)
ALTER TABLE emprestimo_aud ADD COLUMN IF NOT EXISTS usuario_responsavel_mod BOOLEAN;
ALTER TABLE emprestimo_aud ADD COLUMN IF NOT EXISTS usuario_emprestimo_mod BOOLEAN;
ALTER TABLE emprestimo_aud ADD COLUMN IF NOT EXISTS emprestimo_item_mod BOOLEAN;
ALTER TABLE emprestimo_aud ADD COLUMN IF NOT EXISTS emprestimo_devolucao_item_mod BOOLEAN;

-- ITEM_AUD - Adicionar colunas faltantes  
ALTER TABLE item_aud ADD COLUMN IF NOT EXISTS grupo_mod BOOLEAN;
ALTER TABLE item_aud ADD COLUMN IF NOT EXISTS image_item_mod BOOLEAN;

-- SAIDA_ITEM_AUD - Adicionar colunas faltantes
ALTER TABLE saida_item_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;
ALTER TABLE saida_item_aud ADD COLUMN IF NOT EXISTS saida_mod BOOLEAN;

-- RESERVA_ITEM_AUD - Adicionar colunas faltantes
ALTER TABLE reserva_item_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;
ALTER TABLE reserva_item_aud ADD COLUMN IF NOT EXISTS reserva_mod BOOLEAN;

-- COMPRA_ITEM_AUD - Adicionar colunas faltantes
ALTER TABLE compra_item_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;
ALTER TABLE compra_item_aud ADD COLUMN IF NOT EXISTS compra_mod BOOLEAN;

-- SOLICITACAO_ITEM_AUD - Adicionar colunas faltantes
ALTER TABLE solicitacao_item_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;
ALTER TABLE solicitacao_item_aud ADD COLUMN IF NOT EXISTS solicitacao_mod BOOLEAN;

-- ITEM_IMAGE_AUD - Adicionar colunas faltantes
ALTER TABLE item_image_aud ADD COLUMN IF NOT EXISTS item_mod BOOLEAN;

-- SAIDA_AUD - Adicionar colunas faltantes
ALTER TABLE saida_aud ADD COLUMN IF NOT EXISTS usuario_mod BOOLEAN;
ALTER TABLE saida_aud ADD COLUMN IF NOT EXISTS emprestimo_mod BOOLEAN;

-- RESERVA_AUD - Adicionar colunas faltantes
ALTER TABLE reserva_aud ADD COLUMN IF NOT EXISTS usuario_mod BOOLEAN;

-- COMPRA_AUD - Adicionar colunas faltantes
ALTER TABLE compra_aud ADD COLUMN IF NOT EXISTS fornecedor_mod BOOLEAN;

-- SOLICITACAO_AUD - Adicionar colunas faltantes
ALTER TABLE solicitacao_aud ADD COLUMN IF NOT EXISTS usuario_mod BOOLEAN;
