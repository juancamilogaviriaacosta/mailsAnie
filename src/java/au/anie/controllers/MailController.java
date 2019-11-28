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
                params.put("name", formatter.formatCellValue(row.getCell(0)));
                params.put("adress1", formatter.formatCellValue(row.getCell(1)));
                params.put("adress2", formatter.formatCellValue(row.getCell(2)));
                params.put("attendance", formatter.formatCellValue(row.getCell(3)));
                params.put("date", formatter.formatCellValue(row.getCell(4)));
                String toEmail = formatter.formatCellValue(row.getCell(5));

                String path = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "mail.jasper";
                JasperReport jasperReport = (JasperReport) JRLoader.loadObjectFromFile(path);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource());
                JasperExportManager.exportReportToPdfStream(jasperPrint, new FileOutputStream("students.pdf"));
                
                String subject = "Warning letter attendance " + params.get("name");
                String body = "";

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

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        messageBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);

        msg.setContent(multipart);

        Transport.send(msg);
    }

    public Part getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
}
