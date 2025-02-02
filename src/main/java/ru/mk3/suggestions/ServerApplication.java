package ru.mk3.suggestions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.mk3.suggestions.bot.SuggestionsBot;
import ru.mk3.suggestions.properties.BotConfig;

@Slf4j
@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new SuggestionsBot(BotConfig.BOT_TOKEN, BotConfig.BOT_USERNAME));

            log.info("Bot has started!");
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

}