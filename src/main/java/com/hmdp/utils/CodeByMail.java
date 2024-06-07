package com.hmdp.utils;

import com.sun.mail.util.MailSSLSocketFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;


@Component
@Slf4j
public class CodeByMail {

    public static void send(String target,String code) throws Exception {

        Properties prop = new Properties();
        // 开启debug调试，以便在控制台查看
        prop.setProperty("mail.debug", "true");
        // 设置邮件服务器主机名
        prop.setProperty("mail.host", "smtp.qq.com");
        // 发送服务器需要身份验证
        prop.setProperty("mail.smtp.auth", "true");
        // 发送邮件协议名称
        prop.setProperty("mail.transport.protocol", "smtp");

        // 开启SSL加密，否则会失败
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);
        prop.put("mail.smtp.ssl.enable", "true");
        prop.put("mail.smtp.ssl.socketFactory", sf);
        prop.put("mail.smtp.ssl.protocols", "TLSv1.2");
        // 创建session
        Session session = Session.getInstance(prop);

        // 通过session得到transport对象
        Transport ts = session.getTransport();
        // 连接邮件服务器：邮箱类型，帐号，授权码代替密码（更安全）
        ts.connect("smtp.qq.com", "167993231", "zeblnqzczjjfbggi");
        // 后面的字符是授权码，用qq密码失败了

        // 创建邮件
        Message message = createSimpleMail(session,target,code);
        // 发送邮件
        ts.sendMessage(message, message.getAllRecipients());
        log.info("发送邮件验证码成功");
        ts.close();
    }

    public static MimeMessage createSimpleMail(Session session,String target,String code)
            throws Exception {
        // 创建邮件对象
        MimeMessage message = new MimeMessage(session);
        // 指明邮件的发件人
        message.setFrom(new InternetAddress("167993231@qq.com"));
        // 指明邮件的收件人
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(
                target));
        // 邮件的标题
        message.setSubject("黑马点评登录验证");
        // 邮件的文本内容
        message.setContent("验证码"+code+"，2分钟内有效，如非本人操作，请及时修改密码", "text/html;charset=UTF-8");
        // 返回创建好的邮件对象
        return message;
    }
}
