package ru.mk3.suggestions.bot;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mk3.suggestions.config.BotConfig;
import ru.mk3.suggestions.config.MessageConfig;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@RequiredArgsConstructor
public class SuggestionsBot extends TelegramLongPollingBot {

    private static final Cache<Long, Boolean> membershipCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private final String botToken;
    private final String botUsername;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }

        Message message = update.getMessage();
        if (message == null) {
            return;
        }

        User user = message.getFrom();
        if (user == null || user.getIsBot()) {
            return;
        }

        Long chatId = message.getChatId();
        if (!isUserMemberOfChannel(chatId)) {
            sendTextMessage(chatId, MessageConfig.MUST_BE_MEMBER);
            return;
        }

        String text = message.getText();
        if (text != null && text.equalsIgnoreCase("/start")) {
            sendTextMessage(chatId, MessageConfig.START_MESSAGE);
            return;
        }

        forwardMessageToAdmins(message);
        sendTextMessage(chatId, MessageConfig.CONFIRMATION_MESSAGE);
    }

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage message = callbackQuery.getMessage();

        if (message != null) {
            String callback = callbackQuery.getData();
            Long adminChatId = message.getChatId();
            int messageId = message.getMessageId();

            if (callback.equals("publish")) {
                CopyMessage copyMessage = new CopyMessage();
                copyMessage.setFromChatId(adminChatId);
                copyMessage.setMessageId(messageId);

                copyMessage.setChatId(BotConfig.TARGET_CHANNEL);

                if (send(copyMessage) != null) {
                    DeleteMessage deleteMessage = new DeleteMessage();
                    deleteMessage.setChatId(adminChatId);
                    deleteMessage.setMessageId(messageId);

                    send(deleteMessage);

                    AnswerCallbackQuery answer = new AnswerCallbackQuery();
                    answer.setCallbackQueryId(callbackQuery.getId());
                    answer.setText("Опубликовано!");

                    send(answer);
                }
            } else if (callback.equals("delete")) {
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(adminChatId);
                deleteMessage.setMessageId(messageId);
                send(deleteMessage);
            }
        }
    }

    private boolean isUserMemberOfChannel(Long userId) {
        Boolean cachedResult = membershipCache.getIfPresent(userId);
        if (cachedResult != null) {
            return cachedResult;
        }

        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(BotConfig.TARGET_CHANNEL);
        getChatMember.setUserId(userId);

        try {
            ChatMember chatMember = execute(getChatMember);
            boolean isMember = chatMember != null && !chatMember.getStatus().equals("left") && !chatMember.getStatus().equals("kicked");

            membershipCache.put(userId, isMember);

            return isMember;
        } catch (TelegramApiException e) {
            log.error("Error checking channel membership", e);
            return false;
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.MARKDOWN);
        send(sendMessage);
    }

    private void forwardMessageToAdmins(Message message) {
        CopyMessage copyMessage = new CopyMessage();

        copyMessage.setFromChatId(message.getChatId());
        copyMessage.setMessageId(message.getMessageId());
        copyMessage.setReplyMarkup(Buttons.POST_MARKUP);

        for (String botAdminId : BotConfig.BOT_ADMINS) {
            copyMessage.setChatId(botAdminId);
            send(copyMessage);
        }
    }

    public <T extends Serializable, Method extends BotApiMethod<T>> T send(Method method) {
        try {
            return execute(method);
        } catch (TelegramApiException e) {
            log.error("Error executing Telegram API method", e);
            return null;
        }
    }

    public List<Message> send(SendMediaGroup method) {
        try {
            return execute(method);
        } catch (TelegramApiException e) {
            log.error("Error executing Telegram API method", e);
            return null;
        }
    }
}