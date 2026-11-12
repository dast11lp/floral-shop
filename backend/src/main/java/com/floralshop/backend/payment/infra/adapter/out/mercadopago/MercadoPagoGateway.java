package com.floralshop.backend.payment.infra.adapter.out.mercadopago;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.floralshop.backend.payment.application.port.out.PaymentGateway;

// Adaptador de Mercado Pago (ADR 0003 y 0014). Por ahora solo dice si tiene credenciales;
// crear el cobro llega en el siguiente PR.
// La credencial viene de la configuración del servidor (variable MERCADOPAGO_ACCESS_TOKEN, ADR 0006)
// y nunca se escribe en el log ni sale en una respuesta.
@Component
class MercadoPagoGateway implements PaymentGateway {

    static final String PROVIDER = "mercadopago";

    private final String accessToken;

    MercadoPagoGateway(@Value("${payment.mercadopago.access-token:}") String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }
}
