package ru.mk3.suggestions;

import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.mk3.suggestions.config.BotConfig;

@Slf4j
public class ServerApplication {

    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new SuggestionsBot(BotConfig.BOT_TOKEN, BotConfig.BOT_USERNAME));

            log.info("Bot has started!");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}