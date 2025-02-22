package in.neuw.oauth2.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import in.neuw.oauth2.client.MockApiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@RestController
public class UpstreamPingController {

    private final RestClient defaultRestClientV1;

    private final RestClient restClient;

    private final RestClient restClientV2;

    private final RestClient restClientV3;

    private final MockApiClient mockApiClient;

    public UpstreamPingController(RestClient restClient,
                                  RestClient restClientV2,
                                  RestClient restClientV3,
                                  RestClient defaultRestClientV1, MockApiClient mockApiClient) {
        this.restClient = restClient;
        this.restClientV2 = restClientV2;
        this.restClientV3 = restClientV3;
        this.defaultRestClientV1 = defaultRestClientV1;
        this.mockApiClient = mockApiClient;
    }

    /**
     * Downstream token and resource endpoint are normal TLS endpoints, no MTLS
     */
    @GetMapping("/ping")
    public ObjectNode ping() {
        return this.defaultRestClientV1.get()
                .uri("/mock/ping")
                .attributes(clientRegistrationId("clientV1"))
                .retrieve()
                .body(ObjectNode.class);
    }

    @GetMapping("/bonus/ping")
    public ObjectNode bonusPing() {
        return this.mockApiClient.ping();
    }

    /**
     * Downstream token endpoint is mtls and resource endpoint is normal TLS endpoint
     */
    @GetMapping("/v1/ping")
    public ObjectNode pingV1() {
        return this.restClient.get()
                .uri("/mock/ping")
                .attributes(clientRegistrationId("clientV2"))
                .retrieve()
                .body(ObjectNode.class);
    }

    /**
     * Downstream token endpoint is TLS and resource endpoint is MTLS endpoint
     */
    @GetMapping("/v2/ping")
    public ObjectNode pingV2() {
        return this.restClientV2.get()
                .uri("/mock/ping")
                .attributes(clientRegistrationId("clientV1"))
                .retrieve()
                .body(ObjectNode.class);
    }

    /**
     * Both Downstream token and resource endpoint are MTLS
     */
    @GetMapping("/v3/ping")
    public ObjectNode pingV3() {
        return this.restClientV3.get()
                .uri("/mock/ping")
                .attributes(clientRegistrationId("clientV2"))
                .retrieve()
                .body(ObjectNode.class);
    }

}
