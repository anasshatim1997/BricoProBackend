package com.bricopro.i18n.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/i18n")
@Tag(name = "Internationalization")
public class I18nController {

    private static final Map<String, Map<String, String>> TRANSLATIONS = Map.of(
        "fr", Map.of(
            "app.name",            "BricoPro",
            "home.title",          "Services à domicile au Maroc",
            "search.placeholder",  "Quel service recherchez-vous ?",
            "book.cta",            "Réserver un prestataire",
            "task.urgent",         "Urgent",
            "payment.cash",        "Paiement en espèces",
            "payment.online",      "Paiement en ligne",
            "rating.label",        "Note",
            "distance.km",         "km"
        ),
        "ar", Map.of(
            "app.name",            "بريكو برو",
            "home.title",          "خدمات منزلية في المغرب",
            "search.placeholder",  "ما الخدمة التي تبحث عنها؟",
            "book.cta",            "احجز مقدم خدمة",
            "task.urgent",         "عاجل",
            "payment.cash",        "دفع نقدي",
            "payment.online",      "دفع إلكتروني",
            "rating.label",        "التقييم",
            "distance.km",         "كم"
        )
    );

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{lang}")
    @Operation(summary = "Get UI translations for a language (fr or ar)")
    public ResponseEntity<Map<String, String>> translations(
            @Parameter(name = "lang", description = "Language code: fr or ar", required = true, in = ParameterIn.PATH) @PathVariable String lang) {
        Map<String, String> t = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.get("fr"));
        return ResponseEntity.ok(t);
    }
}
