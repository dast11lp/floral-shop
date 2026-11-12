package com.floralshop.backend.payment.application.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.floralshop.backend.payment.application.port.out.PaymentGateway;

// Registro por pasarela (ADR 0003): encuentra el adaptador de cada una por su nombre.
// Varias pueden estar a la vez (ADR 0014). Java puro, sin anotaciones de Spring.
public class PaymentGatewayRegistry {

    private final Map<String, PaymentGateway> byProvider;

    // Dos adaptadores con el mismo nombre son un error de configuración: falla al arrancar.
    public PaymentGatewayRegistry(List<PaymentGateway> gateways) {
        this.byProvider = gateways.stream()
                .collect(Collectors.toUnmodifiableMap(PaymentGateway::provider, Function.identity()));
    }

    // Si existe el adaptador y tiene credenciales. Una pasarela sin código o sin credenciales no se ofrece.
    public boolean isAvailable(String provider) {
        PaymentGateway gateway = byProvider.get(provider);
        return gateway != null && gateway.isConfigured();
    }
}
