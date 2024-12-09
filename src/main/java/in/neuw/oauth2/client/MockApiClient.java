package in.neuw.oauth2.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/mock")
public interface MockApiClient {

    @GetExchange("/ping")
    ObjectNode ping();

}
