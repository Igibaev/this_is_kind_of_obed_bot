package kz.aday.bot.util;

import javax.inject.Singleton;
import java.util.Random;

@Singleton
public class StringUtils {

    public static String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }

        return text.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("`", "\\`")
                .replace("[", "\\[");
    }
}
