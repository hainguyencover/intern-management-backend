package com.holaho.intern.notification.service;

import com.holaho.intern.notification.dto.NotificationPreferenceDto;
import com.holaho.intern.notification.entity.NotificationPreference;
import com.holaho.intern.notification.repository.NotificationPreferenceRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<NotificationPreference> getPreferencesByUserId(Long userId) {
        return preferenceRepository.findByUser_Id(userId);
    }

    @Transactional
    public void updatePreference(Long userId, NotificationPreferenceDto.Request request) {
        Optional<NotificationPreference> opt = preferenceRepository.findByUser_IdAndEventType(userId, request.getEventType());
        NotificationPreference pref;
        if (opt.isPresent()) {
            pref = opt.get();
        } else {
            pref = new NotificationPreference();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found: " + userId));
            pref.setUser(user);
            pref.setEventType(request.getEventType());
        }
        pref.setEmailEnabled(request.isEmailEnabled());
        pref.setWebsocketEnabled(request.isWebsocketEnabled());
        pref.setInAppEnabled(request.isInAppEnabled());
        preferenceRepository.save(pref);
        log.info("Updated notification preference for user: {}, event: {}", userId, request.getEventType());
    }
}
