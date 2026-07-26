package com.dapfintech.auth.dto.response;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
	
	private UUID userId;

	 private String accessToken;

	    private String refreshToken;

	    private String tokenType;

	    private String fullName;

	    private String role;
}
