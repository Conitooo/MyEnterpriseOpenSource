package com.myenterpriseos.myenterpriseopensource;

import com.myenterpriseos.myenterpriseopensource.entity.AppUser;
import com.myenterpriseos.myenterpriseopensource.entity.Company;
import com.myenterpriseos.myenterpriseopensource.enums.UserRole;
import com.myenterpriseos.myenterpriseopensource.repository.AppUserRepository;
import com.myenterpriseos.myenterpriseopensource.repository.CompanyRepository;
import com.myenterpriseos.myenterpriseopensource.security.BootstrapAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapAdminTests {
    @Test
    void createsOnlyFirstAdminForExistingCompany() throws Exception {
        AppUserRepository users = mock(AppUserRepository.class);
        CompanyRepository companies = mock(CompanyRepository.class);
        PasswordEncoder passwords = mock(PasswordEncoder.class);
        Company company = new Company(); company.setId(7L);
        when(companies.findById(7L)).thenReturn(Optional.of(company));
        when(passwords.encode("LongPasswordExample2026!")).thenReturn("hashed");
        BootstrapAdmin bootstrap = new BootstrapAdmin(users, companies, passwords,
                "7", "", "admin", "LongPasswordExample2026!");
        bootstrap.run(new DefaultApplicationArguments(new String[0]));
        var saved = org.mockito.ArgumentCaptor.forClass(AppUser.class);
        verify(users).save(saved.capture());
        assertEquals(company, saved.getValue().getCompany());
        assertEquals("hashed", saved.getValue().getPasswordHash());
        assertEquals(UserRole.ADMIN, saved.getValue().getRole());

        reset(users, companies, passwords);
        when(users.count()).thenReturn(1L);
        bootstrap.run(new DefaultApplicationArguments(new String[0]));
        verify(users, never()).save(any());
        verifyNoInteractions(companies, passwords);
    }
}
