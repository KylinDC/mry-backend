package com.mryqr.core.app.control;

import com.mryqr.BaseApiTest;
import com.mryqr.core.app.AppApi;
import com.mryqr.core.app.domain.App;
import com.mryqr.core.app.domain.AppSetting;
import com.mryqr.core.app.domain.page.Page;
import com.mryqr.core.app.domain.page.control.Control;
import com.mryqr.core.app.domain.page.control.PImageCardLinkControl;
import com.mryqr.core.app.domain.ui.pagelink.PageLink;
import com.mryqr.utils.PreparedAppResponse;
import org.junit.jupiter.api.Test;

import static com.google.common.collect.Lists.newArrayList;
import static com.mryqr.core.app.domain.ui.pagelink.PageLinkType.EXTERNAL_URL;
import static com.mryqr.core.app.domain.ui.pagelink.PageLinkType.PAGE;
import static com.mryqr.core.common.exception.ErrorCode.PAGE_LINK_ID_DUPLICATED;
import static com.mryqr.core.common.exception.ErrorCode.VALIDATION_LINK_PAGE_NOT_EXIST;
import static com.mryqr.core.common.utils.UuidGenerator.newShortUuid;
import static com.mryqr.core.plan.domain.PlanType.PROFESSIONAL;
import static com.mryqr.utils.RandomTestFixture.defaultImageCardLinkControl;
import static com.mryqr.utils.RandomTestFixture.defaultImageCardLinkControlBuilder;
import static com.mryqr.utils.RandomTestFixture.rPageLinkName;
import static com.mryqr.utils.RandomTestFixture.rUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImageCardLinkControlApiTest extends BaseApiTest {

    @Test
    public void should_create_control_normally() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        PImageCardLinkControl control = defaultImageCardLinkControl();
        AppApi.updateAppControls(response.jwt(), response.appId(), control);

        App app = appRepository.byId(response.appId());
        Control updatedControl = app.controlByIdOptional(control.getId()).get();
        assertEquals(control, updatedControl);
    }

    @Test
    public void should_fail_create_if_reference_non_exists_page() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        PageLink pageLink = PageLink.builder().id(newShortUuid()).type(PAGE).pageId(Page.newPageId()).build();
        PImageCardLinkControl control = defaultImageCardLinkControlBuilder().links(newArrayList(pageLink)).build();
        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), VALIDATION_LINK_PAGE_NOT_EXIST);
    }


    @Test
    public void should_fail_update_app_if_menu_id_duplicated() {
        PreparedAppResponse response = setupApi.registerWithApp();
        setupApi.updateTenantPackages(response.tenantId(), PROFESSIONAL);

        String linkId = newShortUuid();
        PageLink link1 = PageLink.builder().id(linkId).name(rPageLinkName()).type(EXTERNAL_URL).url(rUrl()).build();
        PageLink link2 = PageLink.builder().id(linkId).name(rPageLinkName()).type(EXTERNAL_URL).url(rUrl()).build();
        PImageCardLinkControl control = defaultImageCardLinkControlBuilder().links(newArrayList(link1, link2)).build();

        App app = appRepository.byId(response.appId());
        AppSetting setting = app.getSetting();
        setting.homePage().getControls().add(control);

        assertError(() -> AppApi.updateAppSettingRaw(response.jwt(), response.appId(), app.getVersion(), setting), PAGE_LINK_ID_DUPLICATED);
    }

}
