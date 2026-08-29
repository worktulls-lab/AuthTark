package com.tarkmull.auth.command;

import com.tarkmull.auth.AuthPlugin;
import com.tarkmull.auth.manager.AuthManager;
import com.tarkmull.auth.manager.CaptchaManager;
import com.tarkmull.auth.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CaptchaCommand implements CommandExecutor {

    private final AuthPlugin plugin;

    public CaptchaCommand(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Эта команда доступна только игрокам.");
            return true;
        }

        AuthManager auth = plugin.getAuthManager();
        CaptchaManager captcha = plugin.getCaptchaManager();

        if (auth.isAuthenticated(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.already-authenticated"));
            return true;
        }

        if (!captcha.isRequired(player.getUniqueId())) {
            Msg.send(player, plugin.getConfig().getString("messages.captcha-not-needed"));
            return true;
        }

        if (args.length != 1) {
            Msg.send(player, plugin.getConfig().getString("messages.captcha-usage"));
            return true;
        }

        CaptchaManager.VerifyResult result = captcha.verify(player, args[0]);
        switch (result) {
            case CORRECT -> Msg.send(player, plugin.getConfig().getString("messages.captcha-correct"));
            case WRONG -> Msg.send(player, plugin.getConfig().getString("messages.captcha-wrong")
                    .replace("%question%", captcha.getQuestion(player.getUniqueId())));
            case EXCEEDED -> player.kick(Msg.parse(plugin.getConfig().getString("messages.captcha-kick")));
        }
        return true;
    }
}
