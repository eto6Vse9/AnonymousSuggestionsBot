package ru.mk3.suggestions.util;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

public class Buttons {

    public static final InlineKeyboardMarkup CHECK_SUBSCRIPTION_MARKUP;
    public static final InlineKeyboardMarkup ANOTHER_ADMIN_ACTION_MARKUP;

    static {
        CHECK_SUBSCRIPTION_MARKUP = createKeyboard(
                InlineKeyboardButton.builder()
                        .text("Проверить")
                        .callbackData("check_subscription")
                        .build()
        );

        ANOTHER_ADMIN_ACTION_MARKUP = createKeyboard(
                InlineKeyboardButton.builder()
                        .text("Недоступно")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("\uD83D\uDDD1")
                        .callbackData("delete")
                        .build()
        );
    }

    private Buttons() {
        throw new IllegalStateException("Utility class");
    }

    public static InlineKeyboardMarkup createPostMarkup(Integer id) {
        return createKeyboard(
                InlineKeyboardButton.builder()
                        .text("✔️")
                        .callbackData("publish:" + id)
                        .build(),
                InlineKeyboardButton.builder()
                        .text("\uD83D\uDDD1")
                        .callbackData("delete:" + id)
                        .build()
        );
    }

    private static InlineKeyboardMarkup createKeyboard(InlineKeyboardButton... buttons) {
        return createKeyboard(List.of(Arrays.asList(buttons)));
    }

    private static InlineKeyboardMarkup createKeyboard(List<List<InlineKeyboardButton>> buttons) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        markupInline.setKeyboard(buttons);
        return markupInline;
    }
}