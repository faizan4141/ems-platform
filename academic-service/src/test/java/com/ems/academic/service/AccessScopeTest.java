package com.ems.academic.service;

import com.ems.academic.domain.TeachingAssignment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccessScopeTest {

    private final UUID tenant = UUID.randomUUID();
    private final UUID staff = UUID.randomUUID();
    private final UUID sectionA = UUID.randomUUID();
    private final UUID sectionB = UUID.randomUUID();
    private final UUID sectionC = UUID.randomUUID();
    private final UUID english = UUID.randomUUID();
    private final UUID maths = UUID.randomUUID();

    private TeachingAssignment classTeacherOf(UUID section) {
        return new TeachingAssignment(tenant, section, staff, null, true, Instant.now());
    }

    private TeachingAssignment teaches(UUID section, UUID subject) {
        return new TeachingAssignment(tenant, section, staff, subject, false, Instant.now());
    }

    @Test
    void aTeacherSeesOnlyTheSectionsTheyAreAssignedTo() {
        AccessService.Scope scope = AccessService.Scope.of(List.of(classTeacherOf(sectionA), teaches(sectionB, english)));

        assertThat(scope.canSeeSection(sectionA)).isTrue();
        assertThat(scope.canSeeSection(sectionB)).isTrue();
        assertThat(scope.canSeeSection(sectionC)).isFalse();
        assertThat(scope.canSeeSection(null)).isFalse();
    }

    @Test
    void aClassTeacherMayEnterAnySubjectForTheirSectionOnly() {
        AccessService.Scope scope = AccessService.Scope.of(List.of(classTeacherOf(sectionA)));

        assertThat(scope.canEditMarks(sectionA, english)).isTrue();
        assertThat(scope.canEditMarks(sectionA, maths)).isTrue();
        assertThat(scope.canEditMarks(sectionB, english)).isFalse();
    }

    @Test
    void aSubjectTeacherMayEnterOnlyThatSubjectInThatSection() {
        AccessService.Scope scope = AccessService.Scope.of(List.of(teaches(sectionA, english)));

        assertThat(scope.canEditMarks(sectionA, english)).isTrue();
        assertThat(scope.canEditMarks(sectionA, maths)).isFalse();
        assertThat(scope.canEditMarks(sectionB, english)).isFalse();
        assertThat(scope.canEditMarks(null, english)).isFalse();
    }

    @Test
    void noAssignmentsMeansNoAccess() {
        AccessService.Scope scope = AccessService.Scope.none();

        assertThat(scope.canSeeSection(sectionA)).isFalse();
        assertThat(scope.canEditMarks(sectionA, english)).isFalse();
    }

    @Test
    void adminsMayDoEverything() {
        AccessService.Scope scope = AccessService.Scope.everything();

        assertThat(scope.canSeeSection(sectionC)).isTrue();
        assertThat(scope.canEditMarks(sectionC, maths)).isTrue();
    }
}