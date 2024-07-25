package com.mryqr.core.app.control;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.AppSetting;
import com.mryqr.core.app.domain.page.Page;
import com.mryqr.core.app.domain.page.control.Control;
import com.mryqr.core.app.domain.page.control.FCheckboxControl;
import com.mryqr.core.app.domain.page.control.FNumberInputControl;
import com.mryqr.core.app.domain.page.control.FRadioControl;
import com.mryqr.core.app.domain.page.control.FSingleLineTextControl;
import com.mryqr.core.app.domain.page.control.PPieControl;
import com.mryqr.core.common.domain.report.ReportRange;
import com.mryqr.core.presentation.PresentationApi;
import com.mryqr.core.presentation.query.pie.QPiePresentation;
import com.mryqr.core.qr.QrApi;
import com.mryqr.core.qr.command.CreateQrResponse;
import com.mryqr.core.submission.SubmissionApi;
import com.mryqr.core.submission.domain.answer.radio.RadioAnswer;
import com.mryqr.utils.PreparedAppResponse;
import com.mryqr.utils.PreparedQrResponse;
import org.junit.jupiter.api.Test;

import static com.mryqr.core.common.domain.report.SubmissionSegmentType.CONTROL_VALUE_SUM;
import static com.mryqr.core.common.exception.ErrorCode.NOT_SUPPORTED_BASED_CONTROL_FOR_PIE;
import static com.mryqr.core.common.exception.ErrorCode.NOT_SUPPORTED_TARGET_CONTROL_FOR_PIE;
import static com.mryqr.core.common.exception.ErrorCode.VALIDATION_CONTROL_NOT_EXIST;
import static com.mryqr.core.common.exception.ErrorCode.VALIDATION_PAGE_NOT_EXIST;
import static com.mryqr.core.plan.domain.PlanType.PROFESSIONAL;
import static com.mryqr.utils.RandomTestFixture.defaultCheckboxControl;
import static com.mryqr.utils.RandomTestFixture.defaultNumberInputControlBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultPieControlBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultRadioControlBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultSingleLineTextControl;
import static com.mryqr.utils.RandomTestFixture.rAnswerBuilder;
import static com.mryqr.utils.RandomTestFixture.rTextOptions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PieControlApiTest extends BaseApiTest {

    @Test
    public void should_create_control_normally() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        PPieControl control = defaultPieControlBuilder().pageId(response.homePageId()).basedControlId(checkboxControl.getId()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl, control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertEquals(control, updatedControl);
        assertTrue(updatedControl.isComplete());
    }

    @Test
    public void should_not_complete_with_no_page() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl);
        PPieControl control = defaultPieControlBuilder().basedControlId(checkboxControl.getId()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertFalse(updatedControl.isComplete());
    }

    @Test
    public void should_not_complete_with_no_control() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl);
        PPieControl control = defaultPieControlBuilder().pageId(response.homePageId()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertFalse(updatedControl.isComplete());
    }

    @Test
    public void should_not_complete_with_no_value_control() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl);
        PPieControl control = defaultPieControlBuilder().segmentType(CONTROL_VALUE_SUM).pageId(response.homePageId()).basedControlId(checkboxControl.getId()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertFalse(updatedControl.isComplete());
    }


    @Test
    public void should_fail_create_control_if_referenced_page_not_exist() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl);
        PPieControl control = defaultPieControlBuilder().pageId(Page.newPageId()).basedControlId(checkboxControl.getId()).build();

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);
        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), VALIDATION_PAGE_NOT_EXIST);
    }

    @Test
    public void should_fail_create_control_if_referenced_control_not_exist() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl);
        PPieControl control = defaultPieControlBuilder().pageId(response.homePageId()).basedControlId(Control.newControlId()).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), VALIDATION_CONTROL_NOT_EXIST);
    }


    @Test
    public void should_fail_create_control_if_referenced_value_control_not_exist() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl);
        PPieControl control = defaultPieControlBuilder().segmentType(CONTROL_VALUE_SUM).pageId(response.homePageId()).basedControlId(checkboxControl.getId()).targetControlId(Control.newControlId()).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), VALIDATION_CONTROL_NOT_EXIST);
    }


    @Test
    public void should_fail_create_control_if_referenced_value_control_is_not_number() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FCheckboxControl checkboxControl = defaultCheckboxControl();
        FSingleLineTextControl singleLineTextControl = defaultSingleLineTextControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), checkboxControl, singleLineTextControl);
        PPieControl control = defaultPieControlBuilder().segmentType(CONTROL_VALUE_SUM).pageId(response.homePageId()).basedControlId(checkboxControl.getId()).targetControlId(singleLineTextControl.getId()).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), NOT_SUPPORTED_TARGET_CONTROL_FOR_PIE);
    }


    @Test
    public void should_fail_create_control_if_referenced_control_not_support() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FSingleLineTextControl singleLineTextControl = defaultSingleLineTextControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), singleLineTextControl);
        PPieControl control = defaultPieControlBuilder().pageId(response.homePageId()).basedControlId(singleLineTextControl.getId()).build();

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);
        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), NOT_SUPPORTED_BASED_CONTROL_FOR_PIE);
    }


    @Test
    public void should_fetch_presentation_values_for_submit_count() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FRadioControl radioControl = defaultRadioControlBuilder().options(rTextOptions(10)).build();
        PPieControl control = defaultPieControlBuilder()
                .pageId(response.homePageId())
                .basedControlId(radioControl.getId())
                .range(ReportRange.NO_LIMIT)
                .build();
        AppApi.updateAppControls(response.jwt(), response.appId(), radioControl, control);

        RadioAnswer radioAnswer1 = rAnswerBuilder(radioControl).optionId(radioControl.getOptions().get(0).getId()).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1);

        RadioAnswer radioAnswer2 = rAnswerBuilder(radioControl).optionId(radioControl.getOptions().get(1).getId()).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer2);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer2);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer2);

        RadioAnswer radioAnswer3 = rAnswerBuilder(radioControl).optionId(radioControl.getOptions().get(2).getId()).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer3);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer3);

        //another qr should not be counted
        CreateQrResponse qrResponse = QrApi.createQr(response.jwt(), response.defaultGroupId());
        SubmissionApi.newSubmission(response.jwt(), qrResponse.getQrId(), response.homePageId(), radioAnswer1);

        QPiePresentation presentation = (QPiePresentation) PresentationApi.fetchPresentation(response.jwt(), response.qrId(), response.homePageId(), control.getId());

        assertEquals(5, presentation.getSegments().stream().filter(count -> count.getOption().equals(radioAnswer1.getOptionId())).findFirst().get().getValue());
        assertEquals(3, presentation.getSegments().stream().filter(count -> count.getOption().equals(radioAnswer2.getOptionId())).findFirst().get().getValue());
        assertEquals(2, presentation.getSegments().stream().filter(count -> count.getOption().equals(radioAnswer3.getOptionId())).findFirst().get().getValue());
    }


    @Test
    public void should_fetch_presentation_values_for_control_sum() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        FRadioControl radioControl = defaultRadioControlBuilder().options(rTextOptions(10)).build();
        FNumberInputControl numberInputControl = defaultNumberInputControlBuilder().precision(3).build();
        PPieControl control = defaultPieControlBuilder()
                .segmentType(CONTROL_VALUE_SUM)
                .pageId(response.homePageId())
                .basedControlId(radioControl.getId())
                .targetControlId(numberInputControl.getId())
                .range(ReportRange.NO_LIMIT)
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), radioControl, control, numberInputControl);

        RadioAnswer radioAnswer1 = rAnswerBuilder(radioControl).optionId(radioControl.getOptions().get(0).getId()).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1, rAnswerBuilder(numberInputControl).number(1d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1, rAnswerBuilder(numberInputControl).number(2d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1, rAnswerBuilder(numberInputControl).number(3d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1, rAnswerBuilder(numberInputControl).number(4d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer1, rAnswerBuilder(numberInputControl).number(5d).build());

        RadioAnswer radioAnswer2 = rAnswerBuilder(radioControl).optionId(radioControl.getOptions().get(1).getId()).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer2, rAnswerBuilder(numberInputControl).number(2d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer2, rAnswerBuilder(numberInputControl).number(3d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswerBuilder(numberInputControl).number(4d).build());

        RadioAnswer radioAnswer3 = rAnswerBuilder(radioControl).optionId(radioControl.getOptions().get(2).getId()).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer3, rAnswerBuilder(numberInputControl).number(10d).build());
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), radioAnswer3);

        //another qr should not be counted
        CreateQrResponse qrResponse = QrApi.createQr(response.jwt(), response.defaultGroupId());
        SubmissionApi.newSubmission(response.jwt(), qrResponse.getQrId(), response.homePageId(), radioAnswer1, rAnswerBuilder(numberInputControl).number(1d).build());

        QPiePresentation presentation = (QPiePresentation) PresentationApi.fetchPresentation(response.jwt(), response.qrId(), response.homePageId(), control.getId());

        assertEquals(15, presentation.getSegments().stream().filter(count -> count.getOption().equals(radioAnswer1.getOptionId())).findFirst().get().getValue());
        assertEquals(5, presentation.getSegments().stream().filter(count -> count.getOption().equals(radioAnswer2.getOptionId())).findFirst().get().getValue());
        assertEquals(10, presentation.getSegments().stream().filter(count -> count.getOption().equals(radioAnswer3.getOptionId())).findFirst().get().getValue());
    }

}
