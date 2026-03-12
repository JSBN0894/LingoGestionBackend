-- Insertar estados de operación por defecto
INSERT INTO state (name, description) VALUES 
    ('Pendiente', 'La orden está pendiente de procesamiento'),
    ('En proceso', 'La orden está siendo preparada'),
    ('Enviado', 'La orden ha sido enviada'),
    ('Entregado', 'La orden ha sido entregada al cliente'),
    ('Cancelado', 'La orden ha sido cancelada');
