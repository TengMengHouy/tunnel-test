package app.stadoor.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TunnelInfoDto {
    private String subdomain;
    private String url;
    private int assignedPort;
    private String tokenName;
    private LocalDateTime connectedAt;
}
