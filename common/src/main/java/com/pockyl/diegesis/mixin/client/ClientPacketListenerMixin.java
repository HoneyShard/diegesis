package com.pockyl.diegesis.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.DiegesisClientRuntime;
import com.pockyl.diegesis.impl.client.menu.MenuSession;

/**
 * Takes the menu a menu-backed station expects without making its screen, and notes when the server closes it; holds back
 * the use packet of a block that opened a station. Injected
 * at calls vanilla reaches on the game thread; with no menu station open everything goes on as in vanilla.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleOpenScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/MenuScreens;create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V"),
            cancellable = true)
    private void diegesis$takeStationMenu(ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (MenuSession.get().onOpenScreen(packet.getType(), packet.getContainerId())) {
            ci.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void diegesis$holdStationUse(Packet<?> packet, CallbackInfo ci) {
        if (DiegesisClientRuntime.dropsPacket(packet)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleContainerClose", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;clientSideCloseContainer()V"))
    private void diegesis$noteServerClose(ClientboundContainerClosePacket packet, CallbackInfo ci) {
        MenuSession.get().onServerClose();
    }
}
