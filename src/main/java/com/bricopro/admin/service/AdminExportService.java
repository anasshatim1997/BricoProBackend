package com.bricopro.admin.service;

import com.bricopro.analytics.WorkerPerformanceService;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Tag(name = "Admin Export Service", description = "Business logic for Admin Export Service")
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminExportService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerPerformanceService performanceService;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] MONTH_NAMES = {
            "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
            "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
    };

    public byte[] exportUsersCsv() {
        List<User> users = userRepository.findAll();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(new byte[]{
                    (byte) 0xEF,
                    (byte) 0xBB,
                    (byte) 0xBF
            });
        } catch (Exception ignored) {
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            pw.println("ID,Prénom,Nom,Email,Téléphone,Rôle,Statut,Vérifié,Date inscription");

            for (User u : users) {
                pw.printf(
                        "%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        u.getId(),
                        escapeCsv(u.getFirstName()),
                        escapeCsv(u.getLastName()),
                        escapeCsv(u.getEmail()),
                        escapeCsv(u.getPhone()),
                        u.getRole().name(),
                        u.getStatus().name(),
                        u.isVerified() ? "Oui" : "Non",
                        u.getCreatedAt() != null
                                ? u.getCreatedAt().format(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : ""
                );
            }

        } catch (Exception e) {
            log.error("CSV export users failed", e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }

        return baos.toByteArray();
    }

    public byte[] exportTasksCsv(LocalDate from, LocalDate to) {
        List<Task> tasks = taskRepository.findAll();

        if (from != null) {
            tasks = tasks.stream()
                    .filter(t -> !t.getScheduledDate().isBefore(from))
                    .toList();
        }

        if (to != null) {
            tasks = tasks.stream()
                    .filter(t -> !t.getScheduledDate().isAfter(to))
                    .toList();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(new byte[]{
                    (byte) 0xEF,
                    (byte) 0xBB,
                    (byte) 0xBF
            });
        } catch (Exception ignored) {
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            pw.println("ID,Titre,Service,Client,Prestataire,Statut,Date planifiée,Prix convenu,Urgent,Créé le");

            for (Task t : tasks) {
                pw.printf(
                        "%d,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        t.getId(),
                        escapeCsv(t.getTitle()),
                        t.getServiceType().name(),
                        escapeCsv(
                                t.getClient().getFirstName() + " " +
                                        t.getClient().getLastName()
                        ),
                        t.getWorker() != null
                                ? escapeCsv(
                                t.getWorker().getFirstName() + " " +
                                        t.getWorker().getLastName()
                        )
                                : "Non assigné",
                        t.getStatus().name(),
                        t.getScheduledDate().format(DATE_FMT),
                        t.getAgreedPrice() != null
                                ? t.getAgreedPrice().toPlainString()
                                : "0",
                        t.isUrgent() ? "Oui" : "Non",
                        t.getCreatedAt() != null
                                ? t.getCreatedAt().format(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                : ""
                );
            }

        } catch (Exception e) {
            log.error("CSV export tasks failed", e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }

        return baos.toByteArray();
    }

    public byte[] exportRevenueCsv(int year) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(new byte[]{
                    (byte) 0xEF,
                    (byte) 0xBB,
                    (byte) 0xBF
            });
        } catch (Exception ignored) {
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            pw.println("Mois,Année,Revenus plateforme (MAD)");

            BigDecimal total = BigDecimal.ZERO;

            for (int m = 1; m <= 12; m++) {
                BigDecimal rev =
                        paymentRepository.sumPlatformFeeByMonthAndYear(m, year);

                BigDecimal monthRev =
                        rev != null ? rev : BigDecimal.ZERO;

                total = total.add(monthRev);

                pw.printf(
                        "%02d,%d,%s%n",
                        m,
                        year,
                        monthRev.toPlainString()
                );
            }

            pw.printf(
                    "TOTAL,%d,%s%n",
                    year,
                    total.toPlainString()
            );

        } catch (Exception e) {
            log.error("CSV export revenue failed", e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }

        return baos.toByteArray();
    }

    public byte[] exportWorkerPerformanceCsv() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            baos.write(new byte[]{
                    (byte) 0xEF,
                    (byte) 0xBB,
                    (byte) 0xBF
            });
        } catch (Exception ignored) {
        }

        try (PrintWriter pw = new PrintWriter(
                new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {

            pw.println(
                    "Worker ID,Missions totales,Missions terminées," +
                            "Missions annulées,Taux de complétion," +
                            "Note moyenne,Avis,Taux de réponse," +
                            "Score perf,Tier,Premium"
            );

            workerProfileRepository.findAll(Pageable.unpaged())
                    .forEach(wp -> {
                        try {

                            WorkerPerformanceService.PerformanceReport r =
                                    performanceService.getReport(
                                            wp.getUser().getId()
                                    );

                            pw.printf(
                                    "%d,%d,%d,%d,%.1f,%.2f,%d,%.1f,%.1f,%s,%s%n",
                                    r.getWorkerId(),
                                    r.getTotalMissions(),
                                    r.getCompletedMissions(),
                                    r.getCancelledMissions(),
                                    r.getCompletionRate(),
                                    r.getAverageRating(),
                                    r.getTotalReviews(),
                                    r.getResponseRate(),
                                    r.getPerformanceScore(),
                                    r.getTier(),
                                    r.isPremium() ? "Oui" : "Non"
                            );

                        } catch (Exception e) {
                            log.warn(
                                    "Skipping worker {} in export: {}",
                                    wp.getId(),
                                    e.getMessage()
                            );
                        }
                    });

        } catch (Exception e) {
            log.error("CSV export worker performance failed", e);
            throw new RuntimeException("Export failed: " + e.getMessage());
        }

        return baos.toByteArray();
    }

    public byte[] exportRevenuePdf(int year) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {

            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont regularFont =
                    PdfFontFactory.createFont(StandardFonts.HELVETICA);

            PdfFont boldFont =
                    PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            DeviceRgb brandOrange =
                    new DeviceRgb(230, 126, 34);

            DeviceRgb darkGray =
                    new DeviceRgb(44, 62, 80);

            DeviceRgb lightGray =
                    new DeviceRgb(236, 240, 241);

            Paragraph header = new Paragraph(
                    "BricoPro — Rapport de Revenus " + year
            )
                    .setFont(boldFont)
                    .setFontSize(20)
                    .setFontColor(brandOrange)
                    .setTextAlignment(TextAlignment.CENTER);

            doc.add(header);

            Paragraph subtitle = new Paragraph(
                    "Généré le " +
                            LocalDate.now().format(
                                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
            )
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20);

            doc.add(subtitle);

            Table table = new Table(
                    UnitValue.createPercentArray(
                            new float[]{40, 30, 30}
                    )
            ).useAllAvailableWidth();

            addTableHeaderCell(
                    table,
                    "Mois",
                    brandOrange,
                    boldFont
            );

            addTableHeaderCell(
                    table,
                    "Revenus (MAD)",
                    brandOrange,
                    boldFont
            );

            addTableHeaderCell(
                    table,
                    "Évolution",
                    brandOrange,
                    boldFont
            );

            BigDecimal total = BigDecimal.ZERO;
            BigDecimal prevMonth = null;

            boolean shaded = false;

            for (int m = 1; m <= 12; m++) {

                BigDecimal rev =
                        paymentRepository.sumPlatformFeeByMonthAndYear(
                                m,
                                year
                        );

                BigDecimal monthRev =
                        rev != null ? rev : BigDecimal.ZERO;

                total = total.add(monthRev);

                String evolution = "—";

                if (prevMonth != null &&
                        prevMonth.compareTo(BigDecimal.ZERO) > 0) {

                    double pct =
                            (
                                    monthRev.doubleValue() -
                                            prevMonth.doubleValue()
                            )
                                    /
                                    prevMonth.doubleValue()
                                    * 100;

                    evolution = String.format("%+.1f%%", pct);
                }

                DeviceRgb rowBg =
                        shaded
                                ? lightGray
                                : new DeviceRgb(255, 255, 255);

                addTableDataCell(
                        table,
                        MONTH_NAMES[m - 1],
                        rowBg,
                        regularFont
                );

                addTableDataCell(
                        table,
                        monthRev.toPlainString() + " MAD",
                        rowBg,
                        regularFont
                );

                addTableDataCell(
                        table,
                        evolution,
                        rowBg,
                        regularFont
                );

                prevMonth = monthRev;
                shaded = !shaded;
            }

            Cell totalLabel = new Cell()
                    .add(
                            new Paragraph("TOTAL")
                                    .setFont(boldFont)
                    )
                    .setBackgroundColor(darkGray)
                    .setFontColor(ColorConstants.WHITE);

            Cell totalValue = new Cell()
                    .add(
                            new Paragraph(
                                    total.toPlainString() + " MAD"
                            ).setFont(boldFont)
                    )
                    .setBackgroundColor(darkGray)
                    .setFontColor(ColorConstants.WHITE);

            Cell totalEmpty = new Cell()
                    .add(
                            new Paragraph("")
                                    .setFont(regularFont)
                    )
                    .setBackgroundColor(darkGray);

            table.addCell(totalLabel);
            table.addCell(totalValue);
            table.addCell(totalEmpty);

            doc.add(table);

            doc.add(new Paragraph("\n"));

            doc.add(
                    new Paragraph("Résumé annuel")
                            .setFont(boldFont)
                            .setFontSize(12)
                            .setFontColor(darkGray)
            );

            doc.add(
                    new Paragraph(
                            "Revenu total plateforme : " +
                                    total.toPlainString() +
                                    " MAD"
                    )
                            .setFont(regularFont)
                            .setFontSize(11)
            );

            doc.add(
                    new Paragraph(
                            "Commission appliquée : 12% sur chaque transaction"
                    )
                            .setFont(regularFont)
                            .setFontSize(9)
                            .setFontColor(ColorConstants.GRAY)
            );

            doc.add(
                    new Paragraph(
                            "\n\nDocument confidentiel — BricoPro © " + year
                    )
                            .setFont(regularFont)
                            .setFontSize(8)
                            .setFontColor(ColorConstants.GRAY)
                            .setTextAlignment(TextAlignment.CENTER)
            );

            doc.close();

        } catch (Exception e) {

            log.error(
                    "PDF export failed for year {}: {}",
                    year,
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "PDF generation failed: " + e.getMessage(),
                    e
            );
        }

        return baos.toByteArray();
    }

    private void addTableHeaderCell(
            Table table,
            String text,
            DeviceRgb bg,
            PdfFont font
    ) {

        Cell cell = new Cell()
                .add(
                        new Paragraph(text)
                                .setFont(font)
                                .setFontColor(ColorConstants.WHITE)
                )
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER);

        table.addHeaderCell(cell);
    }

    private void addTableDataCell(
            Table table,
            String text,
            DeviceRgb bg,
            PdfFont font
    ) {

        Cell cell = new Cell()
                .add(
                        new Paragraph(text)
                                .setFont(font)
                )
                .setBackgroundColor(bg);

        table.addCell(cell);
    }

    private String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        if (
                value.contains(",") ||
                        value.contains("\"") ||
                        value.contains("\n")
        ) {

            return "\"" +
                    value.replace("\"", "\"\"") +
                    "\"";
        }

        return value;
    }
}