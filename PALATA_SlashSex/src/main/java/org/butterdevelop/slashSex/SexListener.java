package org.butterdevelop.slashSex;

import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SexListener implements Listener {
    private final SlashSex plugin;
    private final Map<UUID, Long> personalCooldown = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private final Map<UUID, Long> lastUseAttempt = new HashMap<>();

    private final long personalCooldownMs;
    private final long damageCooldownMs;
    private final long spamThresholdMs;

    public SexListener(SlashSex plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        // время личного кулдауна
        long personalSec = plugin.getConfig().getLong("personal-cooldown-seconds", 60);
        this.personalCooldownMs = personalSec * 1000L;
        // время после получения урона
        long damageSec = plugin.getConfig().getLong("damage-cooldown-seconds", 30);
        this.damageCooldownMs = damageSec * 1000L;
        // порог спама использования
        this.spamThresholdMs = plugin.getConfig().getLong("spam-threshold-ms", 1000);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) {
            lastDamageTime.put(p.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        Player user = e.getPlayer();
        UUID uuid = user.getUniqueId();
        long now = System.currentTimeMillis();

        // анти-спам: быстрое повторное нажатие
        Long lastAttempt = lastUseAttempt.get(uuid);
        if (lastAttempt != null && now - lastAttempt < spamThresholdMs) {
            return; // тихо игнорируем
        }
        lastUseAttempt.put(uuid, now);

        // Проверка предмета по lore
        ItemStack item = user.getInventory().getItemInMainHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<String> lore = meta.hasLore() ? meta.getLore() : null;
        boolean isNormal = lore != null && lore.stream().anyMatch(l -> l.contains("Забавная игрушка"));
        boolean isStrong = lore != null && lore.stream().anyMatch(l -> l.contains("Усиленная версия"));
        if (!isNormal && !isStrong) return;

        // Личный кулдаун
        Long readyTime = personalCooldown.get(uuid);
        if (readyTime != null && now < readyTime) {
            long secsLeft = ((readyTime - now) + 999) / 1000;
            user.sendMessage(ChatColor.RED + "Подождите еще " + secsLeft + " сек перед следующим использованием!");
            return;
        }
        // Проверка урона за заданный период
        Long lastHit = lastDamageTime.get(uuid);
        if (lastHit != null && now - lastHit < damageCooldownMs) {
            long wait = ((lastHit + damageCooldownMs - now) + 999) / 1000;
            user.sendMessage(ChatColor.RED + "Вы получили урон недавно. Подождите еще " + wait + " сек.");
            return;
        }

        // Расход предмета и установка кулдауна
        item.setAmount(item.getAmount() - 1);
        personalCooldown.put(uuid, now + personalCooldownMs);

        Location targetLoc = user.getLocation();
        assert targetLoc.getWorld() != null;
        targetLoc.getWorld().playSound(targetLoc, Sound.BLOCK_ANVIL_BREAK, SoundCategory.PLAYERS, 1f, 1f);
        targetLoc.getWorld().playSound(targetLoc, "slashsex.dildo", SoundCategory.PLAYERS, 0.3f, 1f);

        // Частицы и звук
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count++ >= 28 * 2) { cancel(); return; }
                Location loc = target.getLocation().add(0, 1, 0);
                Objects.requireNonNull(loc.getWorld()).spawnParticle(Particle.CLOUD, loc, 30, 0.5, 1, 0.5, 0.1);

                // Спавним невоспринимаемое ведро молока под игроком
                Location dropLoc = target.getLocation().add(0, 0.5, 0);
                Item milk = loc.getWorld().dropItem(dropLoc, new ItemStack(Material.MILK_BUCKET));
                milk.setPickupDelay(Integer.MAX_VALUE);   // никто не сможет подобрать
                milk.setInvulnerable(true);               // никакие атаки/воронки не сломают

                // Авто-удаление через 10 секунд (200 тиков), чтобы не засорять мир
                Bukkit.getScheduler().runTaskLater(plugin, milk::remove, 10 * 20L);
            }
        }.runTaskTimer(plugin, 0L, 10L);

        // Баффы
        int amp = isNormal ? 0 : 1;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30*20, amp));
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 30*20, amp));
        target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20, amp));
        target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 15*20, amp));
        user.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10*20, 0));

        Scoreboard board = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team tUser = board.getEntryTeam(user.getName());
        Team tTarget = board.getEntryTeam(target.getName());
        ChatColor colorUser = (tUser != null ? tUser.getColor() : ChatColor.WHITE);
        ChatColor colorTarget = (tTarget != null ? tTarget.getColor() : ChatColor.WHITE);
        String userName = colorUser + user.getName();
        String targetName = colorTarget + target.getName();

        String chatMsg = userName + ChatColor.WHITE + " трахнул " + targetName;
        Bukkit.broadcastMessage(chatMsg);

        String userBar = ChatColor.WHITE + "Вы трахнули " + targetName;
        user.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacy(userBar));

        String targetBar = userName + ChatColor.WHITE + " трахнул вас";
        target.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacy(targetBar));

        // Статистика
        StatsManager.get().recordUse(uuid, target.getUniqueId());
    }
}