package com.mryqr.core.qr.eventhandler;

import com.mryqr.core.common.domain.event.DomainEvent;
import com.mryqr.core.common.domain.event.DomainEventHandler;
import com.mryqr.core.common.utils.MryTaskRunner;
import com.mryqr.core.plate.domain.task.CountPlateForTenantTask;
import com.mryqr.core.qr.domain.QrCreatedEvent;
import com.mryqr.core.qr.domain.task.CountQrForAppTask;
import com.mryqr.core.qr.domain.task.SyncAttributeValuesForQrTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.mryqr.core.common.domain.event.DomainEventType.QR_CREATED;

@Slf4j
@Component
@RequiredArgsConstructor
public class QrCreatedEventHandler implements DomainEventHandler {
    private final CountQrForAppTask countQrForAppTask;
    private final SyncAttributeValuesForQrTask syncAttributeValuesForQrTask;
    private final CountPlateForTenantTask countPlateForTenantTask;

    @Override
    public boolean canHandle(DomainEvent domainEvent) {
        return domainEvent.getType() == QR_CREATED;
    }

    @Override
    public void handle(DomainEvent domainEvent, MryTaskRunner taskRunner) {
        QrCreatedEvent event = (QrCreatedEvent) domainEvent;
        taskRunner.run(() -> countQrForAppTask.run(event.getAppId(), event.getArTenantId()));
        taskRunner.run(() -> countPlateForTenantTask.run(event.getArTenantId()));
        taskRunner.run(() -> syncAttributeValuesForQrTask.run(event.getQrId()));
    }
}
