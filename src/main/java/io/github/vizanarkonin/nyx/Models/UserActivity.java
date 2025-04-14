package io.github.vizanarkonin.nyx.Models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="user_activities")
@Getter @Setter
@NoArgsConstructor
public class UserActivity {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime timestamp;
    private String action;
    private String details;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", referencedColumnName = "id")
    @NotFound(action = NotFoundAction.IGNORE)
    private User user;

    public UserActivity(User user, String action, String details) {
        this.timestamp  = LocalDateTime.now();
        this.user       = user;
        this.action     = action;
        this.details    = details;
    }

    public String getTimeStampFormatted() {
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
