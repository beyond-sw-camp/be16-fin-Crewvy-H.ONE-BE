package com.crewvy.workforce_service.salary.config;

import com.crewvy.workforce_service.salary.entity.Holidays;
import com.crewvy.workforce_service.salary.entity.IncomeTax;
import com.crewvy.workforce_service.salary.repository.IncomeTaxRepository;
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
public class IncomeTaxDataSeeder implements ApplicationRunner {

    private final IncomeTaxRepository incomeTaxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (incomeTaxRepository.count() == 0) {

            ClassPathResource resource = new ClassPathResource("data/incomeTax.json");

            try (InputStream inputStream = resource.getInputStream()) {

                List<IncomeTax> incomeTaxList = objectMapper.readValue(inputStream, new TypeReference<>() {
                });

                incomeTaxRepository.saveAll(incomeTaxList);

                log.info("근로소득 세액 데이터 {}건 DB 저장 완료", incomeTaxList.size());
            } catch (Exception e) {
                log.info("근로소득 세액  데이터 로딩 실패: {}", e.getMessage());
            }
        } else {
            log.info("근로소득 세액  데이터가 이미 DB에 존재, Data Seeding Pass");
        }
    }
}


