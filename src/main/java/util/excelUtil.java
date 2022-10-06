package util;

import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class excelUtil {
    /**
     * 创建一个新的表格
     *
     * @param fileName
     */
    public static void createExcel(String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            // 定义一个新的工作簿
            XSSFWorkbook wb = new XSSFWorkbook();
            // 创建一个Sheet页
            XSSFSheet sheet = wb.createSheet("First sheet");
            //设置行高
            sheet.setDefaultRowHeight((short) (2 * 256));
            //设置列宽
            sheet.setColumnWidth(0, 4000);
            sheet.setColumnWidth(1, 4000);
            sheet.setColumnWidth(2, 4000);
            XSSFFont font = wb.createFont();
            font.setFontName("宋体");
            font.setFontHeightInPoints((short) 16);
            //获得表格第一行
            XSSFRow row = sheet.createRow(0);
            //根据需要给第一行每一列设置标题
            XSSFCell cell = row.createCell(0);
            cell.setCellValue("libraryName");
            cell = row.createCell(1);
            cell.setCellValue("groupId");
            cell = row.createCell(2);
            cell.setCellValue("artifactId");
            wb.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 追加数据到excel表中
     *
     * @param fileName
     * @param libraryName
     * @param idInfo
     */
    public static void appendToExcel(String fileName, String libraryName, String[] idInfo) {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(fileName);
            XSSFWorkbook workbook = new XSSFWorkbook(fis);
            //获取到第1个sheet，在里面追加
            XSSFSheet sheet = workbook.getSheetAt(0);
            if (sheet != null) {
                // 获取最后一个实际行的下标
                int i = sheet.getLastRowNum();
                //创建一行
                XSSFRow rows = sheet.createRow(i + 1);
                // 该行创建一个单元格,在该单元格里设置值
                XSSFCell cells = rows.createCell(0);
                cells.setCellValue(libraryName);
                //获得groupId&artifactId
                String groupId = idInfo[0];
                String artifactId = idInfo[1];
                cells = rows.createCell(1);
                cells.setCellValue(groupId);
                cells = rows.createCell(2);
                cells.setCellValue(artifactId);
            }
            fos = new FileOutputStream(fileName);
            //写入
            workbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                //ignore
            }
        }
    }

    public static void backUpExcel(String src, HashMap<String[], String> record) {
        System.out.println("数据输出到Excel...");
        // 定义一个新的工作簿
        XSSFWorkbook wb = new XSSFWorkbook();
        // 创建一个Sheet页
        XSSFSheet sheet = wb.createSheet("First sheet");
        //设置行高
        sheet.setDefaultRowHeight((short) (2 * 256));
        //设置列宽
        sheet.setColumnWidth(0, 4000);
        sheet.setColumnWidth(1, 4000);
        sheet.setColumnWidth(2, 4000);
        XSSFFont font = wb.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 16);
        //获得表格第一行
        XSSFRow row = sheet.createRow(0);
        //根据需要给第一行每一列设置标题
        XSSFCell cell = row.createCell(0);
        cell.setCellValue("libraryName");
        cell = row.createCell(1);
        cell.setCellValue("groupId");
        cell = row.createCell(2);
        cell.setCellValue("artifactId");
        XSSFRow rows;
        XSSFCell cells;
        int i = 0;
        //遍历record hashmap 每一条数据放在excel中一行的单元格里
        for (Map.Entry<String[], String> entry : record.entrySet()) {
            // 在这个sheet页里创建一行
            rows = sheet.createRow(++i);
            String[] idInfo = entry.getKey();
            String libraryName = entry.getValue();
            String groupId = idInfo[0];
            String artifactId = idInfo[1];
            // 该行创建一个单元格,在该单元格里设置值
            cells = rows.createCell(0);
            cells.setCellValue(libraryName);
            cells = rows.createCell(1);
            cells.setCellValue(groupId);
            cells = rows.createCell(2);
            cells.setCellValue(artifactId);
        }
        //保存至文件目录
        try {
            File file = new File(src);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            wb.write(fileOutputStream);
            wb.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
