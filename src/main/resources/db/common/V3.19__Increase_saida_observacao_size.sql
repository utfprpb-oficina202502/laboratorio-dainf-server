-- Aumenta o tamanho do campo observacao de VARCHAR(255) para VARCHAR(2000)
-- para acomodar histórico de transições (Empréstimo → Saída)
--
-- Tamanho 2000:
-- - Cabeçalho histórico: ~150 caracteres
-- - Observação original: até 1850 caracteres (preserva observações longas + histórico de Reserva)
--
-- ROLLBACK (manual - executar apenas se necessário):
-- 1. Verificar se existem observações > 255 caracteres:
--    SELECT COUNT(*) FROM saida WHERE LENGTH(observacao) > 255;
-- 2. Se houver dados, migrar ou arquivar antes do rollback
-- 3. Executar:
--    ALTER TABLE saida ALTER COLUMN observacao TYPE VARCHAR(255);
--    ALTER TABLE saida_aud ALTER COLUMN observacao TYPE VARCHAR(255);

ALTER TABLE saida ALTER COLUMN observacao TYPE VARCHAR(2000);

-- Também atualiza a tabela de auditoria correspondente
ALTER TABLE saida_aud ALTER COLUMN observacao TYPE VARCHAR(2000);
