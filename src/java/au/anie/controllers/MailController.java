package au.anie.controllers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.activation.DataHandler;
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

    public void send() throws Exception {
        InputStream fis = uploadedFile.getInputStream();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String logo = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "logo.jpg";
                    String signature = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "signature.jpg";

                    String fromEmail = "";
                    String password = "";

                    Properties props = new Properties();
                    //props.put("mail.smtp.host", "smtp.gmail.com");
                    props.put("mail.smtp.host", "smtp.office365.com");
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

                    SimpleDateFormat yyyymmdd = new SimpleDateFormat("yyyy-MM-dd");
                    SimpleDateFormat ddmmyyyy = new SimpleDateFormat("dd/MM/yyyy");

                    File pdftmp = new File("pdftmp");
                    if (pdftmp.exists()) {
                        rdelete(pdftmp);
                    }
                    pdftmp.mkdirs();

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
                        params.put("date", ddmmyyyy.format(row.getCell(4).getDateCellValue()));
                        String toEmail = formatter.formatCellValue(row.getCell(5));

                        File folder = new File(pdftmp.getAbsolutePath() + File.separator + UUID.randomUUID().toString());
                        folder.mkdirs();
                        String finalpdf = folder.getAbsolutePath() + File.separator + "Attendance - " + yyyymmdd.format(row.getCell(4).getDateCellValue()) + ".pdf";
                        System.out.println(finalpdf);
                        String jasper = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "mail.jasper";
                        JasperReport jasperReport = (JasperReport) JRLoader.loadObjectFromFile(jasper);
                        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, new JREmptyDataSource());
                        FileOutputStream fos = new FileOutputStream(finalpdf);
                        JasperExportManager.exportReportToPdfStream(jasperPrint, fos);
                        fos.close();

                        String subject = "Warning letter attendance " + params.get("name");
                        String body = "<hr><br/>"
                                + "<p style=\"font-weight:bold;\">"
                                + "Date: " + params.get("date") + "<br/>"
                                + "Name: " + params.get("name") + "<br/>"
                                + "Adress: " + params.get("adress1") + "<br/>"
                                + "&nbsp;&nbsp;&nbsp;" + params.get("adress2") + "<br/><br/><br/>"
                                + "Unsatisfactory attendance warning </p>"
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
                                + "<hr><br/><br/>"
                                + "<p style=\"color: #1F497D; font-weight:bold;\">\n"
                                + "Yours sincerely,<br/>\n"
                                + "Diana Gaviria<br/><br/>\n"
                                + "\n"
                                + "Reception<br/><br/>\n"
                                + "<img src=\"cid:logo\" width=\"50\" height=\"50\"><br/>"
                                + "About us:\n"
                                + "</p>\n"
                                + "\n"
                                + "<p style=\"color: #1F497D;\">\n"
                                + "Australian National Institute of Education is a Registered Training Organisation<br/>\n"
                                + "Please find out how we can help you at <font style=\"text-decoration: underline;\">www.anie.edu.au</font><br/>\n"
                                + "</p>\n"
                                + "\n"
                                + "\n"
                                + "<p style=\"color: #1F497D; font-weight:bold;\">\n"
                                + "Contact us:\n"
                                + "</p>\n"
                                + "\n"
                                + "<p style=\"color: #1F497D\">\n"
                                + "Suite 11, 197 Prospect Highway, Seven Hills, NSW 2147<br/>\n"
                                + "Phone: 1300 812 355 (Australia ), +61 2 9620 5501 (overseas)\n"
                                + "</p>\n"
                                + "\n"
                                + "<p style=\"color: #1F497D; font-weight:bold; text-decoration: underline;\">\n"
                                + "RTO: 41160 | CRICOS Provider Code: 03682M | ABN: 54 603 488 526\n"
                                + "</p>\n"
                                + "\n"
                                + "<p style=\"color: #A8D08D; font-weight:bold;\">\n"
                                + "Please consider the environment before printing this email.\n"
                                + "\n"
                                + "\n"
                                + "<p style=\"color: #8EAADB; font-weight: lighter;\">\n"
                                + "Disclaimer: This e-mail, it's content, and any files transmitted with it are intended solely for the addressee(s) and may be legally privileged and confidential.  If you are not the intended recipient, you must not use, disclose, distribute, copy, print or rely on this e-mail.  Please destroy it and contact the sender by e-mail return.  This e-mail has been prepared using information believed by the author to be reliable and accurate, but Skills International makes no warranty as to accuracy or completeness.  In particular, Skills International does not accept responsibility for changes made to this e-mail after it was sent.  Any opinions expressed in this document are those of the author and do not necessarily reflect the opinions of Skills International. Although Skills International has taken steps to ensure that this e-mail and attachments are free from any virus, we would advise that in keeping with good computing practice, the recipient should ensure they are actually virus free.\n"
                                + "</p>";

                        sendAttachmentEmail(session, toEmail, subject, body, finalpdf);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        String logo = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "logo.jpg";

        Multipart multipart = new MimeMultipart();

        BodyPart bodyPartText = new MimeBodyPart();
        bodyPartText.setContent(body, "text/html");
        multipart.addBodyPart(bodyPartText);

        BodyPart bodyPartImg = new MimeBodyPart();
        bodyPartImg.setDataHandler(new DataHandler(new FileDataSource(signature)));
        bodyPartImg.setHeader("Content-ID", "<image>");
        multipart.addBodyPart(bodyPartImg);

        BodyPart bodyPartLogo = new MimeBodyPart();
        bodyPartLogo.setDataHandler(new DataHandler(new FileDataSource(logo)));
        bodyPartLogo.setHeader("Content-ID", "<logo>");
        multipart.addBodyPart(bodyPartLogo);

        BodyPart bodyPartFile = new MimeBodyPart();
        bodyPartFile.setDataHandler(new DataHandler(new FileDataSource(filename)));
        bodyPartFile.setFileName(filename.substring(filename.lastIndexOf(File.separator) + 1));
        multipart.addBodyPart(bodyPartFile);

        msg.setContent(multipart);

        Transport.send(msg);
    }

    public void rdelete(File tmp) {
        File[] file = tmp.listFiles();
        if (file != null) {
            for (File f : file) {
                if (f.isDirectory()) {
                    rdelete(f);
                } else {
                    f.delete();
                }
            }
        }
        tmp.delete();
    }

    public Part getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(Part uploadedFile) {
        this.uploadedFile = uploadedFile;
    }
}
