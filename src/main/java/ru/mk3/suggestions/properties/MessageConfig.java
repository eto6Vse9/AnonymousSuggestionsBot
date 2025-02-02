package ru.mk3.suggestions.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource("classpath:messages.properties")
@Data
public class MessageConfig {

    @Value("${start}")
    private String startMessage;

    @Value("${confirmation}")
    private String confirmationMessage;

    @Value("${must-be-member}")
    private String mustBeMemberMessage;

    @Value("${limit-exceeded}")
    private String limitExceededMessage;

    @Value("${subscription-check-limit-exceeded}")
    private String subscriptionCheckLimitExceededMessage;

}