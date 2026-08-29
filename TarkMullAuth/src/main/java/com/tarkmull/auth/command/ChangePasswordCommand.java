package com.tarkmull.auth.command;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.manager.AuthManager;
import com.tarkmull.auth.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangePasswordCommand implements CommandExecutor {

    private final AuthPlugin plugin;

    public ChangePasswordCommand(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда доступна только игрокам.");
            return true;
        }

        AuthManager auth = plugin.getAuthManager();

        if (!auth.isRegistered(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.not-registered"));
            return true;
        }

        if (!auth.isAuthenticated(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.must-login-first"));
            return true;
        }

        if (args.length != 3) {
            Msg.send(player, plugin.getConfig().getString("messages.changepassword-usage"));
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];
        String newPasswordConfirm = args[2];

        if (!auth.checkPassword(player, oldPassword)) {
            Msg.send(player, plugin.getConfig().getString("messages.wrong-old-password"));
            return true;
        }

        if (!newPassword.equals(newPasswordConfirm)) {
            Msg.send(player, plugin.getConfig().getString("messages.password-mismatch"));
            return true;
        }

        int minLength = plugin.getConfig().getInt("min-password-length", 4);
        if (newPassword.length() < minLength) {
            Msg.send(player, plugin.getConfig().getString("messages.password-too-short")
                    .replace("%min%", String.valueOf(minLength)));
            return true;
        }

        if (newPassword.equalsIgnoreCase(player.getName())) {
            Msg.send(player, plugin.getConfig().getString("messages.password-too-simple"));
            return true;
        }

        if (newPassword.equals(oldPassword)) {
            Msg.send(player, plugin.getConfig().getString("messages.password-same-as-old"));
            return true;
        }

        auth.changePassword(player, newPassword);
        Msg.send(player, plugin.getConfig().getString("messages.changepassword-success"));
        return true;
    }
}
