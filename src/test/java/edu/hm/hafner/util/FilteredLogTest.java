package edu.hm.hafner.util;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;

import static edu.hm.hafner.util.assertions.Assertions.*;

/**
 * Tests the class {@link FilteredLog}.
 *
 * @author Ullrich Hafner
 */
class FilteredLogTest extends SerializableTest<FilteredLog> {
    private static final String TITLE = "Title: ";

    @Test
    void shouldLogNothing() {
        var filteredLog = new FilteredLog(TITLE, 5);

        assertThat(filteredLog).hasNoErrorMessages().hasNoInfoMessages();
        assertThat(filteredLog.size()).isEqualTo(0);

        var empty = new FilteredLog();
        assertThat(empty.size()).isEqualTo(0);
    }

    @Test
    void shouldLogAllErrors() {
        var filteredLog = new FilteredLog(TITLE, 5);

        assertThat(filteredLog).doesNotHaveErrors();
        filteredLog.logError("1");
        assertThat(filteredLog).hasErrors();

        filteredLog.logError("2");
        filteredLog.logError("3");
        filteredLog.logError("4");
        filteredLog.logError("5");

        assertThatExactly5MessagesAreLogged(filteredLog);
        assertThat(filteredLog.size()).isEqualTo(5);
    }

    @Test
    void shouldSkipAdditionalErrors() {
        FilteredLog filteredLog = create5ErrorsLogWithTitle(StringUtils.EMPTY);

        assertThat(filteredLog).hasOnlyErrorMessages("1", "2", "3", "4", "5",
                "  ... skipped logging of 2 additional errors ...");
    }

    @Test
    void shouldSkipAdditionalErrorsWithTitle() {
        FilteredLog filteredLog = create5ErrorsLogWithTitle(TITLE);

        assertThat(filteredLog).hasOnlyErrorMessages(TITLE, "1", "2", "3", "4", "5",
                "  ... skipped logging of 2 additional errors ...");
    }

    private FilteredLog create5ErrorsLogWithTitle(final String title) {
        var filteredLog = new FilteredLog(title, 5);

        filteredLog.logError("1");
        filteredLog.logError("2");
        filteredLog.logError("3");
        filteredLog.logError("4");
        filteredLog.logError("5");
        filteredLog.logError("6");
        filteredLog.logError("7");

        assertThat(filteredLog.size()).isEqualTo(7);

        return filteredLog;
    }

    private void assertThatExactly5MessagesAreLogged(final FilteredLog filteredLog) {
        assertThat(filteredLog).hasOnlyErrorMessages(TITLE, "1", "2", "3", "4", "5");
    }

    @Test
    void shouldMergeLogger() {
        var parent = new FilteredLog("Parent Errors");

        parent.logInfo("parent Info 1");
        parent.logError("parent Error 1");

        var child = new FilteredLog("Child Errors");
        child.logInfo("child Info 1");
        child.logError("child Error 1");

        parent.merge(child);

        assertThat(parent).hasOnlyInfoMessages("parent Info 1", "child Info 1")
                .hasOnlyErrorMessages("Parent Errors", "parent Error 1", "Child Errors", "child Error 1");
        assertThat(parent.size()).isEqualTo(2);
    }

    @Test
    void shouldLogExceptions() {
        var filteredLog = new FilteredLog(TITLE, 1);

        filteredLog.logException(new IllegalArgumentException("Cause"), "Message");
        filteredLog.logException(new IllegalArgumentException(""), "Message");

        assertThat(filteredLog).hasErrorMessages(TITLE,
                "Message", "java.lang.IllegalArgumentException: Cause");
    }

    @Test
    void shouldLog20ErrorsByDefault() {
        FilteredLog filteredLog = createLogWith20Elements();

        assertThat(filteredLog.getErrorMessages()).hasSize(22)
                .contains(TITLE)
                .contains("error19")
                .doesNotContain("error20")
                .contains("  ... skipped logging of 5 additional errors ...");
        assertThat(filteredLog.getInfoMessages()).hasSize(25)
                .contains("info0")
                .contains("info24");
    }

    @Override
    protected void assertThatRestoredInstanceEqualsOriginalInstance(final FilteredLog original,
            final FilteredLog restored) {
        assertThat(original).usingRecursiveComparison(RecursiveComparisonConfiguration.builder().withIgnoredFields("lock").build()).isEqualTo(restored);
    }

    private FilteredLog createLogWith20Elements() {
        var filteredLog = new FilteredLog(TITLE);

        for (int i = 0; i < 25; i++) {
            filteredLog.logError("error%d", i);
            filteredLog.logInfo("info%d", i);
        }
        return filteredLog;
    }

    @Override
    protected FilteredLog createSerializable() {
        return createLogWith20Elements();
    }

    /** Actually tests {@link edu.hm.hafner.util.SerializableTest}. */
    @Test
    void shouldManuallyUseSerializationHelpers() {
        FilteredLog serializable = createSerializable();

        byte[] bytes = toByteArray(serializable);
        FilteredLog restored = restore(bytes);

        assertThatRestoredInstanceEqualsOriginalInstance(serializable, restored);
    }
}
