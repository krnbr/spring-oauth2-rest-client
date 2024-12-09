package in.neuw.oauth2.utils;

import in.neuw.oauth2.exceptions.BootTimeException;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.util.Base64;

import static javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm;
import static javax.net.ssl.TrustManagerFactory.getInstance;

public class MtlsConfigCompanion {

    private static final Logger logger = LoggerFactory.getLogger(MtlsConfigCompanion.class);

    private static final String KEY_STORE = "KEY STORE";
    private static final String TRUST_STORE = "TRUST STORE";

    private MtlsConfigCompanion() {}

    @SneakyThrows
    public static KeyManagerFactory getKeyManagerFactory(final KeyStore keyStore,
                                                         final String keyStorePassword) {
        var kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keyStore, keyStorePassword.toCharArray());
        return kmf;
    }

    @SneakyThrows
    public static TrustManagerFactory getTrustManagerFactory(KeyStore trustStore) {
        TrustManagerFactory tmf = getInstance(getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }

    @SneakyThrows
    public static KeyStore keyStore(final String storeContent,
                                    final String storePassword) {
        return keyStore(storeContent, storePassword, KEY_STORE);
    }

    @SneakyThrows
    public static KeyStore keyStore(final String storeContent,
                                    final String storePassword,
                                    final String intention) {
        var storeType = type(storeContent);
        logger.info("store format is {}, purpose is {}", storeType, intention);
        var keyStore = KeyStore.getInstance(storeType);
        var inputStream = new ByteArrayInputStream(getBytesFromBase64String(storeContent));
        keyStore.load(inputStream, storePassword.toCharArray());
        return keyStore;
    }

    @SneakyThrows
    public static KeyStore trustStore(final String storeContent,
                                      final String storePassword) {
        return keyStore(storeContent, storePassword, TRUST_STORE);
    }

    public static String type(final String base64EncodedStoreContent) {
        if (base64EncodedStoreContent.startsWith("/u3+7QAAAA")) {
            return "JKS";
        } else if (base64EncodedStoreContent.startsWith("MII")) {
            return "PKCS12";
        } else {
            throw new BootTimeException("store type not recognized");
        }
    }

    public static byte[] getBytesFromBase64String(final String base64FileString) {
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(base64FileString);
            logger.info("The string is a valid Base64-encoded string.");
        } catch (IllegalArgumentException e) {
            logger.info("The string is not a valid Base64-encoded string");
            throw new BootTimeException("unable to transform input base64 String", e);
        }
        return fileBytes;
    }

}