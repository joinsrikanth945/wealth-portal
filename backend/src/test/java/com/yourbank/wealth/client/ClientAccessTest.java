package com.yourbank.wealth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.yourbank.wealth.TestData;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ClientAccessTest {

    private final ClientRepository repo = mock(ClientRepository.class);
    private final ClientAccess access = new ClientAccess(repo);
    private final Client clara = TestData.client(1, "rm1", "client1");

    @Test
    void theRmSeesTheirOwnClient() {
        assertThat(access.canSee(clara, TestData.RM1)).isTrue();
        assertThat(access.canManage(clara, TestData.RM1)).isTrue();
    }

    @Test
    void anotherRmDoesNot() {
        assertThat(access.canSee(clara, TestData.RM2)).isFalse();
    }

    @Test
    void theClientSeesThemselvesButCannotManage() {
        assertThat(access.canSee(clara, TestData.CLIENT1)).isTrue();
        assertThat(access.canManage(clara, TestData.CLIENT1)).isFalse();
    }

    @Test
    void adminsSeeEveryone() {
        assertThat(access.canSee(clara, TestData.ADMIN)).isTrue();
        assertThat(access.canManage(clara, TestData.ADMIN)).isTrue();
    }

    @Test
    void forbiddenLooksLikeNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(clara));

        assertThatThrownBy(() -> access.requireVisible(1L, TestData.RM2))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
