package io.d4tzz.ecommerce.payment.application.port;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where to send the customer's browser so they can pay on the gateway's page. The client sends {@code params} to
 * {@code url} with {@code httpMethod}, without changing them: gateways sign these values, so a changed amount is
 * refused by the gateway.
 *
 * @param httpMethod GET or POST, whichever the gateway expects
 * @param url        the gateway's page or endpoint, without a query string: for GET the browser builds it from
 *                   {@code params}
 * @param params     names and values the gateway expects; may be empty
 */
public record PaymentRedirect(String httpMethod, String url, Map<String, String> params) {
    public PaymentRedirect {
        // Keeps the params in the order they were added, which is easier to read when debugging
        params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
    }
}
