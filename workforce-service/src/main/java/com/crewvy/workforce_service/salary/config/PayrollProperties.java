package com.crewvy.workforce_service.salary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "payroll") // "payroll" 접두사를 가진 설정을 바인딩
public class PayrollProperties {

    private Rates rates = new Rates();
    private Constants constants = new Constants();

    // Getters and Setters
    public Rates getRates() { return rates; }
    public void setRates(Rates rates) { this.rates = rates; }
    public Constants getConstants() { return constants; }
    public void setConstants(Constants constants) { this.constants = constants; }

    public static class Rates {
        private BigDecimal nationalPension = new BigDecimal("0.045");
        private BigDecimal healthInsurance = new BigDecimal("0.03545");
        private BigDecimal longTermCareInsurance = new BigDecimal("0.1295");
        private BigDecimal employmentInsurance = new BigDecimal("0.009");
        private BigDecimal localIncomeTax = new BigDecimal("0.1");

        // Getters and Setters
        public BigDecimal getNationalPension() { return nationalPension; }
        public void setNationalPension(BigDecimal nationalPension) { this.nationalPension = nationalPension; }
        public BigDecimal getHealthInsurance() { return healthInsurance; }
        public void setHealthInsurance(BigDecimal healthInsurance) { this.healthInsurance = healthInsurance; }
        public BigDecimal getLongTermCareInsurance() { return longTermCareInsurance; }
        public void setLongTermCareInsurance(BigDecimal longTermCareInsurance) { this.longTermCareInsurance = longTermCareInsurance; }
        public BigDecimal getEmploymentInsurance() { return employmentInsurance; }
        public void setEmploymentInsurance(BigDecimal employmentInsurance) { this.employmentInsurance = employmentInsurance; }
        public BigDecimal getLocalIncomeTax() { return localIncomeTax; }
        public void setLocalIncomeTax(BigDecimal localIncomeTax) { this.localIncomeTax = localIncomeTax; }
    }

    public static class Constants {
        private BigDecimal monthlyHours = new BigDecimal("209");

        // Getter and Setter
        public BigDecimal getMonthlyHours() { return monthlyHours; }
        public void setMonthlyHours(BigDecimal monthlyHours) { this.monthlyHours = monthlyHours; }
    }
}
