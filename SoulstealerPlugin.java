package com.soulstealer;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulstealerPlugin extends JavaPlugin {

    public NamespacedKey swordKey;
    public NamespacedKey soulsKey;
    public NamespacedKey craftedKey;
    public NamespacedKey recipeKey;
    public NamespacedKey ownerKey;

    public Map<UUID, Long> abilityCooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        swordKey = new NamespacedKey(this, "soulstealer_sword");
        soulsKey = new NamespacedKey(this, "souls");
        craftedKey = new NamespacedKey(this, "has_crafted");
        recipeKey = new NamespacedKey(this, "soulstealer_recipe");
        ownerKey = new NamespacedKey(this, "ss_owner");

        getServer().getPluginManager().registerEvents(new SwordListener(this), this);
        getCommand("soulstealer").setExecutor(new SwordCommand(this));
        getCommand("ability").setExecutor(new SwordAbilityCommand(this));

        SwordManager.registerRecipe(this);

        getLogger().info("Soulstealer Plugin Enabled!");
    }
}
