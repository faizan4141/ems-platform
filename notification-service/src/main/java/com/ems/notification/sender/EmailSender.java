package com.ems.notification.sender;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class EmailSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSender(JavaMailSender mailSender, @Value("${notifications.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public Channel channel() { return Channel.EMAIL; }

    @Override
    public void send(Notification n) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(n.getRecipient());
        mail.setSubject(n.getSubject());
        mail.setText(n.getBody());
        mailSender.send(mail);
    }
}