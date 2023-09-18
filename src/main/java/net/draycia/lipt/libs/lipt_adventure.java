package net.draycia.lipt.libs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class lipt_adventure extends TwoArgFunction {

    @Override
    public LuaValue call(final LuaValue modname, final LuaValue env) {
        final LuaValue library = tableOf();

        library.set("mmToComponent", new MiniMessageToComponent());

        final LuaValue ttcName = LuaValue.valueOf("component");
        final LuaValue ttc = new TableToComponent();

        library.set("tableToComponent", ttc);

        env.set(ttcName, ttc);
        env.get("package").get("loaded").set(ttcName, ttc);

        env.set(modname, library);
        env.get("package").get("loaded").set(modname, library);

        return library;
    }

    static class MiniMessageToComponent extends OneArgFunction {
        @Override
        public LuaValue call(final LuaValue arg) {
            return CoerceJavaToLua.coerce(MiniMessage.miniMessage().deserialize(arg.checkjstring()));
        }
    }

    static class TableToComponent extends OneArgFunction {
        @Override
        public LuaValue call(final LuaValue arg) {
            System.out.println(arg.typename());

            if (arg.istable()) {
                final Map<?,?> table = convertToMap(arg.checktable());

                Gson gson = new Gson();
                Type gsonType = new TypeToken<HashMap>() {}.getType();
                String gsonString = gson.toJson(table, gsonType);

                final Component component = GsonComponentSerializer.gson().deserialize(gsonString);
                return CoerceJavaToLua.coerce(component);
            }

            return null;
        }
    }

    private static Map<?, ?> convertToMap(LuaTable table) {
        final HashMap<Object, Object> map = new HashMap<>();

        for (final LuaValue key : table.keys()) {
            final LuaValue value = table.get(key);

            if (key.isstring() && key.checkjstring().equalsIgnoreCase("extra")) {
                map.put(key, convertToList(value.checktable()));
            } else if (value.istable()) {
                map.put(key, convertToMap(value.checktable()));
            } else {
                map.put(key, value.tojstring());
            }
        }

        return map;
    }

    private static Object[] convertToList(LuaTable table) {
        final List<Object> list = new ArrayList<>();

        for (final LuaValue key : table.keys()) {
            final LuaValue value = table.get(key);

            list.add(convertToMap(value.checktable()));
        }

        return list.toArray();
    }

}
