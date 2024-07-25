package com.mryqr.core.app.control;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.AppSetting;
import com.mryqr.core.app.domain.page.control.Control;
import com.mryqr.core.app.domain.page.control.PImageViewControl;
import com.mryqr.core.common.domain.UploadedFile;
import com.mryqr.utils.PreparedAppResponse;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.mryqr.core.common.exception.ErrorCode.IMAGE_ID_DUPLICATED;
import static com.mryqr.core.plan.domain.PlanType.PROFESSIONAL;
import static com.mryqr.utils.RandomTestFixture.defaultImageViewControl;
import static com.mryqr.utils.RandomTestFixture.defaultImageViewControlBuilder;
import static com.mryqr.utils.RandomTestFixture.rImageFile;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageViewControlApiTest extends BaseApiTest {

    @Test
    public void should_create_control_normally() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        PImageViewControl control = defaultImageViewControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertEquals(control, updatedControl);
    }

    @Test
    public void should_fail_create_control_if_image_id_duplicated() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        UploadedFile uploadedFile = rImageFile();
        PImageViewControl control = defaultImageViewControlBuilder().images(newArrayList(uploadedFile, uploadedFile)).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), IMAGE_ID_DUPLICATED);
    }

}
