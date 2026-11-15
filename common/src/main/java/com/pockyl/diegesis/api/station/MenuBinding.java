package com.pockyl.diegesis.api.station;

import net.minecraft.world.inventory.MenuType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How a menu-backed station ({@link Authority#MENU}) maps onto the block's vanilla menu: which menu types it takes, which
 * menu slots its slot groups and outputs show. Any other menu the server opens for the use (a plugin's chest screen, a
 * mod's own table) is left alone: its vanilla screen opens and the station leaves.
 *
 * <pre>{@code
 * MenuBinding.of(MenuType.CRAFTING).output("result", 0).slots("grid", 1, 9).build()
 * }</pre>
 */
public final class MenuBinding {
    /** Ticks the server has to open the menu after the use; then the station gives up and the camera flies back. */
    public static final int DEFAULT_OPEN_TIMEOUT = 30;
    /** Ticks an automatic click waits after the menu opened. */
    public static final int DEFAULT_AUTO_DELAY = 3;

    private final List<MenuType<?>> accepted;
    private final Map<String, SlotRange> slots;
    private final Map<String, Integer> outputs;
    private final int openTimeoutTicks;
    private final int autoDelayTicks;

    private MenuBinding(Builder builder) {
        this.accepted = List.copyOf(builder.accepted);
        this.slots = Map.copyOf(builder.slots);
        this.outputs = Map.copyOf(builder.outputs);
        this.openTimeoutTicks = builder.openTimeoutTicks;
        this.autoDelayTicks = builder.autoDelayTicks;
    }

    /** A binding taking menus of these types. */
    public static Builder of(MenuType<?>... accepted) {
        if (accepted.length == 0) {
            throw new IllegalArgumentException("A menu binding needs at least one menu type");
        }
        return new Builder(List.of(accepted));
    }

    public boolean accepts(MenuType<?> type) {
        return type != null && accepted.contains(type);
    }

    public List<MenuType<?>> accepted() {
        return accepted;
    }

    /** Slot group name to the menu slots it shows. */
    public Map<String, SlotRange> slots() {
        return slots;
    }

    /** Output name to the menu slot it shows. */
    public Map<String, Integer> outputs() {
        return outputs;
    }

    public int openTimeoutTicks() {
        return openTimeoutTicks;
    }

    public int autoDelayTicks() {
        return autoDelayTicks;
    }

    /** {@code count} menu slots from {@code first}. */
    public record SlotRange(int first, int count) {
    }

    public static final class Builder {
        private final List<MenuType<?>> accepted;
        private final Map<String, SlotRange> slots = new LinkedHashMap<>();
        private final Map<String, Integer> outputs = new LinkedHashMap<>();
        private int openTimeoutTicks = DEFAULT_OPEN_TIMEOUT;
        private int autoDelayTicks = DEFAULT_AUTO_DELAY;

        private Builder(List<MenuType<?>> accepted) {
            this.accepted = accepted;
        }

        /** The station's slot group {@code group} shows {@code count} menu slots from {@code firstMenuSlot}. */
        public Builder slots(String group, int firstMenuSlot, int count) {
            if (firstMenuSlot < 0 || count <= 0) {
                throw new IllegalArgumentException("Bad menu slots for " + group + ": " + firstMenuSlot + " + " + count);
            }
            slots.put(Objects.requireNonNull(group), new SlotRange(firstMenuSlot, count));
            return this;
        }

        /** The station's output {@code name} shows menu slot {@code menuSlot}; a click takes from it as from that slot. */
        public Builder output(String name, int menuSlot) {
            if (menuSlot < 0) {
                throw new IllegalArgumentException("Bad menu slot for " + name + ": " + menuSlot);
            }
            outputs.put(Objects.requireNonNull(name), menuSlot);
            return this;
        }

        /** Ticks the server has to open the menu, {@value #DEFAULT_OPEN_TIMEOUT} by default. */
        public Builder openTimeoutTicks(int ticks) {
            openTimeoutTicks = Math.max(1, ticks);
            return this;
        }

        /** Ticks an automatic click waits after the menu opened, {@value #DEFAULT_AUTO_DELAY} by default, never below 2. */
        public Builder autoDelayTicks(int ticks) {
            autoDelayTicks = Math.max(2, ticks);
            return this;
        }

        public MenuBinding build() {
            return new MenuBinding(this);
        }
    }
}
