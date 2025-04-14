package io.github.vizanarkonin.nyx.Models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "authorities")
@Getter @Setter @NoArgsConstructor
@IdClass(AuthorityId.class)
public class Authority {
    @Id
    String username;
    @Id
    String authority;
}
