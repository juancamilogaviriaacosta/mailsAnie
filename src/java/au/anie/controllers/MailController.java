package au.anie.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.Part;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author juan
 */
@ViewScoped
@ManagedBean(name = "mailController")
public class MailController {

    private Part uploadedFile;

    public void send() {
        try {
            String fromEmail = "";
            String password = "";

            String logo = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "logo.jpg";
            String signature = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "signature.jpg";

            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            Authenticator auth = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(fromEmail, password);
                }
            };
            Session session = Session.getInstance(props, auth);

            InputStream fis = uploadedFile.getInputStream();
            Workbook workbook = uploadedFile.getSubmittedFileName().endsWith(".xls") ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
            DataFormatter formatter = new DataFormatter();

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); //skip header
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                Map<String, Object> params = new HashMap<>();
                params.put("logo", logo);
                params.put("signature", signature);
                params.put("name", formatter.formatCellValue(row.getCell(0)));
                params.put("adress1", formatter.formatCellValue(row.getCell(1)));
                params.put("adress2", formatter.formatCellValue(row.getCell(2)));
                params.put("attendance", formatter.formatCellValue(row.getCell(3)));
                params.put("date", formatter.formatCellValue(row.getCell(4)));
                String toEmail = formatter.formatCellValue(row.getCell(5));

                String jasper = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "mail.jasper";
                JasperReport jasperReport = (JasperReport) JRLoader.loadObjectFromFile(jasper);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource());
                JasperExportManager.exportReportToPdfStream(jasperPrint, new FileOutputStream("students.pdf"));

                String subject = "Warning letter attendance " + params.get("name");
                String body = "<hr><br/>"
                        + "Date: " + params.get("date") + "<br/>"
                        + "Name: " + params.get("name") + "<br/>"
                        + "Adress: " + params.get("adress1") + "<br/>"
                        + "&nbsp;&nbsp;&nbsp;&nbsp;" + params.get("adress2") + "<br/><br/>"
                        + "Unsatisfactory attendance warning<br/>"
                        + "<br/>"
                        + "Dear " + params.get("name") + "<br/>"
                        + "<br/>"
                        + "Thank you for studying with Australian National Institute of Education (ANIE). During the enrolment and orientation programme, you were informed of the student visa condition relating to course attendance. All international students are expected to maintain 40 hours of class attendance on fortnightly basis.<br/>"
                        + "<br/>"
                        + "You have attended " + params.get("attendance") + "% of the class hours in last fortnight, whereas you are expected to maintain at least 80%.<br/>"
                        + "<br/>"
                        + "You are now requested to meet Director of Studies and discuss the reasons of your shortfall in attendance, so that it improves afterwards. We may offer you options so that you achieve the required attendance level. If you miss more than 80% of your attendance in two consecutive terms, ANIE will report you to Department of Education which may affect your student visa.<br/>"
                        + "<br/>"
                        + "<img src=\"cid:image\" width=\"120\" height=\"42\"><br/>"
                        + "Letter sent by<br/>"
                        + "<br/>"
                        + "Student Support Manager<br/>"
                        + "<br/>"
                        + "Australian National Institute of Education (ANIE)<br/>"
                        + "<hr>";

                sendAttachmentEmail(session, toEmail, subject, body, "students.pdf");
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAttachmentEmail(Session session, String toEmail, String subject, String body, String filename) throws MessagingException, UnsupportedEncodingException {

        MimeMessage msg = new MimeMessage(session);
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        msg.setFrom(new InternetAddress("reception@anie.edu.au", "reception@anie.edu.au"));
        msg.setReplyTo(InternetAddress.parse("reception@anie.edu.au", false));
        msg.setSubject(subject, "UTF-8");
        msg.setSentDate(new Date());
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        String signature = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "signature.jpg";

        Multipart multipart = new MimeMultipart();

        BodyPart bodyPartText = new MimeBodyPart();
        bodyPartText.setContent(body, "text/html");
        multipart.addBodyPart(bodyPartText);

        BodyPart bodyPartImg = new MimeBodyPart();
        bodyPartImg.setDataHandler(new DataHandler(new FileDataSource(signature)));
        bodyPartImg.setHeader("Content-ID", "<image>");
        multipart.addBodyPart(bodyPartImg);

        BodyPart bodyPartFile = new MimeBodyPart();
        bodyPartFile.setDataHandler(new DataHandler(new FileDataSource(filename)));
        bodyPartFile.setFileName(filename);
        multipart.addBodyPart(bodyPartFile);

        msg.setContent(multipart);

        Transport.send(msg);
    }

    public static void sendImageEmail(Session session, String toEmail, String subject, String body) {
        try {
            MimeMessage msg = new MimeMessage(session);
            msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
            msg.addHeader("format", "flowed");
            msg.addHeader("Content-Transfer-Encoding", "8bit");

            msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply-JD"));

            msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));

            msg.setSubject(subject, "UTF-8");

            msg.setSentDate(new Date());

            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));

            // Create the message body part
            BodyPart messageBodyPart = new MimeBodyPart();

            messageBodyPart.setText(body);

            // Create a multipart message for attachment
            Multipart multipart = new MimeMultipart();

            // Set text message part
            multipart.addBodyPart(messageBodyPart);

            // Second part is image attachment
            messageBodyPart = new MimeBodyPart();
            String filename = "image.png";
            DataSource source = new FileDataSource(filename);
            messageBodyPart.setDataHandler(new DataHandler(source));
            messageBodyPart.setFileName(filename);
            //Trick is to add the content-id header here
            messageBodyPart.setHeader("Content-ID", "image_id");
            multipart.addBodyPart(messageBodyPart);

            //third part for displaying image in the email body
            messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent("<h1>Attached Image</h1>"
                    + "<img src='cid:image_id'>", "text/html");
            multipart.addBodyPart(messageBodyPart);

            //Set the multipart message to the email message
            msg.setContent(multipart);

            // Send message
            Transport.send(msg);
            System.out.println("EMail Sent Successfully with image!!");
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public Part getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
}
