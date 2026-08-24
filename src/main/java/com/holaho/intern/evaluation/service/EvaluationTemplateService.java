package com.holaho.intern.evaluation.service;

import com.holaho.intern.evaluation.dto.EvaluationTemplateResponse;
import com.holaho.intern.evaluation.entity.EvaluationCriterion;
import com.holaho.intern.evaluation.entity.EvaluationTemplate;
import com.holaho.intern.evaluation.enums.TemplateStatus;
import com.holaho.intern.evaluation.repository.EvaluationTemplateRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationTemplateService {

    private final EvaluationTemplateRepository templateRepository;

    /**
     * Find the active template for a program, falling back to the default template.
     */
    @Transactional(readOnly = true)
    public EvaluationTemplate findActiveTemplateForProgram(Long programId) {
        Long tenantId = getCurrentTenantId();

        // 1. Try program-specific template
        if (programId != null) {
            List<EvaluationTemplate> programTemplates =
                    templateRepository.findActiveByProgramId(tenantId, programId);
            if (!programTemplates.isEmpty()) {
                return programTemplates.get(0); // latest version
            }
        }

        // 2. Fall back to default (no program association)
        List<EvaluationTemplate> defaults = templateRepository.findDefaultActive(tenantId);
        if (!defaults.isEmpty()) {
            return defaults.get(0);
        }

        // 3. Last resort: any active template in tenant
        List<EvaluationTemplate> all = templateRepository.findByTenantIdAndStatus(tenantId, TemplateStatus.ACTIVE);
        if (!all.isEmpty()) {
            return all.get(0);
        }

        throw new NotFoundException("No active evaluation template found");
    }

    @Transactional(readOnly = true)
    public EvaluationTemplateResponse getById(Long id) {
        EvaluationTemplate template = templateRepository.findByIdWithCriteria(id)
                .orElseThrow(() -> new NotFoundException("EvaluationTemplate", id));
        return mapToResponse(template);
    }

    @Transactional(readOnly = true)
    public List<EvaluationTemplateResponse> listActiveTemplates() {
        Long tenantId = getCurrentTenantId();
        return templateRepository.findByTenantIdAndStatus(tenantId, TemplateStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ── Mapping ──

    public EvaluationTemplateResponse mapToResponse(EvaluationTemplate template) {
        List<EvaluationTemplateResponse.CriterionResponse> criteriaResponses =
                template.getCriteria().stream()
                        .map(c -> EvaluationTemplateResponse.CriterionResponse.builder()
                                .id(c.getId())
                                .category(c.getCategory().name())
                                .name(c.getName())
                                .description(c.getDescription())
                                .weight(c.getWeight())
                                .maxScore(c.getMaxScore())
                                .displayOrder(c.getDisplayOrder())
                                .required(c.getRequired())
                                .build())
                        .collect(Collectors.toList());

        return EvaluationTemplateResponse.builder()
                .id(template.getId())
                .programId(template.getProgram() != null ? template.getProgram().getId() : null)
                .programName(template.getProgram() != null ? template.getProgram().getName() : null)
                .name(template.getName())
                .description(template.getDescription())
                .evaluationPeriod(template.getEvaluationPeriod().name())
                .version(template.getVersion())
                .status(template.getStatus().name())
                .criteria(criteriaResponses)
                .createdAt(template.getCreatedAt())
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
