package com.myschool.userservice.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.myschool.userservice.dto.CourseStatsDto;
import com.myschool.userservice.dto.DashboardStatsDto;
import com.myschool.userservice.dto.StudentReportDto;
import com.myschool.userservice.entity.Course;
import com.myschool.userservice.entity.Student;
import com.myschool.userservice.repository.CourseRepository;
import com.myschool.userservice.repository.EnrollmentRepository;
import com.myschool.userservice.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final StudentRepository studentRepository;
    private final CourseRepository  courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ─────────────────────────────────────────────────
    // 1. DASHBOARD STATISTICS
    // ─────────────────────────────────────────────────

    public DashboardStatsDto getDashboardStats() {
        long totalStudents    = studentRepository.count();
        long totalCourses     = courseRepository.count();
        long totalEnrollments = enrollmentRepository.count();

        double avgEnrollments = totalCourses == 0 ? 0.0
                : (double) totalEnrollments / totalCourses;

        // Load all enrollment counts in ONE query
        Map<Long, Long> enrollmentCountMap = enrollmentRepository
                .countEnrollmentsGroupedByCourse()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // Most popular course
        List<Course> courses = courseRepository.findAll();
        Course mostPopular = courses.stream()
                .max(Comparator.comparingLong(
                        c -> enrollmentCountMap.getOrDefault(c.getId(), 0L)))
                .orElse(null);

        long popularId          = mostPopular != null ? mostPopular.getId() : 0L;
        String popularTitle     = mostPopular != null ? mostPopular.getTitle() : "N/A";
        long popularEnrollments = mostPopular != null
                ? enrollmentCountMap.getOrDefault(mostPopular.getId(), 0L) : 0L;

        return new DashboardStatsDto(
                totalStudents, totalCourses, totalEnrollments,
                Math.round(avgEnrollments * 100.0) / 100.0,
                popularId, popularTitle, popularEnrollments
        );
    }

    // ─────────────────────────────────────────────────
    // 2. COURSE STATS (inscriptions + taux de remplissage)
    // ─────────────────────────────────────────────────

    public List<CourseStatsDto> getCourseStats() {
        // Load all enrollment counts in ONE query
        Map<Long, Long> enrollmentCountMap = enrollmentRepository
                .countEnrollmentsGroupedByCourse()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return courseRepository.findAll().stream()
                .map(c -> {
                    long enrolled = enrollmentCountMap.getOrDefault(c.getId(), 0L);
                    int maxCap = (c.getMaxCapacity() != null && c.getMaxCapacity() > 0)
                            ? c.getMaxCapacity()
                            : 5;
                    double fillRate = Math.min(
                            Math.round((double) enrolled / maxCap * 10000.0) / 100.0,
                            100.0
                    );
                    return new CourseStatsDto(c.getId(), c.getTitle(), enrolled, fillRate, maxCap);
                })
                .sorted(Comparator.comparingLong(CourseStatsDto::getEnrolledCount).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────
    // 3. STUDENT REPORT LIST
    // ─────────────────────────────────────────────────

    public List<StudentReportDto> getStudentReport() {
        // Load all enrollment counts in ONE query
        Map<Long, Long> studentEnrollmentMap = enrollmentRepository
                .countEnrollmentsGroupedByStudent()
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return studentRepository.findAll().stream()
                .map(s -> new StudentReportDto(
                        s.getId(),
                        s.getUser() != null ? s.getUser().getFullName() : "",
                        s.getUser() != null ? s.getUser().getEmail() : "",
                        s.getLevel(),
                        studentEnrollmentMap.getOrDefault(s.getId(), 0L)
                ))
                .sorted(Comparator.comparing(StudentReportDto::getFullName))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────
    // 4. EXPORT EXCEL
    // ─────────────────────────────────────────────────

    public byte[] exportStudentsExcel() throws Exception {
        List<StudentReportDto> students = getStudentReport();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Students");
            sheet.setColumnWidth(0, 3000);
            sheet.setColumnWidth(1, 7000);
            sheet.setColumnWidth(2, 9000);
            sheet.setColumnWidth(3, 4000);
            sheet.setColumnWidth(4, 5000);

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);

            // Data style (alternating rows)
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            CellStyle altStyle = workbook.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LAVENDER.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            altStyle.setBorderBottom(BorderStyle.THIN);
            altStyle.setBorderRight(BorderStyle.THIN);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("MySchool – Student Report – " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            // Headers row
            Row headerRow = sheet.createRow(2);
            String[] headers = {"ID", "Full Name", "Email", "Level", "Enrolled Courses"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 3;
            for (StudentReportDto s : students) {
                Row row = sheet.createRow(rowIdx);
                CellStyle style = (rowIdx % 2 == 0) ? altStyle : dataStyle;

                createCell(row, 0, String.valueOf(s.getStudentId()), style);
                createCell(row, 1, s.getFullName(), style);
                createCell(row, 2, s.getEmail(), style);
                createCell(row, 3, s.getLevel(), style);
                createCell(row, 4, String.valueOf(s.getEnrolledCourses()), style);
                rowIdx++;
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    // ─────────────────────────────────────────────────
    // 5. EXPORT PDF
    // ─────────────────────────────────────────────────

    public byte[] exportStudentsPdf() throws Exception {
        List<StudentReportDto> students = getStudentReport();
        List<CourseStatsDto>   courseStats = getCourseStats();
        DashboardStatsDto      stats = getDashboardStats();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Color purple = new Color(124, 58, 237);
        Color lightPurple = new Color(237, 233, 254);
        Color white = Color.WHITE;
        Color dark = new Color(17, 24, 39);

        Font titleFont   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, white);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, white);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, purple);
        Font headerFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, white);
        Font bodyFont    = FontFactory.getFont(FontFactory.HELVETICA, 9, dark);
        Font boldBody    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, dark);

        // ── Header banner ──
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell bannerCell = new PdfPCell();
        bannerCell.setBackgroundColor(purple);
        bannerCell.setPadding(16);
        bannerCell.setBorder(Rectangle.NO_BORDER);
        Paragraph bannerContent = new Paragraph();
        bannerContent.add(new Chunk("MySchool – Activity Report\n", titleFont));
        bannerContent.add(new Chunk("Generated on " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")),
                subtitleFont));
        bannerCell.addElement(bannerContent);
        banner.addCell(bannerCell);
        doc.add(banner);
        doc.add(new Paragraph(" "));

        // ── Summary stats ──
        doc.add(new Paragraph("Overview", sectionFont));
        doc.add(new Paragraph(" "));
        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingBefore(4f);
        addStatCard(statsTable, "Total Students",    String.valueOf(stats.getTotalStudents()),    purple, lightPurple);
        addStatCard(statsTable, "Total Courses",     String.valueOf(stats.getTotalCourses()),     purple, lightPurple);
        addStatCard(statsTable, "Total Enrollments", String.valueOf(stats.getTotalEnrollments()), purple, lightPurple);
        addStatCard(statsTable, "Avg. per Course",
                String.format("%.1f", stats.getAverageEnrollmentsPerCourse()), purple, lightPurple);
        doc.add(statsTable);
        doc.add(new Paragraph(" "));

        // ── Most popular course ──
        if (stats.getMostPopularCourseId() > 0) {
            Paragraph popular = new Paragraph();
            popular.add(new Chunk("Most popular course: ", boldBody));
            popular.add(new Chunk(stats.getMostPopularCourseTitle() +
                    " (" + stats.getMostPopularCourseEnrollments() + " enrollments)", bodyFont));
            doc.add(popular);
            doc.add(new Paragraph(" "));
        }

        // ── Course stats table ──
        doc.add(new Paragraph("Enrollments per Course", sectionFont));
        doc.add(new Paragraph(" "));
        PdfPTable courseTable = new PdfPTable(3);
        courseTable.setWidthPercentage(100);
        courseTable.setWidths(new float[]{4f, 2f, 2f});
        addTableHeader(courseTable, headerFont, purple, "Course Title", "Enrolled", "Fill Rate");
        boolean alt = false;
        for (CourseStatsDto cs : courseStats) {
            Color bg = alt ? lightPurple : white;
            addTableRow(courseTable, bodyFont, bg,
                    cs.getCourseTitle(),
                    String.valueOf(cs.getEnrolledCount()),
                    cs.getFillRate() + "%");
            alt = !alt;
        }
        doc.add(courseTable);
        doc.add(new Paragraph(" "));

        // ── Students list ──
        doc.add(new Paragraph("Students List", sectionFont));
        doc.add(new Paragraph(" "));
        PdfPTable studentTable = new PdfPTable(5);
        studentTable.setWidthPercentage(100);
        studentTable.setWidths(new float[]{1f, 3f, 4f, 2f, 2f});
        addTableHeader(studentTable, headerFont, purple, "ID", "Full Name", "Email", "Level", "Courses");
        alt = false;
        for (StudentReportDto s : students) {
            Color bg = alt ? lightPurple : white;
            addTableRow(studentTable, bodyFont, bg,
                    String.valueOf(s.getStudentId()),
                    s.getFullName(),
                    s.getEmail(),
                    s.getLevel(),
                    String.valueOf(s.getEnrolledCourses()));
            alt = !alt;
        }
        doc.add(studentTable);

        // ── Footer ──
        doc.add(new Paragraph(" "));
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY);
        doc.add(new Paragraph("This report was automatically generated by MySchool platform.", footerFont));

        doc.close();
        return out.toByteArray();
    }

    private void addStatCard(PdfPTable table, String label, String value,
                             Color accent, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setPadding(12);
        cell.setBorderColor(accent);
        Paragraph p = new Paragraph();
        Font valFont  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, accent);
        Font lblFont  = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(107, 114, 128));
        p.add(new Chunk(value + "\n", valFont));
        p.add(new Chunk(label, lblFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, Font font, Color bg, String... cols) {
        for (String col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col, font));
            cell.setBackgroundColor(bg);
            cell.setPadding(8);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, Font font, Color bg, String... values) {
        for (String val : values) {
            PdfPCell cell = new PdfPCell(new Phrase(val, font));
            cell.setBackgroundColor(bg);
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    // ─────────────────────────────────────────────────
    // 6. RAPPORT MENSUEL AUTOMATIQUE (@Scheduled)
    // ─────────────────────────────────────────────────

    /** Exécuté le 1er de chaque mois à 08h00 */
    @Scheduled(cron = "0 0 8 1 * *")
    public void generateMonthlyReport() {
        log.info("=== [SCHEDULED] Monthly report generation started – {} ===",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        try {
            DashboardStatsDto stats = getDashboardStats();
            List<CourseStatsDto> courseStats = getCourseStats();

            log.info("Monthly Report Summary:");
            log.info("  Total students    : {}", stats.getTotalStudents());
            log.info("  Total courses     : {}", stats.getTotalCourses());
            log.info("  Total enrollments : {}", stats.getTotalEnrollments());
            log.info("  Avg per course    : {}", String.format("%.2f", stats.getAverageEnrollmentsPerCourse()));
            log.info("  Most popular      : {} ({} enrollments)",
                    stats.getMostPopularCourseTitle(), stats.getMostPopularCourseEnrollments());
            log.info("Course breakdown:");
            courseStats.forEach(cs ->
                    log.info("    {} → {} enrolled ({} %)",
                            cs.getCourseTitle(), cs.getEnrolledCount(), cs.getFillRate()));
            log.info("=== Monthly report generation completed ===");
        } catch (Exception e) {
            log.error("Monthly report generation failed", e);
        }
    }
}
