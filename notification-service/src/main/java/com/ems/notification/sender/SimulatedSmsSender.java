package com.ems.notification.sender;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stand-in until a real SMS provider is chosen. Replace this one class to go live. */
@Component
public class SimulatedSmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SimulatedSmsSender.class);

    @Override
    public Channel channel() { return Channel.SMS; }

    @Override
    public void send(Notification n) {
        String phone = n.getRecipient();
        String masked = phone.length() <= 4 ? "****" : "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
        log.info("SMS (simulated) to {}, {} characters", masked, n.getBody().length());   // never log the message itself
    }
}