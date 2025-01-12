package in.neuw.oauth2.config;

import in.neuw.oauth2.client.MockApiClient;
import in.neuw.oauth2.utils.MtlsConfigCompanion;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.apache.hc.client5.http.config.TlsConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.security.KeyStore;
import java.time.Duration;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

@Configuration
@EnableWebSecurity
public class Oauth2ClientConfig {

    @Value("${client.cert.keystore}")
    private String keyStoreContent;

    @Value("${client.cert.keystore.password}")
    private String keyStorePassword;

    private RestClient defaultRestClient;

    @PostConstruct
    @SneakyThrows
    void initialize() {
        this.defaultRestClient = RestClient.builder()
                // default one has no custom ssl customization, etc.
                // .requestFactory(clientHttpRequestFactory())
                .messageConverters((messageConverters) -> {
                    messageConverters.clear();
                    messageConverters.add(new FormHttpMessageConverter());
                    messageConverters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
    }

    /**
     * This is needed as we do not want Spring security to render the login form,
     * in the case here, we just want to consume services protected by OAUTH2
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.build();
    }

    // we mark this as bean if we want the Spring's client to be available automatically
    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsAccessTokenResponseClient() {
        RestClientClientCredentialsTokenResponseClient accessTokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();
        accessTokenResponseClient.setRestClient(this.defaultRestClient);
        return accessTokenResponseClient;
    }

    public OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> mtlsClientCredentialsAccessTokenResponseClient() {
        RestClientClientCredentialsTokenResponseClient accessTokenResponseClient =
                new RestClientClientCredentialsTokenResponseClient();
        var restClient = RestClient.builder()
                .requestFactory(clientHttpRequestFactory())
                .messageConverters((messageConverters) -> {
                    messageConverters.clear();
                    messageConverters.add(new FormHttpMessageConverter());
                    messageConverters.add(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
                .build();
        accessTokenResponseClient.setRestClient(restClient);
        return accessTokenResponseClient;
    }

    /**
     * This one works just perfect for all clients which do not need any explicit overriding like - httpClient, etc.
     * Token Endpoint & Resource Endpoint both are normal TLS, i.e. no Mutual TLS/ MTLS, no client auth
     */
    @Bean
    public RestClient defaultRestClientV1(RestClient.Builder builder,
                                          OAuth2AuthorizedClientManager authorizedClientManager,
                                          @Value("${downstream.base-path}") String downstreamBasePath) {
        var oAuth2ClientHttpRequestInterceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        return builder
                .requestInterceptor(oAuth2ClientHttpRequestInterceptor)
                .baseUrl(downstreamBasePath)
                // set this if you do not want to include the attributes while calling, or in case of http interfaces
                /*.defaultRequest(d -> {
                    d.attributes(clientRegistrationId("clientV1"));
                })*/
                .build();
    }

    @Bean
    public RestClient restClientWithAttributes(RestClient.Builder builder,
                                               OAuth2AuthorizedClientManager authorizedClientManager,
                                               @Value("${downstream.base-path}") String downstreamBasePath) {
        var oAuth2ClientHttpRequestInterceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        return builder
                .requestInterceptor(oAuth2ClientHttpRequestInterceptor)
                .baseUrl(downstreamBasePath)
                .defaultRequest(d -> {
                    d.attributes(clientRegistrationId("clientV1"));
                })
                .build();
    }

    /**
     * the initialization of the exchange based on above rest client
     */
    @Bean
    public MockApiClient mockApiClient(RestClient restClientWithAttributes) {
        RestClientAdapter adapter = RestClientAdapter.create(restClientWithAttributes);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(adapter).build();
        return factory.createClient(MockApiClient.class);
    }

    /**
     * Token Endpoint is MTLS while Resource Endpoint is normal TLS
     */
    @Bean
    @SneakyThrows
    public RestClient restClient(RestClient.Builder builder,
                                 @Value("${downstream.base-path}") String downstreamBasePath,
                                 final ClientRegistrationRepository clientRegistrationRepository) {
        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(c -> c.accessTokenResponseClient(mtlsClientCredentialsAccessTokenResponseClient()))
                .build();

        var oauth2AuthorizedClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);

        var authorizedClientServiceOAuth2AuthorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oauth2AuthorizedClientService);
        authorizedClientServiceOAuth2AuthorizedClientManager.setAuthorizedClientProvider(provider);

        var oAuth2ClientHttpRequestInterceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientServiceOAuth2AuthorizedClientManager);

        return builder
                .requestInterceptor(oAuth2ClientHttpRequestInterceptor)
                .baseUrl(downstreamBasePath)
                .build();
    }

    /**
     * Token Endpoint is normal MTLS while Resource Endpoint is MTLS
     */
    @Bean
    @SneakyThrows
    public RestClient restClientV2(RestClient.Builder builder,
                                   OAuth2AuthorizedClientManager authorizedClientManager,
                                   @Value("${downstream.mtls-base-path}") String downstreamBasePath) {
        var requestInterceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        return builder
                .requestFactory(clientHttpRequestFactory())
                .requestInterceptor(requestInterceptor)
                .baseUrl(downstreamBasePath)
                .build();
    }

    /**
     * Token Endpoint & Resource Endpoint both are MTLS
     */
    @Bean
    @SneakyThrows
    public RestClient restClientV3(RestClient.Builder builder,
                                   final ClientRegistrationRepository clientRegistrationRepository,
                                   @Value("${downstream.mtls-base-path}") String downstreamBasePath) {
        var provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials(c -> c.accessTokenResponseClient(mtlsClientCredentialsAccessTokenResponseClient()))
                .build();

        var oauth2AuthorizedClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);

        var authorizedClientServiceOAuth2AuthorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oauth2AuthorizedClientService);
        authorizedClientServiceOAuth2AuthorizedClientManager.setAuthorizedClientProvider(provider);

        var oAuth2ClientHttpRequestInterceptor = new OAuth2ClientHttpRequestInterceptor(authorizedClientServiceOAuth2AuthorizedClientManager);

        return builder
                .requestInterceptor(oAuth2ClientHttpRequestInterceptor)
                .requestFactory(clientHttpRequestFactory())
                .baseUrl(downstreamBasePath)
                .build();
    }

    @SneakyThrows
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        KeyStore keyStore = MtlsConfigCompanion.keyStore(keyStoreContent, keyStorePassword);
        var sslContext = SSLContexts
                .custom()
                .loadKeyMaterial(keyStore, keyStorePassword.toCharArray())
                // configure truststore if needed, in certain cases, it is already added to the JDKs CA.
                // if not this might be required to add explicit trust.
                //.loadTrustMaterial(trustStore, new TrustAllStrategy())
                .build();
        var defaultS = new DefaultClientTlsStrategy(sslContext);

        var tlsConfig = new TlsConfig.Builder()
                // TODO: tls config as per other needs
                .build();

        var httpClientConnectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(defaultS)
                .setDefaultTlsConfig(tlsConfig)
                .build();

        var httpClient = HttpClients.custom()
                .setConnectionManager(httpClientConnectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));

        return factory;
    }

}
