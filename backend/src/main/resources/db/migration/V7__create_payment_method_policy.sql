-- Política de pago por pasarela y medio (ADR 0003, enmendado por el ADR 0014, punto 1).
-- Cada fila es una combinación que se le puede ofrecer al cliente: si está habilitada,
-- cuántos minutos se aparta el cupo y su prioridad (la de menor número es la sugerida).
-- Que la pasarela tenga credenciales no se guarda aquí: vive en la configuración del servidor.
CREATE TABLE payment_method_policy (
    id           SMALLINT    GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    provider     VARCHAR(30) NOT NULL,
    method       VARCHAR(30) NOT NULL
        CONSTRAINT ck_payment_method_policy_method
            CHECK (method IN ('CARD', 'PSE', 'NEQUI', 'CASH_POINT', 'MANUAL_TRANSFER', 'CASH_ON_DELIVERY')),
    enabled      BOOLEAN     NOT NULL DEFAULT FALSE,
    hold_minutes INTEGER     NOT NULL
        CONSTRAINT ck_payment_method_policy_hold_minutes CHECK (hold_minutes > 0),
    priority     INTEGER     NOT NULL
        CONSTRAINT ck_payment_method_policy_priority CHECK (priority > 0),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payment_method_policy_provider_method UNIQUE (provider, method)
);

-- Arranque: Mercado Pago con tarjeta, habilitado y sugerido (ADR 0014).
-- PSE queda creado pero deshabilitado hasta probarlo en el sandbox.
-- 36 minutos de cupo apartado, con sus fuentes en el ADR 0013 (punto 5).
INSERT INTO payment_method_policy (provider, method, enabled, hold_minutes, priority) VALUES
    ('mercadopago', 'CARD', TRUE, 36, 1),
    ('mercadopago', 'PSE', FALSE, 36, 2);
