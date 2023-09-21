package net.draycia.lipt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.draycia.lipt.libs.LiptAdventure;
import net.draycia.lipt.libs.LiptEvent;
import net.draycia.lipt.libs.LiptLog;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

public final class Lipt extends JavaPlugin implements Listener {

    private final File scriptDir;
    private LiptEvent liptEvent;

    private final List<LuaValue> loadedScripts = new ArrayList<>();

    public Lipt() {
        this.scriptDir = new File(this.getDataFolder(), "scripts");
        this.prepareResources();

        try {
            final Globals globals = JsePlatform.standardGlobals();

            this.loadDefaultLibraries(globals);
            this.loadCustomScripts(globals);

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
        this.liptEvent.onEnable();
    }

    @Override
    public void onDisable() {
        this.liptEvent.onDisable();
    }

    private void prepareResources() {
        this.scriptDir.mkdirs();
        this.saveResource("scripts/helloworld.lua", true);
    }

    private void loadDefaultLibraries(final Globals globals) {
        new LiptLog().call(LuaValue.valueOf("lipt_log"), globals);
        this.liptEvent = new LiptEvent(this);
        this.liptEvent.call(LuaValue.valueOf("lipt_event"), globals);
        new LiptAdventure().call(LuaValue.valueOf("lipt_adventure"), globals);

        // Add Bukkit Server class to global and package.loaded
        final LuaValue serverName = LuaValue.valueOf("server");
        final LuaValue server = CoerceJavaToLua.coerce(Bukkit.getServer());

        globals.set(serverName, server);
        globals.get("package").get("loaded").set(serverName, server);
    }

    private void loadCustomScripts(final Globals globals) throws FileNotFoundException {
        final File[] files = this.scriptDir.listFiles();

        if (files == null) {
            return;
        }

        for (final File file : files) {
            if (file.isDirectory()) {
                this.loadProject(file);
            }

            if (!file.getName().endsWith(".lua")) {
                continue;
            }

            final LuaValue script = globals.load(new FileReader(file), file.getName());

            this.loadedScripts.add(script);
        }
    }

    private void loadProject(final File projectFile) throws FileNotFoundException {
        final File indexFile = new File(projectFile, "index.lua");

        if (!indexFile.exists()) {
            return;
        }

        final Globals projectGlobals = JsePlatform.standardGlobals();
        this.loadDefaultLibraries(projectGlobals);
        projectGlobals.set("__liptpackages__", LuaValue.tableOf());
        // Stolen from https://github.com/Lukkit/Lukkit/blob/19c43f2a33f32d1c67efb6e0508b2cba6074a3ea/src/main/java/nz/co/jammehcow/lukkit/environment/LuaEnvironment.java#L51-L91
        // I was stuck on this for so long...
        projectGlobals.set("require", new OneArgFunction() {
            @SuppressWarnings("ResultOfMethodCallIgnored")
            @Override
            public LuaValue call(LuaValue arg) {
                // Get the path as a Java String
                String path = arg.checkjstring();

                // It's fine to append your path with .lua as it follows Lua standards.
                if (path.startsWith("/")) {
                    path.replaceFirst("/", "");
                }
                if (!path.endsWith(".lua")) {
                    path = path + ".lua";
                }

                // Load the script if it's already in memory for this plugin.
                LuaValue possiblyLoadedScript = projectGlobals.get("__liptpackages__").checktable().get(path);
                // Luaj likes to return nil instead of null
                if (possiblyLoadedScript != null && !possiblyLoadedScript.isnil()) {
                    return possiblyLoadedScript;
                }

                // Get the resource as an InputStream from the plugin's resource getter
                InputStream is;
                try {
                    is = new FileInputStream(new File(projectFile, path));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }

                LuaValue calledScript = projectGlobals.load(new InputStreamReader(is, StandardCharsets.UTF_8), path.replace("/", ".")).call();
                projectGlobals.get("__liptpackages__").checktable().set(path, calledScript);
                return calledScript;

            }
        });

        this.loadedScripts.add(projectGlobals.load(new FileReader(indexFile), indexFile.getName()));
    }

}
