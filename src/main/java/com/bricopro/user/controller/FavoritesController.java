package com.bricopro.user.controller;

import com.bricopro.user.entity.User;
import com.bricopro.user.service.FavoritesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients/me/favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final FavoritesService favoritesService;

    @GetMapping
    public List<User> getFavorites() {
        return favoritesService.getFavoriteWorkers();
    }

    @PostMapping("/{workerId}")
    public void addFavorite(@PathVariable Long workerId) {
        favoritesService.addFavorite(workerId);
    }

    @DeleteMapping("/{workerId}")
    public void removeFavorite(@PathVariable Long workerId) {
        favoritesService.removeFavorite(workerId);
    }
}