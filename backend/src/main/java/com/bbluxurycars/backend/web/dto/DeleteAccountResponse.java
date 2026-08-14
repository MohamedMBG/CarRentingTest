package com.bbluxurycars.backend.web.dto;

import java.time.Instant;

/** Body of {@code POST /v1/user/delete}. */
public record DeleteAccountResponse(boolean deleted, Instant deletedAt) {
}
