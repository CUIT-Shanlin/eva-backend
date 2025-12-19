package edu.cuit.bc.evaluation.application.usecase;

import edu.cuit.bc.evaluation.application.port.DeleteEvaRecordRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeleteEvaRecordUseCaseTest {

    @Test
    void delete_shouldDelegate() {
        AtomicReference<List<Integer>> lastIds = new AtomicReference<>();
        DeleteEvaRecordRepository repo = lastIds::set;
        DeleteEvaRecordUseCase useCase = new DeleteEvaRecordUseCase(repo);

        useCase.delete(List.of(1, 2, 3));

        assertEquals(List.of(1, 2, 3), lastIds.get());
    }
}

