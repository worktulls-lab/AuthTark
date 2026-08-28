package com.tarkmull.auth.command;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.manager.AuthManager;
import com.tarkmull.auth.util.AuthFlow;
import com.tarkmull.auth.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {

    private final AuthPlugin plugin;

    public LoginCommand(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда доступна только игрокам.");
            return true;
        }

        AuthManager auth = plugin.getAuthManager();

        if (auth.isAuthenticated(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.already-authenticated"));
            return true;
        }

        if (!auth.isRegistered(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.not-registered"));
            return true;
        }

        if (args.length != 1) {
            Msg.send(player, plugin.getConfig().getString("messages.login-usage"));
            return true;
        }

        if (!auth.checkPassword(player, args[0])) {
            int attempts = auth.registerFailedAttempt(player.getUniqueId());
            int max = plugin.getConfig().getInt("login.max-attempts", 5);
            if (attempts >= max) {
                player.kick(Msg.parse(plugin.getConfig().getString("messages.kick-attempts")));
                return true;
            }
            Msg.send(player, plugin.getConfig().getString("messages.wrong-password")
                    .replace("%left%", String.valueOf(max - attempts)));
            return true;
        }

        AuthFlow.completeAuth(plugin, player, true);
        Msg.send(player, plugin.getConfig().getString("messages.login-success"));
        return true;
    }
}
