package au.anie.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.bean.ManagedBean;
import javax.faces.view.ViewScoped;
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
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("name", "Juan");
            parametros.put("adress1", "");
            parametros.put("adress2", "");
            parametros.put("attendance", "");
            parametros.put("date", "");

            String path = new File(this.getClass().getResource("MailController.class").getPath()).getParent() + File.separator + "mail.jasper";
            JasperReport jasperReport = (JasperReport) JRLoader.loadObjectFromFile(path);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, new JREmptyDataSource());
            JasperExportManager.exportReportToPdfStream(jasperPrint, new FileOutputStream("/home/juan/Escritorio/students.pdf"));

            File file = new File("/home/juan/Escritorio/students.xlsx");
            List<List<List<Object>>> archivoExcel = getArchivoExcel(new FileInputStream(file), file.getName());
            for (List<List<Object>> hoja : archivoExcel) {
                for (List<Object> fila : hoja) {
                    for (Object celda : fila) {
                        System.out.println(celda);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<List<List<Object>>> getArchivoExcel(InputStream fis, String nombre) throws IOException {
        Workbook workbook = nombre.endsWith(".xls") ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
        List<List<List<Object>>> resp = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        int n = workbook.getNumberOfSheets();
        for (int i = 0; i < n; i++) {
            List<List<Object>> hoja = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(i);
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext()) {
                List<Object> fila = new ArrayList<>();
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    Object valor = formatter.formatCellValue(cell);
                    fila.add(valor);
                }
                hoja.add(fila);
            }
            resp.add(hoja);
        }
        return resp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
