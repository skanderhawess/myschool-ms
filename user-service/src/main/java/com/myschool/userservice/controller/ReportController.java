package com.myschool.userservice.controller;

import com.myschool.userservice.dto.CourseStatsDto;
import com.myschool.userservice.dto.DashboardStatsDto;
import com.myschool.userservice.dto.StudentReportDto;
import com.myschool.userservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReportController {

    private final ReportService reportService;

    /** Statistiques globales du tableau de bord */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        return ResponseEntity.ok(reportService.getDashboardStats());
    }

    /** Statistiques par cours (inscrits + taux de remplissage) */
    @GetMapping("/courses")
    public ResponseEntity<List<CourseStatsDto>> getCourseStats() {
        return ResponseEntity.ok(reportService.getCourseStats());
    }

    /** Liste complète des étudiants avec leurs inscriptions */
    @GetMapping("/students")
    public ResponseEntity<List<StudentReportDto>> getStudentReport() {
        return ResponseEntity.ok(reportService.getStudentReport());
    }

    /** Export Excel de la liste des étudiants */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel() throws Exception {
        byte[] data = reportService.exportStudentsExcel();
        String filename = "students_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** Export PDF du rapport complet */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf() throws Exception {
        byte[] data = reportService.exportStudentsPdf();
        String filename = "report_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }
}
