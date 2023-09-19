package net.draycia.lipt.libs;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.draycia.lipt.Lipt;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.plugin.EventExecutor;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.reflections.Reflections;

public class LiptEvent extends TwoArgFunction {

    private final On on;
    private final OnEnable onEnable;
    private final OnDisable onDisable;

    public LiptEvent(final Lipt lipt) {
        this.on = new On(lipt);
        this.onEnable = new OnEnable();
        this.onDisable = new OnDisable();
    }

    public void onEnable() {
        this.onEnable.onEnable();
    }

    public void onDisable() {
        this.onDisable.onDisable();
    }

    @Override
    public LuaValue call(final LuaValue modname, final LuaValue env) {
        final LuaValue library = tableOf();

        library.set("on", this.on);
        library.set("onEnable", this.onEnable);
        library.set("onDisable", this.onDisable);

        env.set(LuaValue.valueOf("onEnable"), this.onEnable);
        env.set(LuaValue.valueOf("onDisable"), this.onDisable);

        env.set(modname, library);
        env.get("package").get("loaded").set(modname, library);

        return library;
    }

    public void registerListeners() {
        this.on.registerListeners();
    }

    static class OnEnable extends OneArgFunction {
        private final List<LuaValue> enableFunctions = new ArrayList<>();

        @Override
        public LuaValue call(final LuaValue arg) {
            this.enableFunctions.add(arg);
            return null;
        }

        public void onEnable() {
            this.enableFunctions.forEach(LuaValue::call);
        }
    }

    static class OnDisable extends OneArgFunction {
        private final List<LuaValue> disableFunctions = new ArrayList<>();

        @Override
        public LuaValue call(final LuaValue arg) {
            this.disableFunctions.add(arg);
            return null;
        }

        public void onDisable() {
            this.disableFunctions.forEach(LuaValue::call);
        }
    }

    static class On extends TwoArgFunction implements Listener {

        private Set<Class<? extends Event>> eventClasses;
        private List<String> validEvents;
        private final Map<String, List<LuaValue>> luaListeners = new HashMap<>();
        private final Lipt lipt;

        On(final Lipt lipt) {
            this.lipt = lipt;
            this.locateEvents();
        }

        public List<LuaValue> listenersByEvent(final String eventName) {
            return this.luaListeners.computeIfAbsent(eventName, $ -> new ArrayList<>());
        }

        @Override
        public LuaValue call(final LuaValue eventName, final LuaValue fun) {
            final String name = eventName.checkjstring();

            if (!this.validEvents.contains(name)) {
                throw new LuaError("Invalid event name for '" + name + "'");
            } else {
                this.listenersByEvent(name).add(fun);
            }

            return null;
        }

        private void locateEvents() {
            final Reflections reflections = new Reflections("org.bukkit");// change to also find custom events
            this.eventClasses = reflections.getSubTypesOf(Event.class).stream()
                // Filter out abstract events and interfaces, the old method (filtering by handler list) removes events which extend other events (e.g. EntityDamageByBlockEvent)
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()) && !Modifier.isInterface(clazz.getModifiers()))
                .collect(Collectors.toSet());

            this.validEvents = this.eventClasses.stream().map(Class::getSimpleName).toList();

            this.lipt.getLogger().info("Found " + this.eventClasses.size() + " available events!");
        }

        private void registerListeners() {
            final EventExecutor eventExecutor = (listener, event) -> iGetCalledForEveryEvent(event);
            this.eventClasses.forEach(clazz -> this.lipt.getServer().getPluginManager()
                .registerEvent(clazz, this, EventPriority.MONITOR, eventExecutor, this.lipt));

            this.lipt.getServer().getPluginManager().registerEvent(EntityDamageByBlockEvent.class, this, EventPriority.MONITOR, eventExecutor, this.lipt);
        }

        private final String[] ignored = {"VehicleBlockCollisionEvent", "EntityAirChangeEvent",
            "VehicleUpdateEvent", "ChunkUnloadEvent", "ChunkLoadEvent", "EntityMoveEvent", "PlayerMoveEvent"};

        public void iGetCalledForEveryEvent(Event event) {
            if (Arrays.stream(ignored).anyMatch(ignored -> event.getEventName().equals(ignored))) {
                return;
            }

            final List<LuaValue> listeners = this.listenersByEvent(event.getEventName());

            if (listeners.isEmpty()) {
                return;
            }

            for (final LuaValue listener : listeners) {
                listener.call(CoerceJavaToLua.coerce(event));
            }

        }
    }

}
