package com.soulstealer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SwordListener implements Listener {

    private final SoulstealerPlugin plugin;

    public SwordListener(SoulstealerPlugin plugin) {
        this.plugin = plugin;
    }

    // --- KILL & CRAFT LOGIC ---

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack mainHand = killer.getInventory().getItemInMainHand();
        if (!SwordManager.isSoulstealerSword(mainHand)) return;

        int souls = SwordManager.getSoulCount(mainHand);

        if (souls >= 8) {
            killer.sendMessage(ChatColor.RED + "[!] The sword cannot consume any more souls!");
            killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
            return;
        }

        souls++;
        SwordManager.setSoulCount(mainHand, souls);

        PotionEffectType strength = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("strength"));
        if (strength != null) {
            killer.addPotionEffect(new PotionEffect(strength, 100, 1, false, true, true));
        }

        killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
        killer.sendMessage(ChatColor.AQUA + "[✦] " + ChatColor.GRAY + "A soul has been consumed!");
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() == null) return;
        if (!(event.getRecipe() instanceof Keyed keyedRecipe)) return;
        if (!keyedRecipe.getKey().equals(plugin.recipeKey)) return;

        Player player = (Player) event.getView().getPlayer();

        if (player.getPersistentDataContainer().has(plugin.craftedKey, PersistentDataType.BYTE)) {
            event.getInventory().setResult(new ItemStack(org.bukkit.Material.AIR));
            player.sendMessage(ChatColor.RED + "[!] You have already claimed this blade's power.");
            return;
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(ChatColor.DARK_PURPLE + "[" + ChatColor.LIGHT_PURPLE + "SOULSTEALER" + ChatColor.DARK_PURPLE + "] " + ChatColor.GRAY + ChatColor.ITALIC + "A new blade has been forged...");
            online.sendMessage(ChatColor.DARK_PURPLE + "  " + ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.DARK_GRAY + ChatColor.ITALIC + "The Soulstealer Sword awakens, hungering for souls...");
        }
        Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f));

        player.getPersistentDataContainer().set(plugin.craftedKey, PersistentDataType.BYTE, (byte) 1);
    }

    // --- SHARPNESS 8 ABILITY LOGIC ---

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;

        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!SwordManager.isSoulstealerSword(item)) return;
        if (SwordManager.getSoulCount(item) < 8) return;

        // Check Cooldown (180 seconds)
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (plugin.abilityCooldowns.containsKey(uuid) && plugin.abilityCooldowns.get(uuid) > now) {
            long timeLeft = (plugin.abilityCooldowns.get(uuid) - now) / 1000;
            player.sendMessage(ChatColor.RED + "[!] The blade needs time to recharge! (" + timeLeft + "s)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return;
        }

        // Set Cooldown to 180 seconds (180,000 milliseconds)
        plugin.abilityCooldowns.put(uuid, now + 180000);

        // Spawn 8 Wither Skeletons in a circle
        Location center = player.getLocation();
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI / 8) * i;
            double x = center.getX() + (2 * Math.cos(angle));
            double z = center.getZ() + (2 * Math.sin(angle));
            Location spawnLoc = new Location(center.getWorld(), x, center.getY(), z);

            WitherSkeleton skeleton = (WitherSkeleton) player.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);

            // Tag them with the owner's UUID so our AI logic knows who to ignore
            skeleton.getPersistentDataContainer().set(plugin.ownerKey, PersistentDataType.STRING, uuid.toString());

            // Push them slightly upward so they don't suffocate in blocks
            skeleton.setVelocity(new Vector(0, 0.5, 0));
        }

        // Visuals & Audio
        player.sendMessage(ChatColor.DARK_PURPLE + "[✦] " + ChatColor.GRAY + "The souls of the past answer your call!");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        player.getWorld().strikeLightning(center); // Cool visual effect
    }

    // --- AI PROTECTION LOGIC ---

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getEntity() instanceof WitherSkeleton)) return;

        WitherSkeleton skeleton = (WitherSkeleton) event.getEntity();
        if (!skeleton.getPersistentDataContainer().has(plugin.ownerKey, PersistentDataType.STRING)) return;

        String ownerUuidStr = skeleton.getPersistentDataContainer().get(plugin.ownerKey, PersistentDataType.STRING);
        if (ownerUuidStr == null) return;

        UUID ownerUuid = UUID.fromString(ownerUuidStr);

        // If the skeleton is trying to target its owner, cancel it!
        if (event.getTarget() != null && event.getTarget().getUniqueId().equals(ownerUuid)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof WitherSkeleton)) return;

        WitherSkeleton skeleton = (WitherSkeleton) event.getDamager();
        if (!skeleton.getPersistentDataContainer().has(plugin.ownerKey, PersistentDataType.STRING)) return;

        String ownerUuidStr = skeleton.getPersistentDataContainer().get(plugin.ownerKey, PersistentDataType.STRING);
        if (ownerUuidStr == null) return;

        UUID ownerUuid = UUID.fromString(ownerUuidStr);

        // If the skeleton somehow damages its owner, cancel the damage
        if (event.getEntity().getUniqueId().equals(ownerUuid)) {
            event.setCancelled(true);
        }
    }
}
