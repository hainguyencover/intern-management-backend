package com.holaho.intern.reporting.service.generator;

import com.holaho.intern.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportGeneratorRegistry {

    private final Map<String, ReportGenerator> generators;

    public ReportGeneratorRegistry(List<ReportGenerator> generatorList) {
        this.generators = generatorList.stream()
                .collect(Collectors.toMap(
                        ReportGenerator::getReportCode,
                        Function.identity()
                ));
    }

    public ReportGenerator get(String reportCode) {
        ReportGenerator generator = generators.get(reportCode);
        if (generator == null) {
            throw new ResourceNotFoundException("Không tìm thấy loại báo cáo với mã: " + reportCode);
        }
        return generator;
    }

    public Collection<ReportGenerator> getAllGenerators() {
        return generators.values();
    }
}
