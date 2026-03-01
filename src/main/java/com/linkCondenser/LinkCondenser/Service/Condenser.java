package com.linkCondenser.LinkCondenser.Service;

import com.google.cloud.firestore.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class Condenser {

    public String base62(Long id) {
        String[] chars = {
                "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
                "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
                "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
        };
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(chars[(int) (id % 62)]);
            id = (Long) id / 62;
        }
        return sb.toString();
    }

    private final ReentrantLock lock = new ReentrantLock(true);

    public String condenseLink(String longUrl, Firestore db) {
        lock.lock();

        DocumentReference docRef = db.collection("current_id").document("number");
        try {
            DocumentSnapshot document = docRef.get().get();
            if (document.exists()) {
                Long number = (Long) document.getData().get("id");

                String shortUrl = this.base62(number);
                try {
                    docRef.set(new HashMap<>() {
                        {
                            put("id", number + 1);
                        }
                    });

                    db.collection("redirections")
                            .document(shortUrl)
                            .set(new HashMap<String, String>() {
                                {
                                    put("longUrl", longUrl);
                                }
                            })
                            .get();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    lock.unlock();
                }
                return shortUrl;
            }
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return "";
    }

}
