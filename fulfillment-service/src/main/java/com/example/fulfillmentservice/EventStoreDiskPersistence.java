package com.example.fulfillmentservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.fulfillmentservice.model.FulfillmentEvent;

/**
 * Utility for reading/writing eventStore (orderId to list of FulfillmentEvents) from/to disk for idempotence across restarts.
 */
public class EventStoreDiskPersistence {

    private static final String FILE_PATH = "fulfillment-service/eventstore-db.ser";

    // Save Map<String, List<FulfillmentEvent>> to disk
    public static void save(Map<String, List<FulfillmentEvent>> eventStore) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(eventStore);
            System.out.println("[DEBUG] EventStore persisted to disk: " + FILE_PATH);
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to persist EventStore to disk: " + e);
        }
    }

    // Load Map<String, List<FulfillmentEvent>> from disk
    @SuppressWarnings("unchecked")
    public static Map<String, List<FulfillmentEvent>> load() {
        File db = new File(FILE_PATH);
        if (!db.exists()) return new HashMap<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_PATH))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                System.out.println("[DEBUG] EventStore loaded from disk.");
                return (Map<String, List<FulfillmentEvent>>) obj;
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to load EventStore from disk: " + e);
        }
        return new HashMap<>();
    }
}
