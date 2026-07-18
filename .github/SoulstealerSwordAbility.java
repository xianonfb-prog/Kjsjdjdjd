package com.soulstealer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.UUID;

public class SwordAbilityCommand implements CommandExecutor {

    private final SoulstealerPlugin plugin;
    private static final int MAX_SOULS = 7;
    private static final long COOLDOWN_MILLIS = 180000; // 180 seconds
    private static final long SKELETON_LIFESPAN_TICKS = 1200L; // 60 seconds

    public SwordAbilityCommand(SoulstealerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        // Only the wielder holding the maxed-out sword can trigger this
        if (!SwordManager.isSoulstealerSword(item)) {
            player.sendMessage(ChatColor.RED + "[!] You must be wielding the Soulstealer Sword to use this ability.");
            return true;
        }

        if (SwordManager.getSoulCount(item) < MAX_SOULS) {
            player.sendMessage(ChatColor.RED + "[!] The blade has not yet consumed enough souls.");
            return true;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (plugin.abilityCooldowns.containsKey(uuid) && plugin.abilityCooldowns.get(uuid) > now) {
            long timeLeft = (plugin.abilityCooldowns.get(uuid) - now) / 1000;
            player.sendMessage(ChatColor.RED + "[!] The blade needs time to recharge! (" + timeLeft + "s)");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            return true;
        }

        plugin.abilityCooldowns.put(uuid, now + COOLDOWN_MILLIS);

        PotionEffectType speed = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("speed"));

        Location center = player.getLocation();
        for (int i = 0; i < 8; i++) {
            double angle = (2 * Math.PI / 8) * i;
            double x = center.getX() + (2 * Math.cos(angle));
            double z = center.getZ() + (2 * Math.sin(angle));
            Location spawnLoc = new Location(center.getWorld(), x, center.getY(), z);

            WitherSkeleton skeleton = (WitherSkeleton) player.getWorld().spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);

            skeleton.getPersistentDataContainer().set(plugin.ownerKey, PersistentDataType.STRING, uuid.toString());
            skeleton.setVelocity(new Vector(0, 0.5, 0));

            // Permanent Speed II
            if (speed != null) {
                skeleton.addPotionEffect(new PotionEffect(speed, Integer.MAX_VALUE, 1, false, false, false));
            }

            // Auto-despawn after 1 minute to prevent stacking
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (skeleton.isValid()) {
                    skeleton.remove();
                }
            }, SKELETON_LIFESPAN_TICKS);
        }

        // Visuals & Audio (lightning removed, replaced with particles)
        player.sendMessage(ChatColor.DARK_PURPLE + "[✦] " + ChatColor.GRAY + "The souls of the past answer your call!");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SOUL, center.clone().add(0, 1, 0), 150, 1.5, 1.0, 1.5, 0.05);
        player.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 0.2, 0), 80, 1.0, 0.3, 1.0, 0.02);

        return true;
    }
}
