package com.xunfang.healthcare.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 轻量级 PDF 生成器，用于统计报表导出，避免引入重量级报表引擎。
 * 输出符合 PDF 1.4 规范的表格型文档。
 *
 * <p>说明：内置字体为 Helvetica，仅支持 ASCII 字符；
 * 含中文的报表建议使用前端打印导出（浏览器打印为 PDF）。</p>
 */
public final class SimplePdfWriter {

    private static final float PAGE_WIDTH = 595f;
    private static final float PAGE_HEIGHT = 842f;
    private static final float MARGIN = 36f;
    private static final float LINE_HEIGHT = 16f;
    private static final int ROWS_PER_PAGE = 40;

    private SimplePdfWriter() {
    }

    /**
     * 生成表格型 PDF。
     *
     * @param title   文档标题
     * @param headers 表头
     * @param rows    数据行
     * @return PDF 字节内容
     */
    public static byte[] write(String title, List<String> headers, List<List<String>> rows) {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add(String.join("  |  ", headers));
        for (List<String> row : rows) {
            lines.add(String.join("  |  ", row));
        }
        try {
            return build(lines);
        } catch (IOException e) {
            throw new IllegalStateException("生成 PDF 失败", e);
        }
    }

    private static byte[] build(List<String> lines) throws IOException {
        int pageCount = Math.max(1, (int) Math.ceil(lines.size() / (double) ROWS_PER_PAGE));
        int objectCount = 2 + pageCount * 2 + 1;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();

        offsets.add(out.size());
        writeObj(out, 1, "<< /Type /Catalog /Pages 2 0 R >>");

        offsets.add(out.size());
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(3 + i * 2).append(" 0 R ");
        }
        writeObj(out, 2, "<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + pageCount + " >>");

        for (int page = 0; page < pageCount; page++) {
            int pageObj = 3 + page * 2;
            int contentObj = pageObj + 1;

            offsets.add(out.size());
            writeObj(out, pageObj, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + (int) PAGE_WIDTH + " "
                    + (int) PAGE_HEIGHT + "] /Resources << /Font << /F1 " + objectCount + " 0 R >> >> /Contents "
                    + contentObj + " 0 R >>");

            byte[] stream = buildStream(lines, page);
            offsets.add(out.size());
            StringBuilder obj = new StringBuilder();
            obj.append(contentObj).append(" 0 obj\n<< /Length ").append(stream.length).append(" >>\nstream\n");
            out.write(obj.toString().getBytes(StandardCharsets.ISO_8859_1));
            out.write(stream);
            out.write("endstream\nendobj\n".getBytes(StandardCharsets.ISO_8859_1));
        }

        offsets.add(out.size());
        writeObj(out, objectCount,
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");

        int xrefOffset = out.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(objectCount + 1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            xref.append(String.format("%010d 00000 n \n", offset));
        }
        xref.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R >>\n");
        xref.append("startxref\n").append(xrefOffset).append("\n%%EOF\n");
        out.write(xref.toString().getBytes(StandardCharsets.ISO_8859_1));
        return out.toByteArray();
    }

    private static void writeObj(ByteArrayOutputStream out, int number, String body) throws IOException {
        out.write((number + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.ISO_8859_1));
    }

    private static byte[] buildStream(List<String> lines, int pageIndex) {
        StringBuilder content = new StringBuilder();
        float y = PAGE_HEIGHT - MARGIN;
        int from = pageIndex * ROWS_PER_PAGE;
        int to = Math.min(lines.size(), from + ROWS_PER_PAGE);
        for (int i = from; i < to; i++) {
            float fontSize = i == 0 ? 14f : 9f;
            content.append("BT /F1 ").append((int) fontSize).append(" Tf 1 0 0 1 ")
                    .append((int) MARGIN).append(' ').append((int) y).append(" Tm (")
                    .append(sanitize(lines.get(i))).append(") Tj ET\n");
            y -= LINE_HEIGHT;
        }
        return content.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c >= 32 && c < 127) {
                sb.append(c);
            } else if (c > 127) {
                sb.append('?');
            }
        }
        return sb.toString().replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
