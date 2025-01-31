package ru.mk3.suggestions;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.mk3.suggestions.config.BotConfig;
import ru.mk3.suggestions.config.MessageConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
public class SuggestionsBot extends TelegramLongPollingBot {

    private static final Cache<Long, Boolean> membershipCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    private final String botToken;
    private final String botUsername;

    public SuggestionsBot(String botToken, String botUsername) {
        super(new DefaultBotOptions(), botToken);
        getOptions().setGetUpdatesTimeout(2);

        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        Map<Long, List<Message>> messagesMap = new HashMap<>();

        for (Update update : updates) {
            Message message = update.getMessage();
            if (message == null) {
                continue;
            }

            User user = message.getFrom();
            if (user == null || user.getIsBot()) {
                continue;
            }

            Long chatId = message.getChatId();
            if (!isUserMemberOfChannel(chatId)) {
                sendTextMessage(chatId, MessageConfig.MUST_BE_MEMBER);
                continue;
            }

            String text = message.getText();
            if (text != null && text.equalsIgnoreCase("/start")) {
                sendTextMessage(chatId, MessageConfig.START_MESSAGE);
                continue;
            }

            messagesMap.computeIfAbsent(chatId, c -> new ArrayList<>()).add(message);
        }

        messagesMap.forEach((sender, messages) -> {
            List<InputMedia> mediaGroup = new ArrayList<>();
            List<Message> mediaMessages = new ArrayList<>();

            for (Message message : messages) {
                InputMedia media;
                if (message.hasPhoto()) {
                    List<PhotoSize> photos = message.getPhoto();
                    PhotoSize photo = photos.get(0);
                    media = new InputMediaPhoto(photo.getFileId());
                } else if (message.hasVideo()) {
                    Video video = message.getVideo();
                    media = new InputMediaVideo(video.getFileId());
                } else {
                    mediaMessages.add(message);
                    continue;
                }

                String caption = message.getCaption();
                if (caption != null) {
                    media.setCaption(caption);
                    media.setParseMode(ParseMode.MARKDOWN);
                }

                mediaGroup.add(media);
                mediaMessages.add(message);
            }

            if (mediaGroup.size() > 1) {
                SendMediaGroup sendMediaGroup = new SendMediaGroup();
                sendMediaGroup.setMedias(mediaGroup);

                for (String botAdminId : BotConfig.BOT_ADMINS) {
                    sendMediaGroup.setChatId(botAdminId);
                    send(sendMediaGroup);
                }
            } else {
                mediaMessages.forEach(this::forwardMessageToAdmins);
            }

            sendTextMessage(sender, MessageConfig.CONFIRMATION_MESSAGE);
        });
    }

    @Override
    public void onUpdateReceived(Update update) {
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