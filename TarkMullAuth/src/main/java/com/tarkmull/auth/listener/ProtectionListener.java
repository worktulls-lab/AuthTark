package com.tarkmull.auth.listener;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.util.Msg;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.List;
import java.util.Locale;

/**
 * Пока игрок не авторизован: не двигается, не пишет в чат, не выполняет
 * посторонние команды, не получает урон/голод и не взаимодействует с миром.
 */
public class ProtectionListener implements Listener {

    private static final List<String> ALLOWED_COMMANDS = List.of("/login", "/register", "/l", "/reg", "/captcha", "/cap");

    private final AuthPlugin plugin;

    public ProtectionListener(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isLocked(Player player) {
        return !plugin.getAuthManager().isAuthenticated(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        if (!isLocked(event.getPlayer())) {
            return;
        }
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!isLocked(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        Msg.send(event.getPlayer(), plugin.getConfig().getString("messages.chat-blocked"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!isLocked(event.getPlayer())) {
            return;
        }
        String cmd = event.getMessage().toLowerCase(Locale.ROOT).split(" ")[0];
        if (!ALLOWED_COMMANDS.contains(cmd)) {
            event.setCancelled(true);
            Msg.send(event.getPlayer(), plugin.getConfig().getString("messages.command-blocked"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && isLocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && isLocked(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (isLocked(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && isLocked(player)) {
            event.setCancelled(true);
        }
    }
}
