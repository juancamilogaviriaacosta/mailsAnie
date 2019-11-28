package au.anie.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
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

    private String name;

    public void print() {
        try {
            File file = new File("/home/juan/Escritorio/students.xlsx");
            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = file.getName().endsWith(".xls") ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
            DataFormatter formatter = new DataFormatter();

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); //skip header
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                Map<String, Object> parametros = new HashMap<>();
                parametros.put("name", formatter.formatCellValue(row.createCell(0)));
                parametros.put("adress1", formatter.formatCellValue(row.createCell(1)));
                parametros.put("adress2", formatter.formatCellValue(row.createCell(2)));
                parametros.put("attendance", formatter.formatCellValue(row.createCell(3)));
                parametros.put("date", formatter.formatCellValue(row.createCell(4)));
                String mail = formatter.formatCellValue(row.createCell(5));

                String path = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "mail.jasper";
                JasperReport jasperReport = (JasperReport) JRLoader.loadObjectFromFile(path);
                JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, new JREmptyDataSource());
                JasperExportManager.exportReportToPdfStream(jasperPrint, new FileOutputStream("/home/juan/Escritorio/students.pdf"));

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAttachmentEmail(Session session, String toEmail, String subject, String body) throws MessagingException, UnsupportedEncodingException {

        MimeMessage msg = new MimeMessage(session);
        msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
        msg.addHeader("format", "flowed");
        msg.addHeader("Content-Transfer-Encoding", "8bit");
        msg.setFrom(new InternetAddress("no_reply@example.com", "NoReply-JD"));
        msg.setReplyTo(InternetAddress.parse("no_reply@example.com", false));
        msg.setSubject(subject, "UTF-8");
        msg.setSentDate(new Date());
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        messageBodyPart = new MimeBodyPart();
        String filename = "abc.txt";
        DataSource source = new FileDataSource(filename);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(filename);
        multipart.addBodyPart(messageBodyPart);

        msg.setContent(multipart);

        Transport.send(msg);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
