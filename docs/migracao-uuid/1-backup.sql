CREATE SCHEMA IF NOT EXISTS backup_uuid;

CREATE TABLE backup_uuid.usuarios AS TABLE public.usuarios;
CREATE TABLE backup_uuid.livros AS TABLE public.livros;
CREATE TABLE backup_uuid.reservas AS TABLE public.reservas;
CREATE TABLE backup_uuid.avaliacoes AS TABLE public.avaliacoes;
CREATE TABLE backup_uuid.itens_carrinho AS TABLE public.itens_carrinho;
CREATE TABLE backup_uuid.pedidos AS TABLE public.pedidos;
CREATE TABLE backup_uuid.itens_pedido AS TABLE public.itens_pedido;
CREATE TABLE backup_uuid.usuarios_favoritos AS TABLE public.usuarios_favoritos;

SELECT 'usuarios' AS tabela, count(*) AS linhas FROM backup_uuid.usuarios
UNION ALL SELECT 'livros', count(*) FROM backup_uuid.livros
UNION ALL SELECT 'reservas', count(*) FROM backup_uuid.reservas
UNION ALL SELECT 'avaliacoes', count(*) FROM backup_uuid.avaliacoes
UNION ALL SELECT 'itens_carrinho', count(*) FROM backup_uuid.itens_carrinho
UNION ALL SELECT 'pedidos', count(*) FROM backup_uuid.pedidos
UNION ALL SELECT 'itens_pedido', count(*) FROM backup_uuid.itens_pedido
UNION ALL SELECT 'usuarios_favoritos', count(*) FROM backup_uuid.usuarios_favoritos;
