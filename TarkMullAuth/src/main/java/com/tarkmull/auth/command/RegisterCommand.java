package com.tarkmull.auth.command;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.manager.AuthManager;
import com.tarkmull.auth.util.AuthFlow;
import com.tarkmull.auth.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {

    private final AuthPlugin plugin;

    public RegisterCommand(AuthPlugin plugin) {
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

        if (auth.isRegistered(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.already-registered"));
            return true;
        }

        if (plugin.getCaptchaManager().isRequired(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.captcha-required")
                    .replace("%question%", plugin.getCaptchaManager().getQuestion(player.getUniqueId())));
            return true;
        }

        if (args.length != 2) {
            Msg.send(player, plugin.getConfig().getString("messages.register-usage"));
            return true;
        }

        String pass1 = args[0];
        String pass2 = args[1];
        int minLength = plugin.getConfig().getInt("min-password-length", 4);

        if (!pass1.equals(pass2)) {
            Msg.send(player, plugin.getConfig().getString("messages.password-mismatch"));
            return true;
        }

        if (pass1.length() < minLength) {
            Msg.send(player, plugin.getConfig().getString("messages.password-too-short")
                    .replace("%min%", String.valueOf(minLength)));
            return true;
        }

        if (pass1.equalsIgnoreCase(player.getName())) {
            Msg.send(player, plugin.getConfig().getString("messages.password-too-simple"));
            return true;
        }

        auth.register(player, pass1);
        AuthFlow.completeAuth(plugin, player, true);
        Msg.send(player, plugin.getConfig().getString("messages.register-success"));
        return true;
    }
}
