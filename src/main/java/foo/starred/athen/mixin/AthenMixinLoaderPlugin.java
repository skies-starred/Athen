package foo.starred.athen.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.net.JarURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class AthenMixinLoaderPlugin implements IMixinConfigPlugin {
    private String pack = "foo.starred.athen.mixin";

    @Override
    public void onLoad(String mixinPackage) {
        this.pack = mixinPackage;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        var mixins = new TreeSet<String>();
        var path = pack.replace('.', '/');

        try {
            var resources = getClass().getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                var url = resources.nextElement();

                if ("file".equals(url.getProtocol())) {
                    directory(Path.of(url.toURI()), mixins);
                    continue;
                }

                if ("jar".equals(url.getProtocol())) {
                    jar((JarURLConnection) url.openConnection(), path, mixins);
                }
            }
        } catch (Exception _) {}

        return new ArrayList<>(mixins);
    }

    private void directory(Path dir, Set<String> mixins) throws IOException {
        if (!Files.isDirectory(dir)) return;

        try (var files = Files.walk(dir)) {
            files.filter(Files::isRegularFile).map(dir::relativize).map(Path::toString).filter(this::mixin).map(this::klass).forEach(mixins::add);
        }
    }

    private void jar(JarURLConnection connection, String path, Set<String> mixins) throws IOException {
        try (var jar = connection.getJarFile()) {
            var entries = jar.entries();

            while (entries.hasMoreElements()) {
                var name = entries.nextElement().getName();
                if (!name.startsWith(path + "/") || !mixin(name)) continue;

                mixins.add(name.substring(path.length() + 1, name.length() - 6).replace('/', '.'));
            }
        }
    }

    private String klass(String path) {
        return path.substring(0, path.length() - 6).replace('\\', '/').replace('/', '.');
    }

    private boolean mixin(String path) {
        return path.endsWith(".class") && !path.contains("$") && !path.endsWith("AthenMixinLoaderPlugin.class") && loaded(path);
    }

    private boolean loaded(String path) {
        String s = path.replace('\\', '/').replace('.', '/');
        int i = s.indexOf("compat/");
        return i == -1 || FabricLoader.getInstance().isModLoaded(s.substring(i + 7).split("/")[0]);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
