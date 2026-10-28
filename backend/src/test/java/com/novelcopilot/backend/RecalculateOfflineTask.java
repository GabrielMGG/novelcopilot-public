package com.novelcopilot.backend;

import com.novelcopilot.backend.benchmark.controller.AiBenchmarkController;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("dev") // or whatever profile has the local DB
@Disabled("Tarefa manual: acessa e altera dados do banco configurado no profile dev.")
public class RecalculateOfflineTask {

    @Autowired
    private AiBenchmarkController controller;
    
    @Autowired
    private AiBenchmarkResponseRepository repository;

    @Test
    public void executeRecalculation() {
        System.out.println("=== START RECALCULATION ===");
        try {
            Map<String, Object> run3 = controller.recalculateCosts(3L);
            System.out.println("Run 3 Recalculate: " + run3);
            
            Map<String, Object> run4 = controller.recalculateCosts(4L);
            System.out.println("Run 4 Recalculate: " + run4);
            
            BigDecimal cost3 = (BigDecimal) run3.get("newTotalCostUsd");
            BigDecimal cost4 = (BigDecimal) run4.get("newTotalCostUsd");
            BigDecimal total = cost3.add(cost4);
            
            System.out.println("Total Cost Run 3: " + cost3);
            System.out.println("Total Cost Run 4: " + cost4);
            System.out.println("Sum: " + total);
            
            BigDecimal openRouterCost = new BigDecimal("0.0176");
            System.out.println("Difference: " + total.subtract(openRouterCost));
            
            System.out.println("DB STATE FOR RUN 3:");
            List<AiBenchmarkResponse> list3 = repository.findByRunId(3L);
            for(AiBenchmarkResponse r : list3) {
                System.out.println(r.getParticipantId() + " -> " + r.getEffectiveCostUsd() + " (" + r.getCostSource() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("=== END RECALCULATION ===");
    }
}
