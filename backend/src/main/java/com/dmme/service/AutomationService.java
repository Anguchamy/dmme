package com.dmme.service;

import com.dmme.domain.Automation;
import com.dmme.domain.FlowStep;
import com.dmme.domain.TriggerKeyword;
import com.dmme.repository.AutomationRepository;
import com.dmme.repository.FlowStepRepository;
import com.dmme.repository.TriggerKeywordRepository;
import com.dmme.web.dto.AutomationDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AutomationService {

    private final AutomationRepository automations;
    private final TriggerKeywordRepository keywords;
    private final FlowStepRepository steps;

    public AutomationService(AutomationRepository automations,
                             TriggerKeywordRepository keywords,
                             FlowStepRepository steps) {
        this.automations = automations;
        this.keywords = keywords;
        this.steps = steps;
    }

    public List<AutomationView> list(UUID userId) {
        return automations.findByUserId(userId).stream().map(a -> view(a.getId(), userId)).toList();
    }

    public AutomationView get(Long id, UUID userId) {
        automations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Automation not found"));
        return view(id, userId);
    }

    @Transactional
    public AutomationView create(AutomationUpsert req, UUID userId) {
        Automation a = new Automation();
        a.setUserId(userId);
        apply(a, req);
        automations.save(a);
        replaceChildren(a.getId(), req);
        return view(a.getId(), userId);
    }

    @Transactional
    public AutomationView update(Long id, AutomationUpsert req, UUID userId) {
        Automation a = automations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Automation not found"));
        apply(a, req);
        automations.save(a);
        replaceChildren(id, req);
        return view(id, userId);
    }

    @Transactional
    public void delete(Long id, UUID userId) {
        Automation a = automations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Automation not found"));
        keywords.deleteByAutomationId(id);
        steps.deleteByAutomationId(id);
        automations.delete(a);
    }

    @Transactional
    public AutomationView setStatus(Long id, String status, UUID userId) {
        Automation a = automations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Automation not found"));
        a.setStatus(status);
        automations.save(a);
        return view(id, userId);
    }

    private void apply(Automation a, AutomationUpsert req) {
        a.setName(req.name());
        a.setType(req.type() != null ? req.type() : "COMMENT");
        a.setStatus(req.status() != null ? req.status() : "DRAFT");
        a.setIgAccountId(req.igAccountId());
        a.setIgMediaId(req.igMediaId());
        a.setMatchAny(req.matchAny());
        a.setPublicReply(req.publicReply());
        a.setAskFollowEnabled(req.askFollowEnabled());
        a.setAskFollowMessage(req.askFollowMessage());
    }

    private void replaceChildren(Long automationId, AutomationUpsert req) {
        keywords.deleteByAutomationId(automationId);
        steps.deleteByAutomationId(automationId);

        if (req.keywords() != null) {
            for (KeywordDto k : req.keywords()) {
                TriggerKeyword tk = new TriggerKeyword();
                tk.setAutomationId(automationId);
                tk.setKeyword(k.keyword());
                tk.setMatchType(k.matchType() != null ? k.matchType() : "CONTAINS");
                keywords.save(tk);
            }
        }
        if (req.steps() != null) {
            for (StepDto s : req.steps()) {
                FlowStep fs = new FlowStep();
                fs.setAutomationId(automationId);
                fs.setStepOrder(s.stepOrder());
                fs.setStepType(s.stepType() != null ? s.stepType() : "TEXT");
                fs.setBody(s.body());
                fs.setButtons(s.buttons());
                fs.setCollectField(s.collectField());
                fs.setCustomKey(s.customKey());
                steps.save(fs);
            }
        }
    }

    private AutomationView view(Long id, UUID userId) {
        Automation a = automations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Automation not found"));
        List<KeywordDto> kw = keywords.findByAutomationId(id).stream()
                .map(k -> new KeywordDto(k.getKeyword(), k.getMatchType())).toList();
        List<StepDto> st = steps.findByAutomationIdOrderByStepOrderAsc(id).stream()
                .map(s -> new StepDto(s.getStepOrder(), s.getStepType(), s.getBody(),
                        s.getButtons(), s.getCollectField(), s.getCustomKey())).toList();
        return new AutomationView(a.getId(), a.getName(), a.getType(), a.getStatus(),
                a.getIgAccountId(), a.getIgMediaId(), a.isMatchAny(), a.getPublicReply(),
                a.isAskFollowEnabled(), a.getAskFollowMessage(), kw, st,
                a.getCreatedAt(), a.getUpdatedAt());
    }
}
