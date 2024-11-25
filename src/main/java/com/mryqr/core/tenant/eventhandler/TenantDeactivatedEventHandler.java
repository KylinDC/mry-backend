package com.mryqr.core.tenant.eventhandler;

import com.mryqr.common.event.consume.AbstractDomainEventHandler;
import com.mryqr.common.utils.MryTaskRunner;
import com.mryqr.core.member.domain.task.SyncTenantActiveStatusToMembersTask;
import com.mryqr.core.tenant.domain.event.TenantDeactivatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantDeactivatedEventHandler extends AbstractDomainEventHandler<TenantDeactivatedEvent> {
    private final SyncTenantActiveStatusToMembersTask syncTenantActiveStatusToMembersTask;

    @Override
    protected void doHandle(TenantDeactivatedEvent event) {
        MryTaskRunner.run(() -> syncTenantActiveStatusToMembersTask.run(event.getTenantId()));
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }
}
