package com.mryqr.core.report.query.number;

import com.mryqr.common.utils.Query;
import com.mryqr.common.validation.id.app.AppId;
import com.mryqr.common.validation.id.group.GroupId;
import com.mryqr.core.app.domain.report.number.NumberReport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import static lombok.AccessLevel.PRIVATE;

@Value
@Builder
@AllArgsConstructor(access = PRIVATE)
public class NumberReportQuery implements Query {

    @AppId
    @NotBlank
    private final String appId;

    @GroupId
    private final String groupId;

    @Valid
    @NotNull
    private final NumberReport report;
}
