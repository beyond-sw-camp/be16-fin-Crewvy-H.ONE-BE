package com.crewvy.workforce_service.attendance.util;

public class DistanceCalculator {

    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * 두 지점의 위도, 경도를 사용하여 거리를 미터(m) 단위로 계산합니다.
     * @param lat1 지점 1의 위도
     * @param lon1 지점 1의 경도
     * @param lat2 지점 2의 위도
     * @param lon2 지점 2의 경도
     * @return 두 지점 간의 거리 (미터)
     */
    public static double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000; // 미터 단위로 변환하여 반환
    }
}
