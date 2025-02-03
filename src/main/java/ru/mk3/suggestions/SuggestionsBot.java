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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.mk3.suggestions.cache.CachedUser;
import ru.mk3.suggestions.cache.UserCacheManager;
import ru.mk3.suggestions.model.AnonymousMessage;
import ru.mk3.suggestions.property.MessagesProperty;
import ru.mk3.suggestions.repository.AnonymousMessageRepository;
import ru.mk3.suggestions.util.Buttons;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
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
    private MessagesProperty messagesProperty;

    @Autowired
    private UserCacheManager userCacheManager;

    @Autowired
    private AnonymousMessageRepository anonymousMessageRepository;

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

        if (cachedUser.getSubscriptionCheckCount() == 0) {
            checkIsMemberOfChannel(cachedUser);
        }

        if (!cachedUser.isSubscribed()) {
            sendTextMessage(chatId, messagesProperty.getMustBeMember(), Buttons.CHECK_SUBSCRIPTION_MARKUP);
            return;
        }

        String text = message.getText();
        if (text != null && text.equalsIgnoreCase("/start")) {
            sendTextMessage(chatId, messagesProperty.getStart());
            return;
        }

        if (!userCacheManager.canSendMessage(cachedUser)) {
            sendTextMessage(chatId, messagesProperty.getLimitExceeded());
            return;
        }

        forwardMessageToAdmins(message);
        sendTextMessage(chatId, messagesProperty.getConfirmation());

        cachedUser.incrementMessageCount();
        cachedUser.setLastMessageTime(LocalDateTime.now());
        userCacheManager.updateUser(cachedUser);
    }

    private void handleCallbackQuery(CallbackQuery callback) {
        MaybeInaccessibleMessage message = callback.getMessage();

        if (message != null) {
            String callbackData = callback.getData();
            String callbackId = callback.getId();

            Long chatId = message.getChatId();
            Integer messageId = message.getMessageId();

            if (callbackData.equals("check_subscription")) {
                CachedUser cachedUser = userCacheManager.getCachedUserByTelegramId(chatId);

                if (!cachedUser.isSubscribed()) {
                    if (!userCacheManager.canCheckSubscription(cachedUser)) {
                        sendAnswerCallback(callbackId, messagesProperty.getSubscriptionCheckLimitExceeded());
                        return;
                    }

                    if (!checkIsMemberOfChannel(cachedUser)) {
                        sendAnswerCallback(callbackId, messagesProperty.getStillNotSubscribed());
                        return;
                    }

                    sendTextMessage(chatId, messagesProperty.getStart());
                }
            } else if (botAdmins.contains(chatId.toString())) {
                String[] args = callbackData.split(":");
                String action = args[0];

                AnonymousMessage anonymousMessage = null;
                if (args.length > 1) {
                    try {
                        Integer id = Integer.valueOf(args[1]);
                        anonymousMessage = anonymousMessageRepository.findById(id).orElse(null);
                    } catch (NumberFormatException ignored) {
                    }
                }

                if (!action.equals("delete")) {
                    if (anonymousMessage == null) {
                        EditMessageReplyMarkup editMarkup = new EditMessageReplyMarkup();
                        editMarkup.setChatId(chatId);
                        editMarkup.setMessageId(messageId);
                        editMarkup.setReplyMarkup(Buttons.ANOTHER_ADMIN_ACTION_MARKUP);
                        send(editMarkup);

                        return;
                    }

                    if (action.equals("publish")) {
                        CopyMessage copyMessage = new CopyMessage();
                        copyMessage.setFromChatId(chatId);
                        copyMessage.setMessageId(messageId);
                        copyMessage.setChatId(targetChannel);

                        if (send(copyMessage) == null) {
                            return;
                        }
                    }
                }

                if (anonymousMessage != null) {
                    anonymousMessageRepository.delete(anonymousMessage);
                }
            } else {
                return;
            }

            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(messageId);
            send(deleteMessage);
        }
    }

    private boolean checkIsMemberOfChannel(CachedUser cachedUser) {
        GetChatMember getChatMember = new GetChatMember();
        getChatMember.setChatId(targetChannel);
        getChatMember.setUserId(cachedUser.getTelegramId());

        boolean subscribed = false;
        try {
            ChatMember chatMember = execute(getChatMember);
            subscribed = chatMember != null &&
                    !chatMember.getStatus().equals("left") && !chatMember.getStatus().equals("kicked");
        } catch (TelegramApiException e) {
            log.error("Error checking channel membership", e);
        }

        cachedUser.setLastSubscriptionCheck(LocalDateTime.now());
        cachedUser.setSubscribed(subscribed);
        cachedUser.incrementSubscriptionCheckCount();
        userCacheManager.updateUser(cachedUser);

        return subscribed;
    }

    private void sendAnswerCallback(String callbackQueryId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        answer.setText(text);
        answer.setShowAlert(true);
        send(answer);
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
        AnonymousMessage anonymousMessage = new AnonymousMessage();
        anonymousMessage = anonymousMessageRepository.save(anonymousMessage);

        CopyMessage copyMessage = new CopyMessage();
        copyMessage.setFromChatId(message.getChatId());
        copyMessage.setMessageId(message.getMessageId());

        InlineKeyboardMarkup markup = Buttons.createPostMarkup(anonymousMessage.getId());
        copyMessage.setReplyMarkup(markup);

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
            return Collections.emptyList();
        }
    }

}