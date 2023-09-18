package net.draycia.lipt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import net.draycia.lipt.libs.lipt_adventure;
import net.draycia.lipt.libs.lipt_event;
import net.draycia.lipt.libs.lipt_log;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public final class Lipt extends JavaPlugin implements Listener {

    private final Globals globals = JsePlatform.debugGlobals();
    private File scriptDir;
    private lipt_event liptEvent;

    private List<LuaValue> loadedScripts = new ArrayList<>();

    public Lipt() {
        this.scriptDir = this.getDataFolder();
        this.prepareResources();

        try {
            this.loadDefaultLibraries();
            this.loadCustomScripts();

            for (final LuaValue script : this.loadedScripts) {
                try {
                    script.call();
                } catch (final LuaError error) {
                    error.printStackTrace();
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        this.liptEvent.registerListeners();
    }

    @Override
    public void onDisable() {

    }

    private void prepareResources() {
        this.scriptDir.mkdirs();
        this.saveResource("helloworld.lua", true);
    }

    private void loadDefaultLibraries() {
        new lipt_log().call(LuaValue.valueOf("lipt_log"), this.globals);
        this.liptEvent = new lipt_event(this);
        this.liptEvent.call(LuaValue.valueOf("lipt_event"), this.globals);
        new lipt_adventure().call(LuaValue.valueOf("lipt_adventure"), this.globals);

        // Add Bukkit Server class to global and package.loaded
        final LuaValue serverName = LuaValue.valueOf("server");
        final LuaValue server = CoerceJavaToLua.coerce(Bukkit.getServer());

        this.globals.set(serverName, server);
        this.globals.get("package").get("loaded").set(serverName, server);
    }

    private void loadCustomScripts() throws FileNotFoundException {
        final File[] files = this.scriptDir.listFiles();

        if (files == null) {
            return;
        }

        for (final File file : files) {
            if (!file.getName().endsWith(".lua")) {
                continue;
            }

            final LuaValue script = this.globals.load(new FileReader(new File(this.getDataFolder(), file.getName())), file.getName());

            this.loadedScripts.add(script);
        }
    }

}
