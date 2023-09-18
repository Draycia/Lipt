package net.draycia.lipt.libs;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class lipt_log extends TwoArgFunction {

    @Override
    public LuaValue call(final LuaValue modname, final LuaValue env) {
        final LuaValue library = tableOf();

        library.set("log", new log());

        env.set(modname, library);
        env.get("package").get("loaded").set(modname, library);

        return library;
    }

    static class log extends OneArgFunction {
        @Override
        public LuaValue call(final LuaValue arg) {
            Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<white>LUA: " + arg.checkjstring()));

            return null;
        }
    }

}
