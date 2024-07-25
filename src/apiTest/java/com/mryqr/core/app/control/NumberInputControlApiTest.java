package com.mryqr.core.app.control;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.AppSetting;
import com.mryqr.core.app.domain.attribute.Attribute;
import com.mryqr.core.app.domain.page.control.AutoCalculateAliasContext;
import com.mryqr.core.app.domain.page.control.Control;
import com.mryqr.core.app.domain.page.control.FNumberInputControl;
import com.mryqr.core.app.domain.page.control.FSingleLineTextControl;
import com.mryqr.core.common.domain.indexedfield.IndexedField;
import com.mryqr.core.common.domain.indexedfield.IndexedValue;
import com.mryqr.core.qr.domain.QR;
import com.mryqr.core.qr.domain.attribute.DoubleAttributeValue;
import com.mryqr.core.submission.SubmissionApi;
import com.mryqr.core.submission.command.NewSubmissionCommand;
import com.mryqr.core.submission.domain.Submission;
import com.mryqr.core.submission.domain.answer.numberinput.NumberInputAnswer;
import com.mryqr.utils.PreparedAppResponse;
import com.mryqr.utils.PreparedQrResponse;
import com.mryqr.utils.RandomTestFixture;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.mryqr.core.app.domain.attribute.Attribute.newAttributeId;
import static com.mryqr.core.app.domain.attribute.AttributeStatisticRange.NO_LIMIT;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_AVERAGE;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_FIRST;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_LAST;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_MAX;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_MIN;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_SUM;
import static com.mryqr.core.app.domain.page.control.FNumberInputControl.MAX_NUMBER;
import static com.mryqr.core.app.domain.page.control.FNumberInputControl.MIN_NUMBER;
import static com.mryqr.core.app.domain.ui.MinMaxSetting.minMaxOf;
import static com.mryqr.core.common.exception.ErrorCode.CONTROL_NOT_NUMERICAL_VALUED;
import static com.mryqr.core.common.exception.ErrorCode.CONTROL_SHOULD_NOT_SELF;
import static com.mryqr.core.common.exception.ErrorCode.INCORRECT_INTEGER_PRECISION;
import static com.mryqr.core.common.exception.ErrorCode.INCORRECT_NUMBER_INPUT_PRECISION;
import static com.mryqr.core.common.exception.ErrorCode.MANDATORY_ANSWER_REQUIRED;
import static com.mryqr.core.common.exception.ErrorCode.MAX_INPUT_NUMBER_REACHED;
import static com.mryqr.core.common.exception.ErrorCode.MAX_OVERFLOW;
import static com.mryqr.core.common.exception.ErrorCode.MIN_INPUT_NUMBER_REACHED;
import static com.mryqr.core.common.exception.ErrorCode.MIN_OVERFLOW;
import static com.mryqr.core.common.exception.ErrorCode.VALIDATION_CONTROL_NOT_EXIST;
import static com.mryqr.core.common.utils.UuidGenerator.newShortUuid;
import static com.mryqr.core.submission.SubmissionUtils.newSubmissionCommand;
import static com.mryqr.utils.RandomTestFixture.defaultFillableSettingBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultNumberInputControlBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultSingleLineTextControl;
import static com.mryqr.utils.RandomTestFixture.rAnswerBuilder;
import static com.mryqr.utils.RandomTestFixture.rAttributeName;
import static com.mryqr.utils.RandomTestFixture.rEmail;
import static com.mryqr.utils.RandomTestFixture.rPassword;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumberInputControlApiTest extends BaseApiTest {

    @Test
    public void should_create_control_normally() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FNumberInputControl control = defaultNumberInputControlBuilder().precision(3).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertEquals(control, updatedControl);
    }

    @Test
    public void should_create_control_with_auto_calculate_enabled() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(3).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .fillableSetting(defaultFillableSettingBuilder().autoFill(true).build())
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);

        App app = appRepository.byId(response.appId());
        FNumberInputControl updatedControl = (FNumberInputControl) app.controlByIdOptional(calculatedControl.getId()).get();
        assertEquals(calculatedControl.getAutoCalculateSetting(), updatedControl.getAutoCalculateSetting());
        assertFalse(updatedControl.getFillableSetting().isAutoFill());
        assertTrue(updatedControl.isShouldAutoCalculate());
    }


    @Test
    public void should_fail_create_control_if_control_not_exist_for_auto_calculate() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(Control.newControlId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(calculatedControl);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), VALIDATION_CONTROL_NOT_EXIST);
    }


    @Test
    public void should_fail_create_control_if_control_not_numerical_valued_for_auto_calculate() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FSingleLineTextControl lineTextControl = defaultSingleLineTextControl();

        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(lineTextControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().addAll(newArrayList(calculatedControl, lineTextControl));

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), CONTROL_NOT_NUMERICAL_VALUED);
    }


    @Test
    public void should_fail_create_control_if_control_reference_self_for_auto_calculate() {
        PreparedAppResponse response = setupApi.registerWithApp();

        String controlId = Control.newControlId();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .id(controlId)
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(controlId)
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().addAll(newArrayList(calculatedControl));

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), CONTROL_SHOULD_NOT_SELF);
    }


    @Test
    public void should_fail_create_control_if_number_greater_than_max() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FNumberInputControl control = defaultNumberInputControlBuilder().minMaxSetting(minMaxOf(1, MAX_NUMBER + 1)).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), MAX_OVERFLOW);
    }

    @Test
    public void should_fail_create_control_if_number_less_than_min() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FNumberInputControl control = defaultNumberInputControlBuilder().minMaxSetting(minMaxOf(MIN_NUMBER - 1, 100)).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), MIN_OVERFLOW);
    }


    @Test
    public void should_answer_normally() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FNumberInputControl control = defaultNumberInputControlBuilder().precision(3).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        NumberInputAnswer answer = RandomTestFixture.rAnswer(control);
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForControlOptional(response.homePageId(), control.getId()).get();
        Submission submission = submissionRepository.byId(submissionId);
        NumberInputAnswer updatedAnswer = (NumberInputAnswer) submission.allAnswers().get(control.getId());
        assertEquals(answer, updatedAnswer);
        IndexedValue indexedValue = submission.getIndexedValues().valueOf(indexedField);
        assertEquals(control.getId(), indexedValue.getRid());
        assertEquals(answer.getNumber(), indexedValue.getSv());
    }

    @Test
    public void should_answer_with_auto_calculated_value() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(3).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        NumberInputAnswer answer = rAnswerBuilder(dependantControl).number(11.0).build();
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);
        Submission submission = submissionRepository.byId(submissionId);
        NumberInputAnswer updatedAnswer = (NumberInputAnswer) submission.getAnswers().get(calculatedControl.getId());
        assertEquals(22, updatedAnswer.getNumber());
    }


    @Test
    public void should_answer_with_auto_calculated_value_with_0_precision() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(2).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .precision(0)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        NumberInputAnswer answer = rAnswerBuilder(dependantControl).number(11.2).build();
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);
        Submission submission = submissionRepository.byId(submissionId);
        NumberInputAnswer updatedAnswer = (NumberInputAnswer) submission.getAnswers().get(calculatedControl.getId());
        assertEquals(22, updatedAnswer.getNumber());
    }

    @Test
    public void should_answer_with_auto_calculated_value_with_2_precision() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(2).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .precision(2)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number / 3")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        NumberInputAnswer answer = rAnswerBuilder(dependantControl).number(11.0).build();
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);
        Submission submission = submissionRepository.byId(submissionId);
        NumberInputAnswer updatedAnswer = (NumberInputAnswer) submission.getAnswers().get(calculatedControl.getId());
        assertEquals(3.67, updatedAnswer.getNumber());
    }

    @Test
    public void should_answer_with_auto_calculated_value_and_ignore_provided_answer() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(3).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        NumberInputAnswer dependantAnswer = rAnswerBuilder(dependantControl).number(11.0).build();
        NumberInputAnswer calculateAnswer = rAnswerBuilder(calculatedControl).number(20.0).build();
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), dependantAnswer, calculateAnswer);
        Submission submission = submissionRepository.byId(submissionId);
        NumberInputAnswer updatedAnswer = (NumberInputAnswer) submission.getAnswers().get(calculatedControl.getId());
        assertEquals(22, updatedAnswer.getNumber());
    }

    @Test
    public void should_not_answer_with_auto_calculated_value_with_invalid_expression() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(3).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number whatever 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        assertNull(submissionRepository
                .byId(SubmissionApi.newSubmission(response.jwt(),
                        response.qrId(),
                        response.homePageId(),
                        rAnswerBuilder(dependantControl).number(9.0).build()))
                .getAnswers().get(calculatedControl.getId()));
    }


    @Test
    public void should_not_answer_with_auto_calculated_value_if_no_value_provided() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(3).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        assertNull(submissionRepository
                .byId(SubmissionApi.newSubmission(response.jwt(),
                        response.qrId(),
                        response.homePageId()))
                .getAnswers().get(calculatedControl.getId()));
    }


    @Test
    public void should_not_answer_with_auto_calculated_value_with_dependant_answer_not_filled() {
        PreparedQrResponse response = setupApi.registerWithQr();

        FNumberInputControl dependantControl = defaultNumberInputControlBuilder().precision(3).build();
        FNumberInputControl calculatedControl = defaultNumberInputControlBuilder()
                .autoCalculateEnabled(true)
                .autoCalculateSetting(FNumberInputControl.AutoCalculateSetting.builder()
                        .aliasContext(AutoCalculateAliasContext.builder()
                                .controlAliases(newArrayList(AutoCalculateAliasContext.ControlAlias.builder()
                                        .id(newShortUuid())
                                        .alias("number")
                                        .controlId(dependantControl.getId())
                                        .build()))
                                .build())
                        .expression("#number * 2")
                        .build())
                .build();

        AppApi.updateAppControls(response.jwt(), response.appId(), dependantControl, calculatedControl);
        assertNull(submissionRepository
                .byId(SubmissionApi.newSubmission(response.jwt(),
                        response.qrId(),
                        response.homePageId(),
                        rAnswerBuilder(dependantControl).number(null).build()))
                .getAnswers().get(calculatedControl.getId()));
    }


    @Test
    public void should_fail_answer_if_not_filled_for_mandatory() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FNumberInputControl control = defaultNumberInputControlBuilder().fillableSetting(defaultFillableSettingBuilder().mandatory(true).build()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        NumberInputAnswer answer = rAnswerBuilder(control).number(null).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);

        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), MANDATORY_ANSWER_REQUIRED);
    }

    @Test
    public void should_fail_answer_if_number_exceeds_max() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FNumberInputControl control = defaultNumberInputControlBuilder().minMaxSetting(minMaxOf(1, 10)).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        NumberInputAnswer answer = rAnswerBuilder(control).number(11d).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);

        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), MAX_INPUT_NUMBER_REACHED);
    }

    @Test
    public void should_fail_answer_if_number_less_than_min() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FNumberInputControl control = defaultNumberInputControlBuilder().minMaxSetting(minMaxOf(5, 10)).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        NumberInputAnswer answer = rAnswerBuilder(control).number(1d).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);

        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), MIN_INPUT_NUMBER_REACHED);
    }

    @Test
    public void should_fail_answer_if_number_has_decimal_for_integer() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FNumberInputControl control = defaultNumberInputControlBuilder().precision(0).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        NumberInputAnswer answer = rAnswerBuilder(control).number(1.1d).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);

        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), INCORRECT_INTEGER_PRECISION);
    }

    @Test
    public void should_fail_answer_if_number_has_decimal_more_than_required() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FNumberInputControl control = defaultNumberInputControlBuilder().precision(2).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        NumberInputAnswer answer = rAnswerBuilder(control).number(1.111d).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);

        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), INCORRECT_NUMBER_INPUT_PRECISION);
    }

    @Test
    public void should_calculate_submission_value_as_attribute_value() {
        PreparedQrResponse response = setupApi.registerWithQr(rEmail(), rPassword());
        FNumberInputControl control = defaultNumberInputControlBuilder().precision(0).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        Attribute firstAttribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_FIRST).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        Attribute lastAttribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_LAST).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        Attribute maxAttribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_MAX).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        Attribute minAttribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_MIN).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        Attribute avgAttribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_AVERAGE).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        Attribute sumAttribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_SUM).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        AppApi.updateAppAttributes(response.jwt(), response.appId(), firstAttribute, lastAttribute, maxAttribute, minAttribute, avgAttribute, sumAttribute);

        SubmissionApi.newSubmission(response.jwt(),
                newSubmissionCommand(response.qrId(), response.homePageId(),
                        NumberInputAnswer.builder().controlId(control.getId()).controlType(control.getType()).number(2d).build()));

        SubmissionApi.newSubmission(response.jwt(),
                newSubmissionCommand(response.qrId(), response.homePageId(),
                        NumberInputAnswer.builder().controlId(control.getId()).controlType(control.getType()).number(1d).build()));

        SubmissionApi.newSubmission(response.jwt(),
                newSubmissionCommand(response.qrId(), response.homePageId(),
                        NumberInputAnswer.builder().controlId(control.getId()).controlType(control.getType()).number(3d).build()));

        App app = appRepository.byId(response.appId());
        IndexedField firstIndexedField = app.indexedFieldForAttributeOptional(firstAttribute.getId()).get();
        IndexedField lastIndexedField = app.indexedFieldForAttributeOptional(lastAttribute.getId()).get();
        IndexedField maxIndexedField = app.indexedFieldForAttributeOptional(maxAttribute.getId()).get();
        IndexedField minIndexedField = app.indexedFieldForAttributeOptional(minAttribute.getId()).get();
        IndexedField avgIndexedField = app.indexedFieldForAttributeOptional(avgAttribute.getId()).get();
        IndexedField sumIndexedField = app.indexedFieldForAttributeOptional(sumAttribute.getId()).get();
        QR qr = qrRepository.byId(response.qrId());

        assertEquals(2, ((DoubleAttributeValue) qr.getAttributeValues().get(firstAttribute.getId())).getNumber());
        assertEquals(3, ((DoubleAttributeValue) qr.getAttributeValues().get(lastAttribute.getId())).getNumber());
        assertEquals(3, ((DoubleAttributeValue) qr.getAttributeValues().get(maxAttribute.getId())).getNumber());
        assertEquals(1, ((DoubleAttributeValue) qr.getAttributeValues().get(minAttribute.getId())).getNumber());
        assertEquals(2, ((DoubleAttributeValue) qr.getAttributeValues().get(avgAttribute.getId())).getNumber());
        assertEquals(6, ((DoubleAttributeValue) qr.getAttributeValues().get(sumAttribute.getId())).getNumber());

        assertEquals(2, qr.getIndexedValues().valueOf(firstIndexedField).getSv());
        assertEquals(3, qr.getIndexedValues().valueOf(lastIndexedField).getSv());
        assertEquals(3, qr.getIndexedValues().valueOf(maxIndexedField).getSv());
        assertEquals(1, qr.getIndexedValues().valueOf(minIndexedField).getSv());
        assertEquals(2, qr.getIndexedValues().valueOf(avgIndexedField).getSv());
        assertEquals(6, qr.getIndexedValues().valueOf(sumIndexedField).getSv());
    }

}
