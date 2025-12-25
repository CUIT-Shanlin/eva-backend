package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.port.DeleteEvaTemplateRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteEvaTemplateUseCaseTest {

    @Test
    void delete_shouldDelegate() {
        AtomicReference<List<Integer>> lastIds = new AtomicReference<>();
        DeleteEvaTemplateRepository repo = lastIds::set;
        DeleteEvaTemplateUseCase useCase = new DeleteEvaTemplateUseCase(repo);

        useCase.delete(List.of(10, 20));

        assertEquals(List.of(10, 20), lastIds.get());
    }
}

