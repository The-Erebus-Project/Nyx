package io.github.vizanarkonin.nyx.Models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="projects")
@Getter @Setter
public class Project implements Comparable<Project> {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    private String name;
    private String description;
    // Need to specify that we want MariaDB's TinyInt(1) approach to Bools.
    @Column(nullable = false, columnDefinition = "BOOLEAN default 1")
    private boolean isStrict = true;

    @Override
    public int compareTo(Project o) {
        return Long.compare(id, o.getId());
    }
}
