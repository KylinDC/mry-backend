package com.mryqr.core.app.control;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.attribute.Attribute;
import com.mryqr.core.app.domain.page.control.Control;
import com.mryqr.core.app.domain.page.control.FPersonNameControl;
import com.mryqr.core.common.domain.indexedfield.IndexedField;
import com.mryqr.core.common.domain.indexedfield.IndexedValue;
import com.mryqr.core.qr.domain.QR;
import com.mryqr.core.qr.domain.attribute.IdentifierAttributeValue;
import com.mryqr.core.submission.SubmissionApi;
import com.mryqr.core.submission.command.NewSubmissionCommand;
import com.mryqr.core.submission.domain.Submission;
import com.mryqr.core.submission.domain.answer.personname.PersonNameAnswer;
import com.mryqr.utils.PreparedAppResponse;
import com.mryqr.utils.PreparedQrResponse;
import org.junit.jupiter.api.Test;

import static com.mryqr.core.app.domain.attribute.Attribute.newAttributeId;
import static com.mryqr.core.app.domain.attribute.AttributeStatisticRange.NO_LIMIT;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_FIRST;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_LAST;
import static com.mryqr.core.common.exception.ErrorCode.MANDATORY_ANSWER_REQUIRED;
import static com.mryqr.core.submission.SubmissionUtils.newSubmissionCommand;
import static com.mryqr.utils.RandomTestFixture.defaultFillableSettingBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultPersonNameControl;
import static com.mryqr.utils.RandomTestFixture.defaultPersonNameControlBuilder;
import static com.mryqr.utils.RandomTestFixture.rAnswer;
import static com.mryqr.utils.RandomTestFixture.rAnswerBuilder;
import static com.mryqr.utils.RandomTestFixture.rAttributeName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonNameControlApiTest extends BaseApiTest {

    @Test
    public void should_create_control_normally() {
        PreparedAppResponse response = setupApi.registerWithApp();

        FPersonNameControl control = defaultPersonNameControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertEquals(control, updatedControl);
    }

    @Test
    public void should_answer_normally() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FPersonNameControl control = defaultPersonNameControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        PersonNameAnswer answer = rAnswer(control);
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForControlOptional(response.homePageId(), control.getId()).get();
        Submission submission = submissionRepository.byId(submissionId);
        PersonNameAnswer updatedAnswer = (PersonNameAnswer) submission.allAnswers().get(control.getId());
        assertEquals(answer, updatedAnswer);
        IndexedValue indexedValue = submission.getIndexedValues().valueOf(indexedField);
        assertEquals(control.getId(), indexedValue.getRid());
        assertTrue(indexedValue.getTv().contains(answer.getName()));
    }

    @Test
    public void should_fail_answer_if_not_filled_for_mandatory() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FPersonNameControl control = defaultPersonNameControlBuilder().fillableSetting(defaultFillableSettingBuilder().mandatory(true).build()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        PersonNameAnswer answer = rAnswerBuilder(control).name(null).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);
        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), MANDATORY_ANSWER_REQUIRED);
    }

    @Test
    public void should_calculate_first_submission_answer_as_attribute_value() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FPersonNameControl control = defaultPersonNameControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        Attribute attribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_FIRST).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        AppApi.updateAppAttributes(response.jwt(), response.appId(), attribute);

        PersonNameAnswer answer = rAnswer(control);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForAttributeOptional(attribute.getId()).get();
        QR qr = qrRepository.byId(response.qrId());
        IdentifierAttributeValue attributeValue = (IdentifierAttributeValue) qr.getAttributeValues().get(attribute.getId());
        assertEquals(answer.getName(), attributeValue.getContent());
        assertTrue(qr.getIndexedValues().valueOf(indexedField).getTv().contains(answer.getName()));
    }

    @Test
    public void should_calculate_last_submission_answer_as_attribute_value() {
        PreparedQrResponse response = setupApi.registerWithQr();
        FPersonNameControl control = defaultPersonNameControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        Attribute attribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_LAST).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        AppApi.updateAppAttributes(response.jwt(), response.appId(), attribute);

        PersonNameAnswer answer = rAnswer(control);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForAttributeOptional(attribute.getId()).get();
        QR qr = qrRepository.byId(response.qrId());
        IdentifierAttributeValue attributeValue = (IdentifierAttributeValue) qr.getAttributeValues().get(attribute.getId());
        assertEquals(answer.getName(), attributeValue.getContent());
        assertTrue(qr.getIndexedValues().valueOf(indexedField).getTv().contains(answer.getName()));
    }

}
