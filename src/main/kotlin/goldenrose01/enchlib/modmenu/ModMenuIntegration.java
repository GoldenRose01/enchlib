package goldenrose01.enchlib.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Mod Menu integration (source set main, no client imports).
 * - Richiede Cloth Config per mostrare la UI.
 * - Editor per la config GLOBALE: ~/.minecraft/config/enchlib/
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory getModConfigScreenFactory() {
        return new ConfigScreenFactory() {
            @Override
            public Object create(Object parent) {
                if (!FabricLoader.getInstance().isModLoaded("cloth-config")) {
                    return null; // senza Cloth, Mod Menu nasconde il pulsante
                }
                try {
                    return buildGlobalEditor(parent);
                } catch (Throwable t) {
                    return null;
                }
            }
        };
    }

    // --------- Build global editor ---------
    private Object buildGlobalEditor(Object parent) throws Exception {
        // Reflection classes
        Class<?> cfgBuilderCls = Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
        Class<?> textCls = Class.forName("net.minecraft.text.Text");
        Class<?> screenCls = Class.forName("net.minecraft.client.gui.screen.Screen");
        Class<?> entryBuilderCls = Class.forName("me.shedaniel.clothconfig2.api.ConfigEntryBuilder");
        Class<?> abstractEntryCls = Class.forName("me.shedaniel.clothconfig2.api.AbstractConfigListEntry");

        // Kotlin bridge types
        Class<?> bridgeCls = Class.forName("goldenrose01.enchlib.modmenu.ConfigBridge");
        Class<?> snapCls = Class.forName("goldenrose01.enchlib.modmenu.ConfigBridge$Snapshot");
        Class<?> availCls = Class.forName("goldenrose01.enchlib.config.GlobalConfigIO$AvailableEnch");
        Class<?> detailsRootCls = Class.forName("goldenrose01.enchlib.config.GlobalConfigIO$EnchantmentsDetailsRoot");
        Class<?> detailsCls = Class.forName("goldenrose01.enchlib.config.GlobalConfigIO$EnchantmentDetails");

        // Load snapshot (globale)
        Object snapshot = bridgeCls.getMethod("loadSnapshot").invoke(null);

        // builder
        Object builder = cfgBuilderCls.getMethod("create").invoke(null);
        cfgBuilderCls.getMethod("setParentScreen", screenCls).invoke(builder, parent);
        Object title = textCls.getMethod("literal", String.class).invoke(null, "EnchLib \u2013 Config globale");
        cfgBuilderCls.getMethod("setTitle", textCls).invoke(builder, title);
        Object entryBuilder = cfgBuilderCls.getMethod("entryBuilder").invoke(builder);

        // categories
        Object catAvailName = textCls.getMethod("literal", String.class).invoke(null, "Disponibilit\u00E0 Incantesimi");
        Object catAvail = cfgBuilderCls.getMethod("getOrCreateCategory", textCls).invoke(builder, catAvailName);

        Object catDetName = textCls.getMethod("literal", String.class).invoke(null, "Dettagli (max level / categorie)");
        Object catDet = cfgBuilderCls.getMethod("getOrCreateCategory", textCls).invoke(builder, catDetName);

        // Access snapshot fields
        Object availableList = snapCls.getMethod("getAvailable").invoke(snapshot);
        Object detailsRoot = snapCls.getMethod("getDetails").invoke(snapshot);
        Object detailsList = detailsRootCls.getMethod("getEnchantments").invoke(detailsRoot);

        // Toggle per id
        int sizeAvail = ((java.util.List) availableList).size();
        for (int i = 0; i < sizeAvail; i++) {
            Object avail = ((java.util.List) availableList).get(i);
            String id = (String) availCls.getMethod("getId").invoke(avail);
            boolean enabled = (boolean) availCls.getMethod("getEnabled").invoke(avail);

            Object label = textCls.getMethod("literal", String.class).invoke(null, id);
            Object boolBuilder = entryBuilderCls.getMethod("startBooleanToggle", textCls, boolean.class)
                    .invoke(entryBuilder, label, enabled);

            Method setSaveConsumer = boolBuilder.getClass().getMethod("setSaveConsumer", Consumer.class);
            setSaveConsumer.invoke(boolBuilder, (Consumer<Boolean>) v -> {
                try {
                    Object updated = availCls.getConstructor(String.class, boolean.class).newInstance(id, v);
                    ((java.util.List) availableList).set(i, updated);
                } catch (Exception ignored) { }
            });

            Object entry = boolBuilder.getClass().getMethod("build").invoke(boolBuilder);
            catAvail.getClass().getMethod("addEntry", abstractEntryCls).invoke(catAvail, entry);
        }

        // Dettagli per ogni incantesimo
        Method getIdDetails = detailsCls.getMethod("getId");

        int sizeDet = ((java.util.List) detailsList).size();
        for (int i = 0; i < sizeDet; i++) {
            Object det = ((java.util.List) detailsList).get(i);
            String id = (String) getIdDetails.invoke(det);

            // max_level
            int maxLevel = (int) detailsCls.getMethod("getMax_level").invoke(det);
            Object labelMax = textCls.getMethod("literal", String.class).invoke(null, id + " max_level");
            Object intBuilder = entryBuilderCls.getMethod("startIntField", textCls, int.class)
                    .invoke(entryBuilder, labelMax, maxLevel);

            intBuilder.getClass().getMethod("setSaveConsumer", Consumer.class)
                    .invoke(intBuilder, (Consumer<Integer>) v -> {
                        try { detailsCls.getMethod("setMax_level", int.class).invoke(det, Math.max(1, v)); }
                        catch (Exception ignored) { }
                    });

            Object intEntry = intBuilder.getClass().getMethod("build").invoke(intBuilder);
            catDet.getClass().getMethod("addEntry", abstractEntryCls).invoke(catDet, intEntry);

            // mob_category CSV
            java.util.List mobCat = (java.util.List) detailsCls.getMethod("getMob_category").invoke(det);
            String csv = (String) bridgeCls.getMethod("joinCsv", java.util.List.class).invoke(null, mobCat);
            Object labelMob = textCls.getMethod("literal", String.class).invoke(null, id + " mob_category (csv)");
            Object txtBuilder = entryBuilderCls.getMethod("startStrField", textCls, String.class)
                    .invoke(entryBuilder, labelMob, csv);

            txtBuilder.getClass().getMethod("setSaveConsumer", Consumer.class)
                    .invoke(txtBuilder, (Consumer<String>) v -> {
                        try {
                            java.util.List parsed = (java.util.List) bridgeCls.getMethod("parseCsv", String.class).invoke(null, Objects.toString(v, ""));
                            mobCat.clear();
                            mobCat.addAll(parsed);
                        } catch (Exception ignored) { }
                    });

            Object txtEntry = txtBuilder.getClass().getMethod("build").invoke(txtBuilder);
            catDet.getClass().getMethod("addEntry", abstractEntryCls).invoke(catDet, txtEntry);
        }

        // Save globale
        cfgBuilderCls.getMethod("setSavingRunnable", Runnable.class).invoke(builder, (Runnable) () -> {
            try {
                bridgeCls.getMethod("saveSnapshot", snapCls).invoke(null, snapshot);
            } catch (Exception ignored) { }
        });

        return cfgBuilderCls.getMethod("build").invoke(builder);
    }
}
