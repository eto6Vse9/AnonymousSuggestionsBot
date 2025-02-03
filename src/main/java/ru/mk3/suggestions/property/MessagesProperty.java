package ru.mk3.suggestions.property;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:messages.properties", encoding = "UTF-8")
@Data
public class MessagesProperty {

    @Value("${start}")
    private String start;

    @Value("${confirmation}")
    private String confirmation;

    @Value("${must-be-member}")
    private String mustBeMember;

    @Value("${limit-exceeded}")
    private String limitExceeded;

    @Value("${subscription-check-limit-exceeded}")
    private String subscriptionCheckLimitExceeded;

    @Value("${still-not-subscribed}")
    private String stillNotSubscribed;

}