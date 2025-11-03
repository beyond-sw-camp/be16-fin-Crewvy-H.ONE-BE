package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 근무지 마스터 엔티티
 * 본사, 지점, 출장지 등 근무 가능한 장소를 관리
 */
@Entity
@Table(name = "work_location")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "work_location_id", nullable = false)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;                // 근무지 명칭 (예: "서울 본사", "부산 지점")

    @Column(name = "address", length = 255)
    private String address;             // 주소

    @Column(name = "latitude")
    private Double latitude;            // 위도 (GPS)

    @Column(name = "longitude")
    private Double longitude;           // 경도 (GPS)

    @Column(name = "gps_radius")
    private Integer gpsRadius;          // GPS 허용 반경 (미터)

    @Column(name = "ip_address", length = 100)
    private String ipAddress;           // IP 주소 또는 IP 대역 (예: "192.168.1.0/24")

    @Column(name = "wifi_ssid", length = 100)
    private String wifiSsid;            // WiFi SSID (네트워크 이름)

    @Column(name = "wifi_bssid", length = 100)
    private String wifiBssid;           // WiFi BSSID (MAC 주소, 선택 사항)

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;           // 활성/비활성

    @Column(name = "description", length = 500)
    private String description;         // 설명 (선택 사항)

    /**
     * 근무지 정보 업데이트
     */
    public void updateInfo(String name, String address, Double latitude, Double longitude,
                          Integer gpsRadius, String ipAddress, String wifiSsid, String wifiBssid, String description) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.gpsRadius = gpsRadius;
        this.ipAddress = ipAddress;
        this.wifiSsid = wifiSsid;
        this.wifiBssid = wifiBssid;
        this.description = description;
    }

    /**
     * 활성/비활성 상태 변경
     */
    public void updateActiveStatus(Boolean isActive) {
        this.isActive = isActive;
    }
}
