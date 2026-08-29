package com.zeezaglobal.gymmanagement.payment;

import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayClientResolver {

    private final Map<PaymentMethod, PaymentGatewayClient> clientsByMethod;

    public PaymentGatewayClientResolver(List<PaymentGatewayClient> clients) {
        this.clientsByMethod = clients.stream()
                .collect(Collectors.toMap(PaymentGatewayClient::supportedMethod, Function.identity()));
    }

    public PaymentGatewayClient resolve(PaymentMethod method) {
        if (method == PaymentMethod.CASH) {
            throw new BadRequestException("CASH payments are recorded directly, not routed through a gateway");
        }
        PaymentGatewayClient client = clientsByMethod.get(method);
        if (client == null) {
            throw new BadRequestException("No gateway client configured for payment method: " + method);
        }
        return client;
    }
}
