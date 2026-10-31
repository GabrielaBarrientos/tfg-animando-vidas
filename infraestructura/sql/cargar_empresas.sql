
-- Carga inicial de empresas financiadoras
-- Base de datos: tfg_animando_vidas

INSERT INTO empresa (nombre)
VALUES
    ('Astara-KIA'),
	('Fundación Prosegur')
ON CONFLICT DO NOTHING;