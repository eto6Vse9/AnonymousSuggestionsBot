package ru.mk3.suggestions.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.List;

public class Buttons {

    public static final InlineKeyboardMarkup POST_MARKUP;

    static {
        POST_MARKUP = createKeyboard(
                InlineKeyboardButton.builder()
                        .text("✔️")
                        .callbackData("publish")
                        .build(),
                InlineKeyboardButton.builder()
                        .text("\uD83D\uDDD1")
                        .callbackData("delete")
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