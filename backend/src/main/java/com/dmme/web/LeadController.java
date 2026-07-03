package com.dmme.web;

import com.dmme.domain.Lead;
import com.dmme.repository.LeadRepository;
import com.dmme.service.CurrentUserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadRepository leads;
    private final CurrentUserService currentUser;

    public LeadController(LeadRepository leads, CurrentUserService currentUser) {
        this.leads = leads;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Lead> list() {
        return leads.findByUserIdOrderByCreatedAtDesc(currentUser.userId());
    }

    /** Export collected leads as CSV. */
    @GetMapping("/export")
    public void export(HttpServletResponse res) throws IOException {
        res.setContentType("text/csv");
        res.setHeader("Content-Disposition", "attachment; filename=leads.csv");
        var writer = res.getWriter();
        writer.println("ig_username,name,email,phone,created_at");
        for (Lead l : leads.findByUserIdOrderByCreatedAtDesc(currentUser.userId())) {
            writer.printf("%s,%s,%s,%s,%s%n",
                    nz(l.getIgUsername()), nz(l.getName()), nz(l.getEmail()),
                    nz(l.getPhone()), l.getCreatedAt());
        }
        writer.flush();
    }

    private String nz(String s) {
        return s == null ? "" : s.replace(",", " ");
    }
}
