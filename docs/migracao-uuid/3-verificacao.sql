SELECT 'reservas' AS tabela,
       (SELECT count(*) FROM backup_uuid.reservas) AS antes,
       (SELECT count(*) FROM reservas) AS depois,
       (SELECT count(*) FROM backup_uuid.reservas b
          JOIN backup_uuid.usuarios bu ON bu.id = b.usuario_id
          JOIN backup_uuid.livros bl ON bl.id = b.livro_id
          JOIN usuarios u ON u.id_legado = bu.id
          JOIN reservas r ON r.usuario_id = u.id AND r.data_reserva = b.data_reserva AND r.status = b.status
          JOIN livros l ON l.id = r.livro_id AND l.titulo = bl.titulo) AS relacoes_conferidas
UNION ALL
SELECT 'avaliacoes',
       (SELECT count(*) FROM backup_uuid.avaliacoes),
       (SELECT count(*) FROM avaliacoes),
       (SELECT count(*) FROM backup_uuid.avaliacoes b
          JOIN backup_uuid.livros bl ON bl.id = b.livro_id
          JOIN usuarios u ON u.id_legado = b.usuario_id
          JOIN avaliacoes a ON a.usuario_id = u.id AND a.nota = b.nota
          JOIN livros l ON l.id = a.livro_id AND l.titulo = bl.titulo)
UNION ALL
SELECT 'itens_carrinho',
       (SELECT count(*) FROM backup_uuid.itens_carrinho),
       (SELECT count(*) FROM itens_carrinho),
       (SELECT count(*) FROM backup_uuid.itens_carrinho b
          JOIN backup_uuid.livros bl ON bl.id = b.livro_id
          JOIN usuarios u ON u.id_legado = b.usuario_id
          JOIN itens_carrinho c ON c.usuario_id = u.id AND c.quantidade = b.quantidade
          JOIN livros l ON l.id = c.livro_id AND l.titulo = bl.titulo)
UNION ALL
SELECT 'pedidos',
       (SELECT count(*) FROM backup_uuid.pedidos),
       (SELECT count(*) FROM pedidos),
       (SELECT count(*) FROM backup_uuid.pedidos b
          JOIN pedidos p ON p.id_legado = b.id
          JOIN usuarios u ON u.id = p.usuario_id AND u.id_legado = b.usuario_id)
UNION ALL
SELECT 'itens_pedido',
       (SELECT count(*) FROM backup_uuid.itens_pedido),
       (SELECT count(*) FROM itens_pedido),
       (SELECT count(*) FROM backup_uuid.itens_pedido b
          JOIN backup_uuid.livros bl ON bl.id = b.livro_id
          JOIN pedidos p ON p.id_legado = b.pedido_id
          JOIN itens_pedido i ON i.pedido_id = p.id AND i.quantidade = b.quantidade
          JOIN livros l ON l.id = i.livro_id AND l.titulo = bl.titulo)
UNION ALL
SELECT 'usuarios_favoritos',
       (SELECT count(*) FROM backup_uuid.usuarios_favoritos),
       (SELECT count(*) FROM usuarios_favoritos),
       (SELECT count(*) FROM backup_uuid.usuarios_favoritos b
          JOIN backup_uuid.livros bl ON bl.id = b.favoritos_id
          JOIN usuarios u ON u.id_legado = b.usuario_id
          JOIN usuarios_favoritos f ON f.usuario_id = u.id
          JOIN livros l ON l.id = f.favoritos_id AND l.titulo = bl.titulo);
