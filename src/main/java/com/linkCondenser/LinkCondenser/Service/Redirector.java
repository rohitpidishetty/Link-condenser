package com.linkCondenser.LinkCondenser.Service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

@Service
public class Redirector {
    public String redirect(String url, Firestore db) {
        try {

            DocumentReference docRef = db.collection("redirections").document(url);
            DocumentSnapshot snap = docRef.get().get();
            if (!snap.exists()) return null;
            return (String) snap.get("longUrl");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        return null;
    }
}
