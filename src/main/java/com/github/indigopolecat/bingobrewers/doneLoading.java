package com.github.indigopolecat.bingobrewers;

import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraft.inventory.ContainerChest;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraftforge.common.MinecraftForge;
import java.util.List;
import net.minecraft.inventory.Slot;


public class doneLoading {
    // This fires an event when the last slot in the inventory is no longer null, checking every 50 ms to see
    // This might be inconsistent, I'd like to find a better way to do this.
    long startTime;
    boolean doneLoading;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ScheduledExecutorService scheduler2 = Executors.newScheduledThreadPool(1);
    public void onInventoryChanged(ContainerChest containerChest) {

        startTime = System.currentTimeMillis();
        List<Slot> slots = containerChest.inventorySlots;
        doneLoading = false;
        scheduler.scheduleAtFixedRate(() -> compareContainerSize(slots), 0, 50, TimeUnit.MILLISECONDS);
    }

    public void compareContainerSize(List<Slot> slots) {
        long elapsedTime = System.currentTimeMillis() - startTime;
        Slot slot = slots.get(slots.size() - 1);
        if (slot != null && !doneLoading) {
            scheduler2.schedule(() -> {
                doneLoading = true;
                MinecraftForge.EVENT_BUS.post(new InventoryLoadingDoneEvent());
                scheduler.shutdown();
                System.out.println("Detected final inventory slot to be non-null");
            }, 100, TimeUnit.MILLISECONDS);
        } else if (elapsedTime >= 3000) {
            scheduler.shutdown();
            MinecraftForge.EVENT_BUS.post(new InventoryLoadingDoneEvent());
            System.out.println("Inventory Load Scheduler timeout, assuming inventory is loaded.");
        }
    }

    public static class InventoryLoadingDoneEvent extends Event {
    }
}
