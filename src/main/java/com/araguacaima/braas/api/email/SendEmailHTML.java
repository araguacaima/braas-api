package com.araguacaima.braas.api.email;

import com.araguacaima.braas.core.MessageType;
import com.araguacaima.commons.utils.OSValidator;
import com.sun.mail.smtp.SMTPTransport;
import de.neuland.jade4j.Jade4J;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SendEmailHTML extends SendEmail {
    private static final String TEXT_HTML = "text/html";
    private final String host;
    private final String username;
    private final String password;
    private final String imageResourceName = "web/public/img/braas.png";
    private final String emailJadeTemplate = "web/views/email-template.jade";
    private String templateFile;

    SendEmailHTML(Properties properties) {
        this.host = properties.getProperty("mail.server.host");
        this.username = properties.getProperty("mail.server.username");
        this.password = properties.getProperty("mail.server.password");

        URL resourceTemplate = SendEmailHTML.class.getClassLoader().getResource(emailJadeTemplate);
        try {
            templateFile = resourceTemplate.toURI().getPath();
            if (OSValidator.isWindows() && templateFile.startsWith("/")) {
                templateFile = templateFile.substring(1);
            }
        } catch (URISyntaxException | NullPointerException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String sendMessage(String recipientsList, String from, String subject, Collection<Object> messages) {

        Map<String, Object> model = new HashMap<>();
        model.put("messages", messages);
        model.put("messageTypeSuccess", MessageType.SUCCESS);
        model.put("messageTypeDebug", MessageType.DEBUG);
        model.put("messageTypeWarning", MessageType.WARNING);
        model.put("messageTypeError", MessageType.ERROR);
        model.put("messageTypeInfo", MessageType.INFO);
        model.put("title", subject);
        model.put("imageResourceName", imageResourceName);


        Properties prop = System.getProperties();
        prop.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(prop, null);
        Message msg = new MimeMessage(session);
        String response = null;
        try {
            final String htmlContent = Jade4J.render(templateFile, model);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientsList, false));
            msg.setSubject(subject);
            msg.setDataHandler(new DataHandler(new HTMLDataSource(htmlContent)));
            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
            t.connect(host, username, password);
            t.sendMessage(msg, msg.getAllRecipients());
            response = t.getLastServerResponse();
            System.out.println("Response: " + response);
            t.close();
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    static class HTMLDataSource implements DataSource {

        private String html;

        public HTMLDataSource(String htmlString) {
            html = htmlString;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (html == null) throw new IOException("html message is null!");
            return new ByteArrayInputStream(html.getBytes());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("This DataHandler cannot write HTML");
        }

        @Override
        public String getContentType() {
            return TEXT_HTML;
        }

        @Override
        public String getName() {
            return "HTMLDataSource";
        }
    }
}