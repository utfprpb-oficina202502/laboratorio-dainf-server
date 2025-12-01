-- Adiciona campo is_cover na tabela item_image para identificar imagem de capa (H2)
ALTER TABLE item_image ADD COLUMN is_cover BOOLEAN NOT NULL DEFAULT false;

-- Cria indice para otimizar ordenacao por is_cover
CREATE INDEX idx_item_image_item_cover ON item_image(item_id, is_cover DESC, id ASC);
