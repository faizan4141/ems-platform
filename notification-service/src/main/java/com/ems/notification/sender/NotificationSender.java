package com.ems.notification.sender;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;

public interface NotificationSender {
    Channel channel();
    void send(Notification notification) throws Exception;
}