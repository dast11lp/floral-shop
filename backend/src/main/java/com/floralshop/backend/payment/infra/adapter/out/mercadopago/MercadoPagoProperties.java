package com.floralshop.backend.payment.infra.adapter.out.mercadopago;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuración de Mercado Pago (payment.mercadopago.* en application.yaml).
// accessToken: credencial del servidor (variable MERCADOPAGO_ACCESS_TOKEN, ADR 0006). Vacía = no se ofrece.
// baseUrl: la API de Mercado Pago (en las pruebas apunta a WireMock).
// returnUrl: adonde vuelve el cliente después de pagar (la página de resultado del frontend).
// checkoutExpiration: cuánto vive el cobro en Mercado Pago; 15 minutos según el ADR 0013 (punto 5).
@ConfigurationProperties("payment.mercadopago")
public record MercadoPagoProperties(String accessToken, String baseUrl, String returnUrl,
                                    Duration checkoutExpiration) {

    boolean hasAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }
}
