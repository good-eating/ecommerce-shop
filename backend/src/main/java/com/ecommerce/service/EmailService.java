package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOrderEmail(Order order, User user, List<OrderItem> orderItems) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("2985917370@qq.com");
            message.setTo(user.getEmail());
            message.setSubject("订单创建成功 - " + order.getOrderNo());

            StringBuilder content = new StringBuilder();
            content.append("尊敬的 ").append(user.getUsername()).append("，\n\n");
            content.append("您的订单已创建成功！\n\n");
            content.append("订单号：").append(order.getOrderNo()).append("\n");
            content.append("订单金额：¥").append(order.getTotalAmount()).append("\n");
            content.append("商品列表：\n");

            for (OrderItem item : orderItems) {
                content.append("- ").append(item.getProductName())
                        .append(" x ").append(item.getQuantity())
                        .append(" = ¥").append(item.getSubtotal()).append("\n");
            }

            content.append("\n请在30分钟内完成支付，否则订单将自动取消。\n\n");
            content.append("感谢您的购物！");

            message.setText(content.toString());
            mailSender.send(message);
            log.info("订单邮件发送成功: {} -> {}", order.getOrderNo(), user.getEmail());
        } catch (Exception e) {
            log.error("发送订单邮件失败: {}", order.getOrderNo(), e);
        }
    }
}
