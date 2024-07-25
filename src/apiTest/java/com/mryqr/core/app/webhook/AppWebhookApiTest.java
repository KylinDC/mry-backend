package com.mryqr.core.app.webhook;

import com.mryqr.BaseApiTest;
import com.mryqr.common.webhook.qr.QrCreatedWebhookPayload;
import com.mryqr.common.webhook.qr.QrDeletedWebhookPayload;
import com.mryqr.common.webhook.qr.QrUpdatedWebhookPayload;
import com.mryqr.common.webhook.submission.SubmissionApprovedWebhookPayload;
import com.mryqr.common.webhook.submission.SubmissionCreatedWebhookPayload;
import com.mryqr.common.webhook.submission.SubmissionDeletedWebhookPayload;
import com.mryqr.common.webhook.submission.SubmissionUpdatedWebhookPayload;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.command.UpdateAppWebhookSettingCommand;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.AppSetting;
import com.mryqr.core.app.domain.QrWebhookType;
import com.mryqr.core.app.domain.WebhookSetting;
import com.mryqr.core.app.domain.config.AppConfig;
import com.mryqr.core.app.domain.page.control.FSingleLineTextControl;
import com.mryqr.core.app.domain.page.setting.ApprovalSetting;
import com.mryqr.core.app.domain.page.setting.PageSetting;
import com.mryqr.core.app.domain.page.setting.SubmissionWebhookType;
import com.mryqr.core.qr.QrApi;
import com.mryqr.core.qr.command.CreateQrResponse;
import com.mryqr.core.qr.domain.QR;
import com.mryqr.core.submission.SubmissionApi;
import com.mryqr.core.submission.domain.Submission;
import com.mryqr.utils.PreparedAppResponse;
import com.mryqr.utils.PreparedQrResponse;
import com.mryqr.utils.apitest.ApiTestingWebhookController;
import org.apache.commons.collections4.MapUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.springframework.test.util.ReflectionTestUtils;

import static com.google.common.collect.Lists.newArrayList;
import static com.mryqr.common.webhook.WebhookPayloadType.QR_CREATED;
import static com.mryqr.common.webhook.WebhookPayloadType.QR_DELETED;
import static com.mryqr.common.webhook.WebhookPayloadType.QR_UPDATED;
import static com.mryqr.common.webhook.WebhookPayloadType.SUBMISSION_APPROVED;
import static com.mryqr.common.webhook.WebhookPayloadType.SUBMISSION_CREATED;
import static com.mryqr.common.webhook.WebhookPayloadType.SUBMISSION_DELETED;
import static com.mryqr.common.webhook.WebhookPayloadType.SUBMISSION_UPDATED;
import static com.mryqr.core.app.domain.config.AppLandingPageType.DEFAULT;
import static com.mryqr.core.app.domain.page.setting.SubmissionWebhookType.ON_APPROVAL;
import static com.mryqr.core.app.domain.page.setting.SubmissionWebhookType.ON_UPDATE;
import static com.mryqr.core.common.domain.event.DomainEventType.QR_RENAMED;
import static com.mryqr.core.common.domain.permission.Permission.AS_TENANT_MEMBER;
import static com.mryqr.core.common.domain.permission.Permission.CAN_MANAGE_APP;
import static com.mryqr.core.plan.domain.PlanType.FLAGSHIP;
import static com.mryqr.core.plan.domain.PlanType.FREE;
import static com.mryqr.utils.RandomTestFixture.defaultPageSettingBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultSingleLineTextControl;
import static com.mryqr.utils.RandomTestFixture.rAnswer;
import static com.mryqr.utils.RandomTestFixture.rImageFile;
import static com.mryqr.utils.RandomTestFixture.rQrName;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.util.Base64.getEncoder;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

@Execution(SAME_THREAD)
public class AppWebhookApiTest extends BaseApiTest {

    @Test
    public void should_call_webhook_when_create_submission() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        FSingleLineTextControl control = defaultSingleLineTextControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        PageSetting pageSetting = defaultPageSettingBuilder()
                .submissionWebhookTypes(newArrayList(SubmissionWebhookType.ON_CREATE))
                .build();
        String appId = response.appId();
        AppApi.updateAppHomePageSetting(response.jwt(), appId, pageSetting);

        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));
        Submission submission = submissionRepository.byId(submissionId);

        SubmissionCreatedWebhookPayload lastPayload = (SubmissionCreatedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(submission.getId(), lastPayload.getSubmissionId());
        assertEquals(SUBMISSION_CREATED, lastPayload.getType());
        assertEquals(submission.getQrId(), lastPayload.getQrId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getGroupId(), lastPayload.getGroupId());
        assertEquals(submission.getAppId(), lastPayload.getAppId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getAnswers().values().stream().toList(), lastPayload.getAnswers());
        assertEquals(submission.getTenantId(), lastPayload.getTenantId());
        assertEquals(submission.getCreatedAt(), lastPayload.getCreatedAt());
        assertEquals(submission.getCreatedBy(), lastPayload.getCreatedBy());
    }

    @Test
    public void should_call_webhook_when_update_submission() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        FSingleLineTextControl control = defaultSingleLineTextControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        PageSetting pageSetting = defaultPageSettingBuilder()
                .submissionWebhookTypes(newArrayList(ON_UPDATE))
                .build();
        String appId = response.appId();
        AppApi.updateAppHomePageSetting(response.jwt(), appId, pageSetting);

        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));
        SubmissionApi.updateSubmission(response.jwt(), submissionId, rAnswer(control));
        Submission submission = submissionRepository.byId(submissionId);

        SubmissionUpdatedWebhookPayload lastPayload = (SubmissionUpdatedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(submission.getId(), lastPayload.getSubmissionId());
        assertEquals(SUBMISSION_UPDATED, lastPayload.getType());
        assertEquals(submission.getQrId(), lastPayload.getQrId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getGroupId(), lastPayload.getGroupId());
        assertEquals(submission.getAppId(), lastPayload.getAppId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getAnswers().values().stream().toList(), lastPayload.getAnswers());
        assertEquals(submission.getCreatedAt(), lastPayload.getCreatedAt());
        assertEquals(submission.getCreatedBy(), lastPayload.getCreatedBy());
        assertNotNull(lastPayload.getUpdatedAt());
    }


    @Test
    public void should_call_webhook_when_approve_submission() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        FSingleLineTextControl control = defaultSingleLineTextControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        PageSetting pageSetting = defaultPageSettingBuilder()
                .submissionWebhookTypes(newArrayList(ON_APPROVAL))
                .approvalSetting(ApprovalSetting.builder().approvalEnabled(true).permission(CAN_MANAGE_APP).build())
                .build();
        String appId = response.appId();
        AppApi.updateAppHomePageSetting(response.jwt(), appId, pageSetting);

        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));
        SubmissionApi.approveSubmission(response.jwt(), submissionId, true);
        Submission submission = submissionRepository.byId(submissionId);

        SubmissionApprovedWebhookPayload lastPayload = (SubmissionApprovedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(submission.getId(), lastPayload.getSubmissionId());
        assertEquals(SUBMISSION_APPROVED, lastPayload.getType());
        assertEquals(submission.getQrId(), lastPayload.getQrId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getGroupId(), lastPayload.getGroupId());
        assertEquals(submission.getAppId(), lastPayload.getAppId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getAnswers().values().stream().toList(), lastPayload.getAnswers());
        assertEquals(submission.getCreatedAt(), lastPayload.getCreatedAt());
        assertEquals(submission.getCreatedBy(), lastPayload.getCreatedBy());
        assertEquals(submission.getApproval(), lastPayload.getApproval());
    }

    @Test
    public void should_call_webhook_when_delete_submission() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        FSingleLineTextControl control = defaultSingleLineTextControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        PageSetting pageSetting = defaultPageSettingBuilder()
                .submissionWebhookTypes(newArrayList(SubmissionWebhookType.ON_DELETE))
                .build();
        String appId = response.appId();
        AppApi.updateAppHomePageSetting(response.jwt(), appId, pageSetting);

        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));
        Submission submission = submissionRepository.byId(submissionId);
        SubmissionApi.deleteSubmission(response.jwt(), submissionId);

        SubmissionDeletedWebhookPayload lastPayload = (SubmissionDeletedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(submission.getId(), lastPayload.getSubmissionId());
        assertEquals(SUBMISSION_DELETED, lastPayload.getType());
        assertEquals(submission.getQrId(), lastPayload.getQrId());
        assertEquals(submission.getPlateId(), lastPayload.getPlateId());
        assertEquals(submission.getPageId(), lastPayload.getPageId());
        assertEquals(submission.getAppId(), lastPayload.getAppId());
    }

    @Test
    public void should_call_webhook_when_create_qr() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        AppConfig config = AppConfig.builder()
                .homePageId(setting.homePageId())
                .operationPermission(AS_TENANT_MEMBER)
                .landingPageType(DEFAULT)
                .qrWebhookTypes(newArrayList(QrWebhookType.ON_CREATE))
                .geolocationEnabled(true)
                .plateBatchEnabled(true)
                .icon(rImageFile())
                .instanceAlias("设备")
                .groupAlias("车间")
                .allowDuplicateInstanceName(true)
                .build();

        ReflectionTestUtils.setField(setting, "config", config);
        AppApi.updateAppSetting(response.jwt(), response.appId(), setting);

        CreateQrResponse qrResponse = QrApi.createQr(response.jwt(), response.defaultGroupId());
        QR qr = qrRepository.byId(qrResponse.getQrId());

        QrCreatedWebhookPayload lastPayload = (QrCreatedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(QR_CREATED, lastPayload.getType());

        assertEquals(qr.getName(), lastPayload.getName());
        assertEquals(qr.getId(), lastPayload.getQrId());
        assertEquals(qr.getPlateId(), lastPayload.getPlateId());
        assertEquals(qr.getAppId(), lastPayload.getAppId());
        assertEquals(qr.getGroupId(), lastPayload.getGroupId());
        assertEquals(qr.isTemplate(), lastPayload.isTemplate());
        assertEquals(qr.getHeaderImage(), lastPayload.getHeaderImage());
        assertEquals(qr.getDescription(), lastPayload.getDescription());
        assertEquals(MapUtils.emptyIfNull(qr.getAttributeValues()).values().stream().toList(), lastPayload.getAttributeValues());
        assertEquals(qr.getAccessCount(), lastPayload.getAccessCount());
        assertEquals(qr.getLastAccessedAt(), lastPayload.getLastAccessedAt());
        assertEquals(qr.getGeolocation(), lastPayload.getGeolocation());
        assertEquals(qr.getCustomId(), lastPayload.getCustomId());
        assertEquals(qr.getTenantId(), lastPayload.getTenantId());
        assertEquals(qr.getCreatedAt(), lastPayload.getCreatedAt());
        assertEquals(qr.getCreatedBy(), lastPayload.getCreatedBy());
    }


    @Test
    public void should_call_webhook_when_update_qr() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        AppConfig config = AppConfig.builder()
                .homePageId(setting.homePageId())
                .operationPermission(AS_TENANT_MEMBER)
                .landingPageType(DEFAULT)
                .qrWebhookTypes(newArrayList(QrWebhookType.ON_UPDATE))
                .geolocationEnabled(true)
                .plateBatchEnabled(true)
                .icon(rImageFile())
                .instanceAlias("设备")
                .groupAlias("车间")
                .allowDuplicateInstanceName(true)
                .build();

        ReflectionTestUtils.setField(setting, "config", config);
        AppApi.updateAppSetting(response.jwt(), response.appId(), setting);

        QrApi.renameQr(response.jwt(), response.qrId(), rQrName());
        QR qr = qrRepository.byId(response.qrId());

        QrUpdatedWebhookPayload lastPayload = (QrUpdatedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(QR_UPDATED, lastPayload.getType());

        assertEquals(qr.getName(), lastPayload.getName());
        assertEquals(qr.getId(), lastPayload.getQrId());
        assertEquals(qr.getPlateId(), lastPayload.getPlateId());
        assertEquals(qr.getAppId(), lastPayload.getAppId());
        assertEquals(qr.getGroupId(), lastPayload.getGroupId());
        assertEquals(qr.isTemplate(), lastPayload.isTemplate());
        assertEquals(qr.getHeaderImage(), lastPayload.getHeaderImage());
        assertEquals(qr.getDescription(), lastPayload.getDescription());
        assertEquals(MapUtils.emptyIfNull(qr.getAttributeValues()).values().stream().toList(), lastPayload.getAttributeValues());
        assertEquals(qr.getAccessCount(), lastPayload.getAccessCount());
        assertEquals(qr.getLastAccessedAt(), lastPayload.getLastAccessedAt());
        assertEquals(qr.getGeolocation(), lastPayload.getGeolocation());
        assertEquals(qr.getCustomId(), lastPayload.getCustomId());
        assertEquals(qr.getTenantId(), lastPayload.getTenantId());
        assertEquals(qr.getCreatedAt(), lastPayload.getCreatedAt());
        assertEquals(qr.getCreatedBy(), lastPayload.getCreatedBy());
        assertEquals(QR_RENAMED, lastPayload.getUpdateType());
        assertNotNull(lastPayload.getUpdatedAt());
    }


    @Test
    public void should_call_webhook_when_delete_qr() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));
        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        AppConfig config = AppConfig.builder()
                .homePageId(setting.homePageId())
                .operationPermission(AS_TENANT_MEMBER)
                .landingPageType(DEFAULT)
                .qrWebhookTypes(newArrayList(QrWebhookType.ON_DELETE))
                .geolocationEnabled(true)
                .plateBatchEnabled(true)
                .icon(rImageFile())
                .instanceAlias("设备")
                .groupAlias("车间")
                .allowDuplicateInstanceName(true)
                .build();

        ReflectionTestUtils.setField(setting, "config", config);
        AppApi.updateAppSetting(response.jwt(), response.appId(), setting);
        QR qr = qrRepository.byId(response.qrId());

        QrApi.deleteQr(response.jwt(), response.qrId());

        QrDeletedWebhookPayload lastPayload = (QrDeletedWebhookPayload) ApiTestingWebhookController.lastPayload;
        assertEquals(authString, ApiTestingWebhookController.lastAuthString);
        assertEquals(QR_DELETED, lastPayload.getType());

        assertEquals(qr.getId(), lastPayload.getQrId());
        assertEquals(qr.getPlateId(), lastPayload.getPlateId());
        assertEquals(qr.getAppId(), lastPayload.getAppId());
        assertEquals(qr.getGroupId(), lastPayload.getGroupId());
    }

    @Test
    public void should_not_call_webhook_if_plan_not_allowed() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(true)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        PageSetting pageSetting = defaultPageSettingBuilder()
                .submissionWebhookTypes(newArrayList(SubmissionWebhookType.ON_CREATE))
                .build();
        AppApi.updateAppHomePageSetting(response.jwt(), response.appId(), pageSetting);
        setupApi.updateTenantPackages(response.tenantId(), FREE);

        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId());
        assertNotEquals(authString, ApiTestingWebhookController.lastAuthString);
    }

    @Test
    public void should_not_call_webhook_if_not_enabled() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        String username = randomAlphanumeric(10);
        String password = randomAlphanumeric(10);
        String authString = "Basic " + getEncoder().encodeToString((username + ":" + password).getBytes(US_ASCII));

        AppApi.updateWebhookSetting(response.jwt(), response.appId(), UpdateAppWebhookSettingCommand.builder()
                .webhookSetting(WebhookSetting.builder()
                        .enabled(false)
                        .url("http://localhost:" + port + "/api-testing/webhook")
                        .username(username)
                        .password(password)
                        .build())
                .build());

        PageSetting pageSetting = defaultPageSettingBuilder()
                .submissionWebhookTypes(newArrayList(SubmissionWebhookType.ON_CREATE))
                .build();
        AppApi.updateAppHomePageSetting(response.jwt(), response.appId(), pageSetting);

        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId());
        assertNotEquals(authString, ApiTestingWebhookController.lastAuthString);
    }

}
