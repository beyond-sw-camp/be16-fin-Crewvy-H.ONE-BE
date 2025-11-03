package com.crewvy.workforce_service.salary.config;

import com.crewvy.workforce_service.salary.entity.Holidays;
import com.crewvy.workforce_service.salary.repository.HolidayRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidayDataSeeder implements ApplicationRunner {

    private final HolidayRepository holidayRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (holidayRepository.count() == 0) {

            ClassPathResource resource = new ClassPathResource("data/holidays.json");

            try (InputStream inputStream = resource.getInputStream()) {

                List<Holidays> holidays = objectMapper.readValue(inputStream, new TypeReference<>() {
                });

                holidayRepository.saveAll(holidays);

                log.info("공휴일 데이터 {}건 DB 저장 완료", holidays.size());
            } catch (Exception e) {
                log.info("공휴일 데이터 로딩 실패: {}", e.getMessage());
            }
        } else {
            log.info("공휴일 데이터가 이미 DB에 존재, Data Seeding Pass");
        }
    }
}
