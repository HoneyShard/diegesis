package com.pockyl.diegesis.impl.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.diegesis.DiegesisMod;
import com.pockyl.diegesis.api.station.Authority;
import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.diegesis.impl.StationRegistry;
import com.pockyl.diegesis.impl.client.focus.FocusController;
import com.pockyl.diegesis.impl.client.menu.MenuSession;
import com.pockyl.diegesis.impl.network.DiegesisNetwork;
import com.pockyl.diegesis.impl.network.Packets;
import com.pockyl.diegesis.platform.ClientPlatform;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** The client half of the library: wiring called by the loaders, and the hook that turns a block use into a station. */
public final class DiegesisClientRuntime {
    /** Station types whose open condition threw, each logged once. */
    private static final Set<ResourceLocation> FAILED_CONDITIONS = ConcurrentHashMap.newKeySet();
    /** The use being sent is the menu of a station gone over to: {@link #onUseBlock} lets it through. */
    private static boolean usingForMenu;

    private DiegesisClientRuntime() {
    }

    public static void init() {
        DiegesisNetwork.setClientHandler(packet -> ClientStations.get().handle(packet));
    }

    /** End of every client tick. */
    public static void tick() {
        ClientStations.get().tick();
        com.pockyl.diegesis.impl.client.menu.MenuSession.get().tick();
    }

    /**
     * The player used a block (right click). Returns {@link InteractionResult#SUCCESS} when it opened a station - the
     * use is then not sent to the server, so the block's own GUI does not open - or null to let the use go on.
     */
    public static InteractionResult onUseBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (usingForMenu || hand != InteractionHand.MAIN_HAND || StationRegistry.isEmpty()) {
            return null;
        }
        FocusController focus = FocusController.get();
        BlockPos pos = hit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        StationType type = StationRegistry.find(state).orElse(null);
        if (type != null) {
            // A part of a multiblock opens the station of its main block.
            BlockPos main = type.anchor().resolve(player.level(), pos, state);
            if (!main.equals(pos)) {
                pos = main.immutable();
                state = player.level().getBlockState(pos);
                if (!type.matches(state) || !type.anchor().resolve(player.level(), pos, state).equals(pos)) {
                    return InteractionResult.CONSUME;
                }
            }
        }
        if (focus.isActive()) {
            // While the camera is at or flying back from a station, a use must never reach a station block (its vanilla
            // GUI would open); the same station opens again, another one waits until the camera is back.
            boolean again = focus.isExiting() && type != null && focus.station() != null && focus.station().pos().equals(pos);
            if (!again) {
                return type != null ? InteractionResult.CONSUME : null;
            }
        }
        if (type == null || !mayOpen(player, type, pos, state)) {
            return null;
        }
        boolean server = type.authority() == Authority.SERVER;
        boolean menu = type.authority() == Authority.MENU;
        ClientStation station = prepare(player, type, pos, state);
        if (server) {
            ClientPlatform.INSTANCE.sendToServer(new Packets.Open(pos));
        }
        if (menu) {
            // The use goes to the server as vanilla sends it; the menu it opens is taken without its screen.
            com.pockyl.diegesis.impl.client.menu.MenuSession.get().arm(station);
        }
        FocusController.get().begin(station, server || menu);
        return menu ? null : InteractionResult.SUCCESS;
    }

    /**
     * Whether the player may open {@code type}'s station at {@code pos} now: its condition, the reach from where they stand,
     * and a server that has the library (else the block keeps its vanilla behaviour).
     */
    public static boolean mayOpen(LocalPlayer player, StationType type, BlockPos pos, BlockState state) {
        if (player.isSpectator() || !opens(player, type, pos, state)) {
            return false;
        }
        if (player.getEyePosition().distanceToSqr(pos.getCenter()) > type.reach() * type.reach()) {
            return false;
        }
        if (type.authority() == Authority.MENU) {
            // Vanilla's rule: sneaking with anything in a hand uses the item, the block's menu never opens.
            return !(player.isSecondaryUseActive() && (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()));
        }
        return type.authority() != Authority.SERVER || ClientPlatform.INSTANCE.serverHasChannel() && ClientStations.get().serverHas(type);
    }

    /**
     * The station's own open condition. It runs on the use of a block, which the game does not guard, and reads other mods' blocks:
     * one that throws keeps the block's own use instead of crashing the game.
     */
    private static boolean opens(LocalPlayer player, StationType type, BlockPos pos, BlockState state) {
        try {
            return type.openCondition().test(player, pos, state);
        } catch (RuntimeException e) {
            if (FAILED_CONDITIONS.add(type.id())) {
                DiegesisMod.LOGGER.error("Open condition of station {} failed at {}", type.id(), pos, e);
            }
            return false;
        }
    }

    /** The client's station at {@code pos}, facing as it opens for the player, in the seat they would likely get. */
    private static ClientStation prepare(LocalPlayer player, StationType type, BlockPos pos, BlockState state) {
        Direction towardsPlayer = Direction.getNearest(player.getX() - pos.getX() - 0.5, 0.0, player.getZ() - pos.getZ() - 0.5);
        if (!towardsPlayer.getAxis().isHorizontal()) {
            towardsPlayer = player.getDirection().getOpposite();
        }
        ClientStation station = ClientStations.get().getOrCreate(type, pos, type.orientation().resolve(state, towardsPlayer));
        // Fly towards the likely seat at once; the server's answer settles it.
        station.setSeat(type.nearestSeat(pos, station.facing(), player.position(), seat -> true).map(seat -> seat.id()).orElse(null));
        return station;
    }

    /**
     * Goes over from the station worked at to the one at {@code pos} without leaving: the camera flies from one to the other
     * over {@code seconds}. False when that station cannot be opened now.
     */
    public static boolean goOver(BlockPos pos, float seconds) {
        LocalPlayer player = Minecraft.getInstance().player;
        FocusController focus = FocusController.get();
        if (player == null || !focus.isInteractive() && !focus.isGoingOver() || !focus.canGoOver()) {
            return false;
        }
        BlockState state = player.level().getBlockState(pos);
        StationType type = StationRegistry.find(state).orElse(null);
        if (type == null || !type.anchor().resolve(player.level(), pos, state).equals(pos) || !mayOpen(player, type, pos, state)
                || !joinsNeighbors(focus.station().type(), type)) {
            return false;
        }
        boolean server = type.authority() == Authority.SERVER;
        boolean menu = type.authority() == Authority.MENU;
        Authority leftAuthority = focus.station().type().authority();
        if (leftAuthority == Authority.SERVER && !server) {
            ClientPlatform.INSTANCE.sendToServer(new Packets.Close());
        }
        if (leftAuthority == Authority.MENU) {
            // Its menu closes on the server before the next use arrives there: a crafting grid gives its items back as on leaving.
            MenuSession.get().leave(focus.station());
        }
        ClientStation station = prepare(player, type, pos, state);
        if (server) {
            // The server ends the use of the station left as it opens this one.
            ClientPlatform.INSTANCE.sendToServer(new Packets.Open(pos));
        }
        if (menu) {
            // A use of its own, as vanilla sends it, once no earlier use is still unanswered.
            MenuSession.get().useNext(station, () -> useForMenu(player, pos));
        }
        focus.goOver(station, server || menu, seconds);
        return true;
    }

    /**
     * Sends the use of a menu station's block the player goes over to, as a right click on its face towards them would, past
     * {@link #onUseBlock}: the station is already taken, only its menu is wanted.
     */
    private static void useForMenu(LocalPlayer player, BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player != player) {
            return;
        }
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 center = pos.getCenter();
        Direction face = Direction.getNearest(eye.x - center.x, eye.y - center.y, eye.z - center.z);
        net.minecraft.world.phys.Vec3 at = center.add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        usingForMenu = true;
        try {
            minecraft.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, new BlockHitResult(at, face, pos, false));
        } finally {
            usingForMenu = false;
        }
    }

    /**
     * Whether the player may go over between stations of these types. A menu station takes part with stations of every kind:
     * its menu is closed on leaving and a use of its own opens the next one's.
     */
    public static boolean joinsNeighbors(StationType from, StationType to) {
        return true;
    }

    /** Whether the camera is taken over this frame; the camera hook asks first. */
    public static boolean isCameraActive() {
        return FocusController.get().isActive() && Minecraft.getInstance().player != null;
    }
}
