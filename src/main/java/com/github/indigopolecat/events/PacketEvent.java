package com.github.indigopolecat.events;

import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.jetbrains.annotations.NotNull;

public abstract class PacketEvent<T extends INetHandler> extends Event {
    private final Packet<T> packet;
    private boolean canceled = false;

    protected PacketEvent(@NotNull Packet<T> packet) {
        this.packet = packet;
    }

    /**
     * @return The packet being sent/received.
     */
    @NotNull
    public Packet<T> getPacket() {
        return packet;
    }

    @Override
    public boolean isCancelable() {
        return true;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean cancel) {
        canceled = cancel;
    }

    /**
     * Fired when a packet is sent to the server.
     */
    public static class Sent extends PacketEvent<INetHandlerPlayServer> {
        public Sent(@NotNull Packet<INetHandlerPlayServer> packet) {
            super(packet);
        }
    }

    /**
     * Fired when a packet is received from the server.
     */
    public static class Received extends PacketEvent<INetHandlerPlayClient> {
        public Received(@NotNull Packet<INetHandlerPlayClient> packet) {
            super(packet);
        }
    }
}