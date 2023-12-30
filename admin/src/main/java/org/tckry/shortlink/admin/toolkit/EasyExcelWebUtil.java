package org.tckry.shortlink.admin.toolkit;

import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 封装 EasyExcel 操作 Web 工具方法
 * @program: shortlink
 * @description:
 * @author: lydms
 * @create: 2023-12-30 19:21
 **/
public class EasyExcelWebUtil {

    /**
     * 向浏览器写入 Excel 响应，直接返回用户下载数据
     *
     * @param response 响应
     * @param fileName 文件名
     * @param clazz    指定写入类
     * @param data     写入数据
     */
    @SneakyThrows
    public static void write(HttpServletResponse response, String fileName, Class<?> clazz, List<?> data) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        // 使用 EasyExcel 将数据写入响应的输出流。clazz 表示数据对象的类型，data 是要导出的数据列表。sheet("Sheet") 指定 Excel 中的 sheet 名称。
        EasyExcel.write(response.getOutputStream(), clazz).sheet("Sheet").doWrite(data);
    }
}
