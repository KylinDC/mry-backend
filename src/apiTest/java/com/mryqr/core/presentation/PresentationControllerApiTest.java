package com.mryqr.core.presentation;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.page.control.FNumberInputControl;
import com.mryqr.core.app.domain.page.control.FSingleLineTextControl;
import com.mryqr.core.app.domain.page.control.PSubmissionReferenceControl;
import com.mryqr.core.app.domain.page.control.PTimeSegmentControl;
import com.mryqr.core.common.domain.display.TextDisplayValue;
import com.mryqr.core.member.MemberApi;
import com.mryqr.core.presentation.query.submissionreference.QSubmissionReferencePresentation;
import com.mryqr.core.submission.SubmissionApi;
import com.mryqr.core.submission.domain.answer.singlelinetext.SingleLineTextAnswer;
import com.mryqr.core.tenant.domain.Tenant;
import com.mryqr.utils.CreateMemberResponse;
import com.mryqr.utils.PreparedQrResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static com.mryqr.core.common.domain.permission.Permission.AS_GROUP_MEMBER;
import static com.mryqr.core.common.domain.permission.Permission.PUBLIC;
import static com.mryqr.core.common.domain.report.SubmissionReportTimeBasedType.CREATED_AT;
import static com.mryqr.core.common.domain.report.SubmissionSegmentType.CONTROL_VALUE_SUM;
import static com.mryqr.core.common.domain.report.TimeSegmentInterval.PER_MONTH;
import static com.mryqr.core.common.exception.ErrorCode.ACCESS_DENIED;
import static com.mryqr.core.common.exception.ErrorCode.AUTHENTICATION_FAILED;
import static com.mryqr.core.common.exception.ErrorCode.CONTROL_NOT_COMPLETE;
import static com.mryqr.core.common.exception.ErrorCode.CONTROL_TYPE_NOT_ALLOWED;
import static com.mryqr.core.common.utils.UuidGenerator.newShortUuid;
import static com.mryqr.core.plan.domain.PlanType.BASIC;
import static com.mryqr.core.plan.domain.PlanType.FREE;
import static com.mryqr.core.plan.domain.PlanType.PROFESSIONAL;
import static com.mryqr.utils.RandomTestFixture.defaultNumberInputControlBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultSingleLineTextControl;
import static com.mryqr.utils.RandomTestFixture.defaultSubmissionReferenceControlBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultTimeSegmentControlBuilder;
import static com.mryqr.utils.RandomTestFixture.rAnswer;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PresentationControllerApiTest extends BaseApiTest {


    @Test
    public void public_user_should_be_able_to_get_non_chart_presentation_if_control_if_public() {
        PreparedQrResponse qrResponse = setupApi.registerWithQr();
        setupApi.updateTenantPackages(qrResponse.tenantId(), PROFESSIONAL);

        FSingleLineTextControl singleLineTextControl = defaultSingleLineTextControl();
        PSubmissionReferenceControl referenceControl = defaultSubmissionReferenceControlBuilder().pageId(qrResponse.homePageId()).build();
        AppApi.updateAppPermissionAndControls(qrResponse.jwt(), qrResponse.appId(), PUBLIC, singleLineTextControl, referenceControl);

        SingleLineTextAnswer singleLineTextAnswer = rAnswer(singleLineTextControl);
        SubmissionApi.newSubmission(qrResponse.jwt(), qrResponse.qrId(), qrResponse.homePageId(), singleLineTextAnswer);
        QSubmissionReferencePresentation presentation = (QSubmissionReferencePresentation) PresentationApi.fetchPresentation(null, qrResponse.qrId(), qrResponse.homePageId(), referenceControl.getId());

        TextDisplayValue value = (TextDisplayValue) presentation.getValues().get(singleLineTextControl.getId());
        assertEquals(singleLineTextAnswer.getContent(), value.getText());
    }

    @Test
    public void public_user_should_not_be_able_to_get_chart_presentation_if_control_is_public() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FNumberInputControl numberInputControl = defaultNumberInputControlBuilder().precision(3).build();
        PTimeSegmentControl control = defaultTimeSegmentControlBuilder()
                .segmentSettings(List.of(PTimeSegmentControl.TimeSegmentSetting.builder()
                        .id(newShortUuid())
                        .name("未命名统计项")
                        .segmentType(CONTROL_VALUE_SUM)
                        .basedType(CREATED_AT)
                        .pageId(response.homePageId())
                        .targetControlId(numberInputControl.getId())
                        .build()))
                .interval(PER_MONTH)
                .max(5)
                .build();
        AppApi.updateAppPermissionAndControls(response.jwt(), response.appId(), PUBLIC, numberInputControl, control);

        assertError(() -> PresentationApi.fetchPresentationRaw(null, response.qrId(), response.homePageId(), control.getId()), AUTHENTICATION_FAILED);
    }

    @Test
    public void should_return_401_if_not_logged_in_but_login_required() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FNumberInputControl numberInputControl = defaultNumberInputControlBuilder().precision(3).build();
        PTimeSegmentControl control = defaultTimeSegmentControlBuilder()
                .segmentSettings(List.of(PTimeSegmentControl.TimeSegmentSetting.builder()
                        .id(newShortUuid())
                        .name("未命名统计项")
                        .segmentType(CONTROL_VALUE_SUM)
                        .basedType(CREATED_AT)
                        .pageId(response.homePageId())
                        .targetControlId(numberInputControl.getId())
                        .build()))
                .interval(PER_MONTH)
                .max(5)
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), numberInputControl, control);
        assertError(() -> PresentationApi.fetchPresentationRaw(null, response.qrId(), response.homePageId(), control.getId()), AUTHENTICATION_FAILED);
    }

    @Test
    public void should_return_403_if_permission_not_enough() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FNumberInputControl numberInputControl = defaultNumberInputControlBuilder().precision(3).build();
        PTimeSegmentControl control = defaultTimeSegmentControlBuilder()
                .segmentSettings(List.of(PTimeSegmentControl.TimeSegmentSetting.builder()
                        .id(newShortUuid())
                        .name("未命名统计项")
                        .segmentType(CONTROL_VALUE_SUM)
                        .basedType(CREATED_AT)
                        .pageId(response.homePageId())
                        .targetControlId(numberInputControl.getId())
                        .build()))
                .interval(PER_MONTH)
                .max(5)
                .build();

        AppApi.updateAppPermissionAndControls(response.jwt(), response.appId(), AS_GROUP_MEMBER, numberInputControl, control);
        CreateMemberResponse noPermissionMember = MemberApi.createMemberAndLogin(response.jwt());
        assertError(() -> PresentationApi.fetchPresentationRaw(noPermissionMember.jwt(), response.qrId(), response.homePageId(), control.getId()), ACCESS_DENIED);
    }


    @Test
    public void should_return_error_if_package_too_low_for_statistics_controls() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FNumberInputControl numberInputControl = defaultNumberInputControlBuilder().precision(3).build();
        PTimeSegmentControl control = defaultTimeSegmentControlBuilder()
                .segmentSettings(List.of(PTimeSegmentControl.TimeSegmentSetting.builder()
                        .id(newShortUuid())
                        .name("未命名统计项")
                        .segmentType(CONTROL_VALUE_SUM)
                        .basedType(CREATED_AT)
                        .pageId(response.homePageId())
                        .targetControlId(numberInputControl.getId())
                        .build()))
                .interval(PER_MONTH)
                .max(5)
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), numberInputControl, control);
        setupApi.updateTenantPackages(response.tenantId(), FREE);
        assertError(() -> PresentationApi.fetchPresentationRaw(response.jwt(), response.qrId(), response.homePageId(), control.getId()), CONTROL_TYPE_NOT_ALLOWED);

        setupApi.updateTenantPackages(response.tenantId(), BASIC);
        assertError(() -> PresentationApi.fetchPresentationRaw(response.jwt(), response.qrId(), response.homePageId(), control.getId()), CONTROL_TYPE_NOT_ALLOWED);

        Tenant tenant = tenantRepository.byId(response.tenantId());
        setupApi.updateTenantPackages(tenant, PROFESSIONAL, Instant.now().minus(10, DAYS));
        assertError(() -> PresentationApi.fetchPresentationRaw(response.jwt(), response.qrId(), response.homePageId(), control.getId()), CONTROL_TYPE_NOT_ALLOWED);
    }


    @Test
    public void should_return_error_if_control_not_complete() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FNumberInputControl numberInputControl = defaultNumberInputControlBuilder().precision(3).build();
        PTimeSegmentControl control = defaultTimeSegmentControlBuilder()
                .segmentSettings(List.of(PTimeSegmentControl.TimeSegmentSetting.builder()
                        .id(newShortUuid())
                        .name("未命名统计项")
                        .segmentType(CONTROL_VALUE_SUM)
                        .basedType(CREATED_AT)
                        .pageId(response.homePageId())
                        .targetControlId(null)
                        .build()))
                .interval(PER_MONTH)
                .max(5)
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), numberInputControl, control);
        assertError(() -> PresentationApi.fetchPresentationRaw(response.jwt(), response.qrId(), response.homePageId(), control.getId()), CONTROL_NOT_COMPLETE);
    }
}
