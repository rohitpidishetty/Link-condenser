package com.linkCondenser.LinkCondenser.Controller;

import com.google.api.Http;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.linkCondenser.LinkCondenser.Service.Condenser;
import com.linkCondenser.LinkCondenser.Service.Redirector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/link-condenser")
public class Gateway {

    private final Condenser condenser;
    private final Redirector redirect;

    private final Firestore db;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public Gateway(Condenser condenser, @Value("${SERVICE}") String service, Redirector redirect) {
        this.condenser = condenser;
        this.redirect = redirect;
        try (InputStream serviceAccount =
                     new ByteArrayInputStream(service.getBytes())) {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            this.db = FirestoreClient.getFirestore();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/condense")
    public String condenseService(@RequestBody Object url) {
        Map<String, String> payload = (HashMap<String, String>) url;
        String longUrl = payload.get("longUrl");
        if (cache.containsKey(longUrl))
            return cache.get(longUrl);
        try {
            String shortUrl = this.condenser.condenseLink(longUrl, db);
            this.cache.put(longUrl, shortUrl);
            return shortUrl;
        } catch (Exception e) {
            return "error";
        }
    }

    @PostMapping("/re-director")
    public String reDirectUrl(@RequestBody Object url) {
        Map<String, String> payload = (HashMap<String, String>) url;
        String longUrl = payload.get("shortUrl");
        if (longUrl.isEmpty()) return null;
        return redirect.redirect(longUrl, db);
    }
}
