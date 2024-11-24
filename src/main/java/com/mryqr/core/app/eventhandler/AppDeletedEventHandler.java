package com.mryqr.core.app.eventhandler;

import com.mryqr.core.app.domain.event.AppDeletedEvent;
import com.mryqr.core.appmanual.domain.task.RemoveManualForAppTask;
import com.mryqr.core.assignment.domain.task.RemoveAllAssignmentsUnderAppTask;
import com.mryqr.core.assignmentplan.domain.task.RemoveAllAssignmentPlansUnderAppTask;
import com.mryqr.core.common.domain.event.DomainEvent;
import com.mryqr.core.common.domain.event.DomainEventHandler;
import com.mryqr.core.common.utils.MryTaskRunner;
import com.mryqr.core.group.domain.task.RemoveAllGroupsForAppTask;
import com.mryqr.core.grouphierarchy.domain.task.RemoveGroupHierarchyForAppTask;
import com.mryqr.core.plate.domain.task.RemoveAllPlatesUnderAppTask;
import com.mryqr.core.platebatch.domain.task.RemoveAllPlateBatchesUnderAppTask;
import com.mryqr.core.qr.domain.task.RemoveAllQrsUnderAppTask;
import com.mryqr.core.submission.domain.task.RemoveAllSubmissionsForAppTask;
import com.mryqr.core.tenant.domain.task.CountAppForTenantTask;
import com.mryqr.core.tenant.domain.task.RemoveAppUsageFromTenantTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.mryqr.core.common.domain.event.DomainEventType.APP_DELETED;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppDeletedEventHandler implements DomainEventHandler {
    private final RemoveAllSubmissionsForAppTask removeAllSubmissionsForAppTask;
    private final RemoveAllQrsUnderAppTask removeAllQrsUnderAppTask;
    private final RemoveAllPlatesUnderAppTask removeAllPlatesUnderAppTask;
    private final RemoveAllPlateBatchesUnderAppTask removeAllPlateBatchesUnderAppTask;
    private final RemoveAppUsageFromTenantTask removeAppUsageFromTenantTask;
    private final RemoveManualForAppTask removeManualForAppTask;
    private final RemoveAllAssignmentPlansUnderAppTask removeAllAssignmentPlansUnderAppTask;
    private final RemoveAllAssignmentsUnderAppTask removeAllAssignmentsUnderAppTask;
    private final RemoveAllGroupsForAppTask removeAllGroupsForAppTask;
    private final RemoveGroupHierarchyForAppTask removeGroupHierarchyForAppTask;
    private final CountAppForTenantTask countAppForTenantTask;

    @Override
    public boolean canHandle(DomainEvent domainEvent) {
        return domainEvent.getType() == APP_DELETED;
    }

    @Override
    public void handle(DomainEvent domainEvent) {
        AppDeletedEvent event = (AppDeletedEvent) domainEvent;
        String appId = event.getAppId();
        MryTaskRunner.run(() -> removeAllSubmissionsForAppTask.run(appId));
        MryTaskRunner.run(() -> removeAllQrsUnderAppTask.run(appId));
        MryTaskRunner.run(() -> removeAllGroupsForAppTask.run(appId));
        MryTaskRunner.run(() -> removeGroupHierarchyForAppTask.run(appId));
        MryTaskRunner.run(() -> removeAllPlatesUnderAppTask.run(appId));
        MryTaskRunner.run(() -> removeAllPlateBatchesUnderAppTask.run(appId));
        MryTaskRunner.run(() -> removeAppUsageFromTenantTask.run(event.getArTenantId(), appId));
        MryTaskRunner.run(() -> removeManualForAppTask.run(appId));
        MryTaskRunner.run(() -> removeAllAssignmentPlansUnderAppTask.run(appId));
        MryTaskRunner.run(() -> removeAllAssignmentsUnderAppTask.run(appId));
        MryTaskRunner.run(() -> countAppForTenantTask.run(event.getArTenantId()));
    }

}
