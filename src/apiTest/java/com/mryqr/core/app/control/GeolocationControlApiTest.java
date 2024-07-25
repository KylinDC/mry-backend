package com.mryqr.core.app.control;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.attribute.Attribute;
import com.mryqr.core.app.domain.page.control.Control;
import com.mryqr.core.app.domain.page.control.FGeolocationControl;
import com.mryqr.core.common.domain.Address;
import com.mryqr.core.common.domain.Geolocation;
import com.mryqr.core.common.domain.Geopoint;
import com.mryqr.core.common.domain.indexedfield.IndexedField;
import com.mryqr.core.common.domain.indexedfield.IndexedValue;
import com.mryqr.core.qr.QrApi;
import com.mryqr.core.qr.command.UpdateQrBaseSettingCommand;
import com.mryqr.core.qr.domain.QR;
import com.mryqr.core.qr.domain.attribute.GeolocationAttributeValue;
import com.mryqr.core.submission.SubmissionApi;
import com.mryqr.core.submission.command.NewSubmissionCommand;
import com.mryqr.core.submission.domain.Submission;
import com.mryqr.core.submission.domain.answer.geolocation.GeolocationAnswer;
import com.mryqr.utils.PreparedAppResponse;
import com.mryqr.utils.PreparedQrResponse;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.mryqr.core.app.domain.attribute.Attribute.newAttributeId;
import static com.mryqr.core.app.domain.attribute.AttributeStatisticRange.NO_LIMIT;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_FIRST;
import static com.mryqr.core.app.domain.attribute.AttributeType.CONTROL_LAST;
import static com.mryqr.core.common.domain.Address.joinAddress;
import static com.mryqr.core.common.exception.ErrorCode.MANDATORY_ANSWER_REQUIRED;
import static com.mryqr.core.common.exception.ErrorCode.OUT_OF_OFF_SET_RADIUS;
import static com.mryqr.core.plan.domain.PlanType.FLAGSHIP;
import static com.mryqr.core.submission.SubmissionUtils.newSubmissionCommand;
import static com.mryqr.utils.RandomTestFixture.defaultFillableSettingBuilder;
import static com.mryqr.utils.RandomTestFixture.defaultGeolocationControl;
import static com.mryqr.utils.RandomTestFixture.defaultGeolocationControlBuilder;
import static com.mryqr.utils.RandomTestFixture.rAddress;
import static com.mryqr.utils.RandomTestFixture.rAnswer;
import static com.mryqr.utils.RandomTestFixture.rAnswerBuilder;
import static com.mryqr.utils.RandomTestFixture.rAttributeName;
import static com.mryqr.utils.RandomTestFixture.rGeolocation;
import static com.mryqr.utils.RandomTestFixture.rQrName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeolocationControlApiTest extends BaseApiTest {

    @Test
    public void should_create_control_normally() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        FGeolocationControl control = defaultGeolocationControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertEquals(control, updatedControl);
    }

    @Test
    public void should_answer_normally() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        FGeolocationControl control = defaultGeolocationControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        GeolocationAnswer answer = rAnswer(control);
        Address address = answer.getGeolocation().getAddress();
        String submissionId = SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForControlOptional(response.homePageId(), control.getId()).get();
        Submission submission = submissionRepository.byId(submissionId);
        GeolocationAnswer updatedAnswer = (GeolocationAnswer) submission.allAnswers().get(control.getId());
        assertEquals(answer, updatedAnswer);
        IndexedValue indexedValue = submission.getIndexedValues().valueOf(indexedField);
        assertEquals(control.getId(), indexedValue.getRid());
        assertTrue(indexedValue.getTv().contains(address.getProvince()));
        assertTrue(indexedValue.getTv().contains(joinAddress(address.getProvince(), address.getCity())));
        assertTrue(indexedValue.getTv().contains(joinAddress(address.getProvince(), address.getCity(), address.getDistrict())));
    }

    @Test
    public void should_fail_answer_if_not_filled_for_mandatory() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        FGeolocationControl control = defaultGeolocationControlBuilder().fillableSetting(defaultFillableSettingBuilder().mandatory(true).build()).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        Geolocation geolocation = Geolocation.builder().point(Geopoint.builder().build()).address(rAddress()).build();
        GeolocationAnswer answer = rAnswerBuilder(control).geolocation(geolocation).build();

        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);
        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), MANDATORY_ANSWER_REQUIRED);
    }

    @Test
    public void should_fail_answer_if_geolocation_is_out_of_range() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);
        AppApi.enableAppPosition(response.jwt(), response.appId());
        FGeolocationControl control = defaultGeolocationControlBuilder().offsetRestrictionEnabled(true).offsetRestrictionRadius(500).build();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        Geolocation qrPosition = rGeolocation();
        UpdateQrBaseSettingCommand updateQrBaseSettingCommand = UpdateQrBaseSettingCommand.builder()
                .name(rQrName())
                .geolocation(qrPosition)
                .build();
        QrApi.updateQrBaseSetting(response.jwt(), response.qrId(), updateQrBaseSettingCommand);

        Geolocation offsetGeolocation = Geolocation.builder().point(Geopoint.builder().latitude(qrPosition.getPoint().getLatitude() - 1).longitude(qrPosition.getPoint().getLongitude() - 1).build()).address(rAddress()).build();
        GeolocationAnswer answer = rAnswerBuilder(control).geolocation(offsetGeolocation).build();
        NewSubmissionCommand command = newSubmissionCommand(response.qrId(), response.homePageId(), answer);

        assertError(() -> SubmissionApi.newSubmissionRaw(response.jwt(), command), OUT_OF_OFF_SET_RADIUS);

        //合法answer
        GeolocationAnswer validAnswer = rAnswerBuilder(control).geolocation(qrPosition).build();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), validAnswer);
    }

    @Test
    public void should_calculate_first_submission_answer_as_attribute_value() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        FGeolocationControl control = defaultGeolocationControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        Attribute attribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_FIRST).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        AppApi.updateAppAttributes(response.jwt(), response.appId(), attribute);

        GeolocationAnswer answer = rAnswer(control);
        Geolocation geolocation = answer.getGeolocation();
        Address address = geolocation.getAddress();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForAttributeOptional(attribute.getId()).get();
        QR qr = qrRepository.byId(response.qrId());
        GeolocationAttributeValue attributeValue = (GeolocationAttributeValue) qr.getAttributeValues().get(attribute.getId());
        assertEquals(geolocation, attributeValue.getGeolocation());
        Set<String> textValues = qr.getIndexedValues().valueOf(indexedField).getTv();
        assertTrue(textValues.contains(address.getProvince()));
        assertTrue(textValues.contains(joinAddress(address.getProvince(), address.getCity())));
        assertTrue(textValues.contains(joinAddress(address.getProvince(), address.getCity(), address.getDistrict())));
    }

    @Test
    public void should_calculate_last_submission_answer_as_attribute_value() {
        PreparedQrResponse response = setupApi.registerWithQr();
        setupApi.updateTenantPackages(response.tenantId(), FLAGSHIP);

        FGeolocationControl control = defaultGeolocationControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);
        Attribute attribute = Attribute.builder().name(rAttributeName()).id(newAttributeId()).type(CONTROL_LAST).pageId(response.homePageId()).controlId(control.getId()).range(NO_LIMIT).build();
        AppApi.updateAppAttributes(response.jwt(), response.appId(), attribute);

        GeolocationAnswer answer = rAnswer(control);
        Geolocation geolocation = answer.getGeolocation();
        Address address = geolocation.getAddress();
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), rAnswer(control));
        SubmissionApi.newSubmission(response.jwt(), response.qrId(), response.homePageId(), answer);

        App app = appRepository.byId(response.appId());
        IndexedField indexedField = app.indexedFieldForAttributeOptional(attribute.getId()).get();
        QR qr = qrRepository.byId(response.qrId());
        GeolocationAttributeValue attributeValue = (GeolocationAttributeValue) qr.getAttributeValues().get(attribute.getId());
        assertEquals(geolocation, attributeValue.getGeolocation());
        Set<String> textValues = qr.getIndexedValues().valueOf(indexedField).getTv();
        assertTrue(textValues.contains(address.getProvince()));
        assertTrue(textValues.contains(joinAddress(address.getProvince(), address.getCity())));
        assertTrue(textValues.contains(joinAddress(address.getProvince(), address.getCity(), address.getDistrict())));
    }

}
