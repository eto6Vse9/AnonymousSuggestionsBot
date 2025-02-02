package ru.mk3.suggestions;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.mk3.suggestions.cache.CachedUser;
import ru.mk3.suggestions.cache.UserCacheManager;
import ru.mk3.suggestions.properties.MessageConfig;
import ru.mk3.suggestions.util.Buttons;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Getter
@RequiredArgsConstructor
@Component
public class SuggestionsBot extends TelegramLongPollingBot {

    @Value("${bot.token}")
    private String botToken;

    @Value("${bot.username}")
    private String botUsername;

    @Value("${bot.admins}")
    private List<String> botAdmins;

    @Value("${bot.target-channel}")
    private String targetChannel;

    @Autowired
    private MessageConfig messageConfig;

    @Autowired
    private UserCacheManager userCacheManager;

    @PostConstruct
    public void init() throws TelegramApiException {
        new TelegramBotsApi(DefaultBotSession.class).registerBot(this);
    }

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
        CachedUser cachedUser = userCacheManager.getCachedUserByTelegramId(chatId);

        if (cachedUser == null) {
            cachedUser = userCacheManager.createCachedUser(chatId);
            cachedUser.setSubscribed(isUserMemberOfChannel(chatId));

            userCacheManager.updateUser(cachedUser);
        } else if (!userCacheManager.canSendMessage(cachedUser)) {
            sendTextMessage(chatId, messageConfig.getLimitExceededMessage());
            return;
        } else if (userCacheManager.shouldCheckSubscription(cachedUser)) {
            cachedUser.setSubscribed(isUserMemberOfChannel(chatId));
            cachedUser.setLastSubscriptionCheck(LocalDateTime.now());

            userCacheManager.updateUser(cachedUser);
        }

        if (!cachedUser.isSubscribed()) {
            sendTextMessage(chatId, messageConfig.getMustBeMemberMessage(), Buttons.CHECK_SUBSCRIPTION_MARKUP);
            return;
        }

        String text = message.getText();
        if (text != null && text.equalsIgnoreCase("/start")) {
            sendTextMessage(chatId, messageConfig.getStartMessage());
            return;
        }

        forwardMessageToAdmins(message);
        sendTextMessage(chatId, messageConfig.getConfirmationMessage());

        cachedUser.incrementMessageCount();
        userCacheManager.updateUser(cachedUser);
    }

    private void handleCallbackQuery(CallbackQuery callback) {
        MaybeInaccessibleMessage message = callback.getMessage();

        if (message != null) {
            Long adminChatId = message.getChatId();
            Integer messageId = message.getMessageId();
            String callbackData = callback.getData();

            if (callbackData.equals("check_subscription")) {
                Long userId = callback.getFrom().getId();
                CachedUser cachedUser = userCacheManager.getCachedUserByTelegramId(userId);

                if (cachedUser == null) {
                    cachedUser = userCacheManager.createCachedUser(userId);
                } else if (!userCacheManager.canCheckSubscription(cachedUser)) {
                    sendTextMessage(userId, messageConfig.getSubscriptionCheckLimitExceededMessage());
                    return;
                }

                boolean subscribed = isUserMemberOfChannel(userId);

                cachedUser.setLastSubscriptionCheck(LocalDateTime.now());
                cachedUser.setSubscribed(subscribed);
                cachedUser.incrementSubscriptionCheckCount();
                userCacheManager.updateUser(cachedUser);

                if (!subscribed) {
                    return;
                }
            } else if (botAdmins.contains(adminChatId.toString())) {
                if (callbackData.equals("publish")) {
                    CopyMessage copyMessage = new CopyMessage();
                    copyMessage.setFromChatId(adminChatId);
                    copyMessage.setMessageId(messageId);
                    copyMessage.setChatId(targetChannel);

                    if (send(copyMessage) == null) {
                        return;
                    }
                } else if (!callbackData.equals("delete")) {
                    return;
                }
            } else {
                return;
            }

            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(adminChatId);
            deleteMessage.setMessageId(messageId);
            send(deleteMessage);
        }
    }

    private boolean isUserMemberOfChannel(Long userId) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(targetChannel);
        getChatMember.setUserId(userId);

        try {
            ChatMember chatMember = execute(getChatMember);
            return chatMember != null &&
                    !chatMember.getStatus().equals("left") && !chatMember.getStatus().equals("kicked");
        } catch (TelegramApiException e) {
            log.error("Error checking channel membership", e);
            return false;
        }
    }

    private void sendTextMessage(Long chatId, String text) {
        sendTextMessage(chatId, text, null);
    }

    private void sendTextMessage(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setParseMode(ParseMode.MARKDOWN);
        sendMessage.setReplyMarkup(markup);
        send(sendMessage);
    }

    private void forwardMessageToAdmins(Message message) {
        CopyMessage copyMessage = new CopyMessage();

        copyMessage.setFromChatId(message.getChatId());
        copyMessage.setMessageId(message.getMessageId());
        copyMessage.setReplyMarkup(Buttons.POST_MARKUP);

        for (String botAdminId : botAdmins) {
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