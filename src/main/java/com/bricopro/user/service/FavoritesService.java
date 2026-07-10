package com.bricopro.user.service;

import com.bricopro.user.entity.ClientFavorite;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.ClientFavoriteRepository;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritesService {

    private final ClientFavoriteRepository favoriteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<User> getFavoriteWorkers() {
        User client = getCurrentUser();
        return favoriteRepository.findByClient(client)
                .stream()
                .map(ClientFavorite::getWorker)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addFavorite(Long workerId) {
        User client = getCurrentUser();
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        if (favoriteRepository.existsByClientAndWorker(client, worker)) {
            return; // already favorite
        }
        ClientFavorite fav = ClientFavorite.builder()
                .client(client)
                .worker(worker)
                .build();
        favoriteRepository.save(fav);
    }

    @Transactional
    public void removeFavorite(Long workerId) {
        User client = getCurrentUser();
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        favoriteRepository.deleteByClientAndWorker(client, worker);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}