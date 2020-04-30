package com.hztraining;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hztraining.inv.Inventory;
import com.hztraining.inv.InventoryKey;
import com.hztraining.inv.InventoryTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PopulateCacheWithJDBC {

    public static void main(String[] args) {

        // ConfigUtil is in the common module
        String configname = ConfigUtil.findConfigNameInArgs(args);
        ClientConfig config = ConfigUtil.getClientConfigForCluster(configname);

        HazelcastInstance client = HazelcastClient.newHazelcastClient(config);
        IMap<InventoryKey, Inventory> invmap = client.getMap("invmap");

        long start = System.nanoTime();
        int counter=0;
        InventoryTable table = new InventoryTable();
        List<Inventory> items = table.readAllFromDatabase(); // Less than 2 seconds to do this
        Map<InventoryKey, Inventory> localMap = new HashMap<>();
        for (Inventory item : items) {
            InventoryKey key = new InventoryKey(item.getSKU(), item.getLocation());
            localMap.put(key, item);
            //invmap.put(key, item); // NO - will take 30 minutes to load one-at-a-time this way!
        }
        //System.out.println("Starting putAll");
        invmap.putAll(localMap);
        //System.out.println("Done with putAll");
        long finish = System.nanoTime();
        long elapsedNanos = finish - start;
        double elapsedSeconds = (double) elapsedNanos / 1_000_000_000D;

        System.out.printf("Finished in %3.3f seconds\n", elapsedSeconds);
        System.out.println("Final count " + counter);
        client.shutdown();
    }
}