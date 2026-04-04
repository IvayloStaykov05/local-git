package com.example.project.backend.repository;

import com.example.project.backend.model.entity.Notification;
import com.example.project.backend.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User user);

    List<Notification> findByRecipientAndIsReadFalse(User user);
}
