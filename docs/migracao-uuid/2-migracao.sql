BEGIN;

ALTER TABLE usuarios ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE livros ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE reservas ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE avaliacoes ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE itens_carrinho ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE pedidos ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE itens_pedido ADD COLUMN novo_id uuid NOT NULL DEFAULT gen_random_uuid();

ALTER TABLE reservas ADD COLUMN novo_usuario_id uuid, ADD COLUMN novo_livro_id uuid;
UPDATE reservas t SET novo_usuario_id = u.novo_id FROM usuarios u WHERE u.id = t.usuario_id;
UPDATE reservas t SET novo_livro_id = l.novo_id FROM livros l WHERE l.id = t.livro_id;

ALTER TABLE avaliacoes ADD COLUMN novo_usuario_id uuid, ADD COLUMN novo_livro_id uuid;
UPDATE avaliacoes t SET novo_usuario_id = u.novo_id FROM usuarios u WHERE u.id = t.usuario_id;
UPDATE avaliacoes t SET novo_livro_id = l.novo_id FROM livros l WHERE l.id = t.livro_id;

ALTER TABLE itens_carrinho ADD COLUMN novo_usuario_id uuid, ADD COLUMN novo_livro_id uuid;
UPDATE itens_carrinho t SET novo_usuario_id = u.novo_id FROM usuarios u WHERE u.id = t.usuario_id;
UPDATE itens_carrinho t SET novo_livro_id = l.novo_id FROM livros l WHERE l.id = t.livro_id;

ALTER TABLE pedidos ADD COLUMN novo_usuario_id uuid;
UPDATE pedidos t SET novo_usuario_id = u.novo_id FROM usuarios u WHERE u.id = t.usuario_id;

ALTER TABLE itens_pedido ADD COLUMN novo_pedido_id uuid, ADD COLUMN novo_livro_id uuid;
UPDATE itens_pedido t SET novo_pedido_id = p.novo_id FROM pedidos p WHERE p.id = t.pedido_id;
UPDATE itens_pedido t SET novo_livro_id = l.novo_id FROM livros l WHERE l.id = t.livro_id;

ALTER TABLE usuarios_favoritos ADD COLUMN novo_usuario_id uuid, ADD COLUMN novo_favoritos_id uuid;
UPDATE usuarios_favoritos t SET novo_usuario_id = u.novo_id FROM usuarios u WHERE u.id = t.usuario_id;
UPDATE usuarios_favoritos t SET novo_favoritos_id = l.novo_id FROM livros l WHERE l.id = t.favoritos_id;

ALTER TABLE reservas DROP COLUMN usuario_id CASCADE, DROP COLUMN livro_id CASCADE;
ALTER TABLE avaliacoes DROP COLUMN usuario_id CASCADE, DROP COLUMN livro_id CASCADE;
ALTER TABLE itens_carrinho DROP COLUMN usuario_id CASCADE, DROP COLUMN livro_id CASCADE;
ALTER TABLE pedidos DROP COLUMN usuario_id CASCADE;
ALTER TABLE itens_pedido DROP COLUMN pedido_id CASCADE, DROP COLUMN livro_id CASCADE;
ALTER TABLE usuarios_favoritos DROP COLUMN usuario_id CASCADE, DROP COLUMN favoritos_id CASCADE;

DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT conrelid::regclass::text AS tabela, conname
        FROM pg_constraint
        WHERE contype = 'p' AND conrelid IN ('usuarios'::regclass, 'pedidos'::regclass)
    LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I CASCADE', r.tabela, r.conname);
    END LOOP;
END $$;

ALTER TABLE usuarios ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE usuarios ALTER COLUMN id DROP DEFAULT;
ALTER TABLE usuarios ALTER COLUMN id DROP NOT NULL;
ALTER TABLE usuarios RENAME COLUMN id TO id_legado;

ALTER TABLE pedidos ALTER COLUMN id DROP IDENTITY IF EXISTS;
ALTER TABLE pedidos ALTER COLUMN id DROP DEFAULT;
ALTER TABLE pedidos ALTER COLUMN id DROP NOT NULL;
ALTER TABLE pedidos RENAME COLUMN id TO id_legado;

ALTER TABLE livros DROP COLUMN id CASCADE;
ALTER TABLE reservas DROP COLUMN id CASCADE;
ALTER TABLE avaliacoes DROP COLUMN id CASCADE;
ALTER TABLE itens_carrinho DROP COLUMN id CASCADE;
ALTER TABLE itens_pedido DROP COLUMN id CASCADE;

ALTER TABLE usuarios RENAME COLUMN novo_id TO id;
ALTER TABLE livros RENAME COLUMN novo_id TO id;
ALTER TABLE reservas RENAME COLUMN novo_id TO id;
ALTER TABLE avaliacoes RENAME COLUMN novo_id TO id;
ALTER TABLE itens_carrinho RENAME COLUMN novo_id TO id;
ALTER TABLE pedidos RENAME COLUMN novo_id TO id;
ALTER TABLE itens_pedido RENAME COLUMN novo_id TO id;

ALTER TABLE usuarios ADD PRIMARY KEY (id);
ALTER TABLE livros ADD PRIMARY KEY (id);
ALTER TABLE reservas ADD PRIMARY KEY (id);
ALTER TABLE avaliacoes ADD PRIMARY KEY (id);
ALTER TABLE itens_carrinho ADD PRIMARY KEY (id);
ALTER TABLE pedidos ADD PRIMARY KEY (id);
ALTER TABLE itens_pedido ADD PRIMARY KEY (id);

ALTER TABLE reservas RENAME COLUMN novo_usuario_id TO usuario_id;
ALTER TABLE reservas RENAME COLUMN novo_livro_id TO livro_id;
ALTER TABLE avaliacoes RENAME COLUMN novo_usuario_id TO usuario_id;
ALTER TABLE avaliacoes RENAME COLUMN novo_livro_id TO livro_id;
ALTER TABLE itens_carrinho RENAME COLUMN novo_usuario_id TO usuario_id;
ALTER TABLE itens_carrinho RENAME COLUMN novo_livro_id TO livro_id;
ALTER TABLE pedidos RENAME COLUMN novo_usuario_id TO usuario_id;
ALTER TABLE itens_pedido RENAME COLUMN novo_pedido_id TO pedido_id;
ALTER TABLE itens_pedido RENAME COLUMN novo_livro_id TO livro_id;
ALTER TABLE usuarios_favoritos RENAME COLUMN novo_usuario_id TO usuario_id;
ALTER TABLE usuarios_favoritos RENAME COLUMN novo_favoritos_id TO favoritos_id;

ALTER TABLE itens_carrinho ALTER COLUMN usuario_id SET NOT NULL, ALTER COLUMN livro_id SET NOT NULL;
ALTER TABLE pedidos ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE itens_pedido ALTER COLUMN pedido_id SET NOT NULL, ALTER COLUMN livro_id SET NOT NULL;
ALTER TABLE usuarios_favoritos ALTER COLUMN usuario_id SET NOT NULL, ALTER COLUMN favoritos_id SET NOT NULL;

ALTER TABLE reservas
    ADD CONSTRAINT fk_reservas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_reservas_livro FOREIGN KEY (livro_id) REFERENCES livros (id);
ALTER TABLE avaliacoes
    ADD CONSTRAINT fk_avaliacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_avaliacoes_livro FOREIGN KEY (livro_id) REFERENCES livros (id),
    ADD CONSTRAINT ukdadvtw6rmc3eutio48ptwgswl UNIQUE (usuario_id, livro_id);
ALTER TABLE itens_carrinho
    ADD CONSTRAINT fk_itens_carrinho_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_itens_carrinho_livro FOREIGN KEY (livro_id) REFERENCES livros (id),
    ADD CONSTRAINT ukkh86rd9hf9yjywmtpbk4q0g60 UNIQUE (usuario_id, livro_id);
ALTER TABLE pedidos
    ADD CONSTRAINT fk_pedidos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);
ALTER TABLE itens_pedido
    ADD CONSTRAINT fk_itens_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id),
    ADD CONSTRAINT fk_itens_pedido_livro FOREIGN KEY (livro_id) REFERENCES livros (id);
ALTER TABLE usuarios_favoritos
    ADD CONSTRAINT fk_usuarios_favoritos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    ADD CONSTRAINT fk_usuarios_favoritos_livro FOREIGN KEY (favoritos_id) REFERENCES livros (id);

COMMIT;
