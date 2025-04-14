package io.github.vizanarkonin.nyx.Models;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@EqualsAndHashCode @NoArgsConstructor
public class AuthorityId {
    private String username;
    private String authority;

    public AuthorityId(String username, String authority) {
        this.username = username;
        this.authority = authority;
    }
}