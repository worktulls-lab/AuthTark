package com.tarkmull.auth.manager;

import com.tarkmull.auth.AuthPlugin;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Простая математическая капча против ботов при регистрации.
 * Пока пример не решён — /register недоступен.
 */
public class CaptchaManager {

    public enum VerifyResult { CORRECT, WRONG, EXCEEDED }

    private final AuthPlugin plugin;
    private final Random random = new Random();

    private final Map<UUID, Integer> answers = new ConcurrentHashMap<>();
    private final Map<UUID, String> questions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();

    public CaptchaManager(AuthPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("captcha.enabled", true);
    }

    /** Генерирует новый пример для игрока и запоминает ожидаемый ответ. */
    public String generate(Player player) {
        int min = plugin.getConfig().getInt("captcha.min-number", 1);
        int max = Math.max(min + 1, plugin.getConfig().getInt("captcha.max-number", 20));

        int a = min + random.nextInt(max - min + 1);
        int b = min + random.nextInt(max - min + 1);

        int answer;
        String question;
        if (random.nextBoolean()) {
            answer = a + b;
            question = a + " + " + b;
        } else {
            if (a < b) {
                int tmp = a;
                a = b;
                b = tmp;
            }
            answer = a - b;
            question = a + " - " + b;
        }

        UUID uuid = player.getUniqueId();
        answers.put(uuid, answer);
        questions.put(uuid, question);
        attempts.remove(uuid);
        return question;
    }

    public boolean isRequired(UUID uuid) {
        return answers.containsKey(uuid);
    }

    public String getQuestion(UUID uuid) {
        return questions.getOrDefault(uuid, "?");
    }

    public VerifyResult verify(Player player, String rawAnswer) {
        UUID uuid = player.getUniqueId();
        Integer expected = answers.get(uuid);
        if (expected == null) {
            return VerifyResult.CORRECT; // решать нечего — не блокируем
        }

        Integer given = parseIntOrNull(rawAnswer);
        if (given != null && given.intValue() == expected.intValue()) {
            answers.remove(uuid);
            questions.remove(uuid);
            attempts.remove(uuid);
            return VerifyResult.CORRECT;
        }

        int max = plugin.getConfig().getInt("captcha.max-attempts", 5);
        int count = attempts.merge(uuid, 1, Integer::sum);
        if (count >= max) {
            clear(uuid);
            return VerifyResult.EXCEEDED;
        }

        generate(player); // новый пример на каждую ошибку — не дать подобрать перебором
        return VerifyResult.WRONG;
    }

    public void clear(UUID uuid) {
        answers.remove(uuid);
        questions.remove(uuid);
        attempts.remove(uuid);
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
