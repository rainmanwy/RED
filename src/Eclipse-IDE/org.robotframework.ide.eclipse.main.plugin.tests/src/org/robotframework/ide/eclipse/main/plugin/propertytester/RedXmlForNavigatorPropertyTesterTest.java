package org.robotframework.ide.eclipse.main.plugin.propertytester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Path;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.rf.ide.core.project.RobotProjectConfig;
import org.robotframework.red.junit.ProjectProvider;

public class RedXmlForNavigatorPropertyTesterTest {

    @ClassRule
    public static ProjectProvider projectProvider = new ProjectProvider(RedXmlForNavigatorPropertyTesterTest.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final RedXmlForNavigatorPropertyTester tester = new RedXmlForNavigatorPropertyTester();

    @BeforeClass
    public static void beforeSuite() throws Exception {
        projectProvider.createDir(Path.fromPortableString("excluded_dir"));
        projectProvider.createDir(Path.fromPortableString("included_dir"));

        final RobotProjectConfig config = new RobotProjectConfig();
        config.addExcludedPath("excluded_dir");
        projectProvider.configure(config);
    }

    @Test
    public void exceptionIsThrown_whenReceiverIsNotResource() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Property tester is unable to test properties of java.lang.Object. It should be used with "
                + IResource.class.getName());

        tester.test(new Object(), "property", null, true);
    }

    @Test
    public void falseIsReturned_whenExpectedValueIsAString() {
        final boolean testResult = tester.test(mock(IResource.class),
                RedXmlForNavigatorPropertyTester.IS_EXCLUDED, null, "value");

        assertThat(testResult).isFalse();
    }

    @Test
    public void falseIsReturnedForUnknownProperty() {
        assertThat(tester.test(mock(IResource.class), "unknown_property", null, true)).isFalse();
        assertThat(tester.test(mock(IResource.class), "unknown_property", null, false)).isFalse();
    }

    @Test
    public void testIsIncludedProperty() {
        assertThat(isIncluded(projectProvider.getDir(Path.fromPortableString("included_dir")), true)).isTrue();
        assertThat(isIncluded(projectProvider.getDir(Path.fromPortableString("included_dir")), false)).isFalse();

        assertThat(isIncluded(projectProvider.getDir(Path.fromPortableString("excluded_dir")), true)).isFalse();
        assertThat(isIncluded(projectProvider.getDir(Path.fromPortableString("excluded_dir")), false)).isTrue();
    }

    @Test
    public void testIsExcludedProperty() {
        assertThat(isExcluded(projectProvider.getDir(Path.fromPortableString("excluded_dir")), true)).isTrue();
        assertThat(isExcluded(projectProvider.getDir(Path.fromPortableString("excluded_dir")), false)).isFalse();

        assertThat(isExcluded(projectProvider.getDir(Path.fromPortableString("included_dir")), true)).isFalse();
        assertThat(isExcluded(projectProvider.getDir(Path.fromPortableString("included_dir")), false)).isTrue();
    }

    @Test
    public void testIsInternalFolderProperty() {
        assertThat(isInternalFolder(projectProvider.getDir(Path.fromPortableString("excluded_dir")), true)).isTrue();
        assertThat(isInternalFolder(projectProvider.getDir(Path.fromPortableString("excluded_dir")), false)).isFalse();

        assertThat(isInternalFolder(projectProvider.getDir(Path.fromPortableString("included_dir")), true)).isTrue();
        assertThat(isInternalFolder(projectProvider.getDir(Path.fromPortableString("included_dir")), false)).isFalse();

        assertThat(isInternalFolder(projectProvider.getProject(), true)).isFalse();
        assertThat(isInternalFolder(projectProvider.getProject(), false)).isTrue();
    }

    @Test
    public void testIsFileProperty() {
        assertThat(isFile(projectProvider.getDir(Path.fromPortableString("excluded_dir")), false)).isTrue();
        assertThat(isFile(projectProvider.getDir(Path.fromPortableString("excluded_dir")), true)).isFalse();

        assertThat(isFile(projectProvider.getDir(Path.fromPortableString("included_dir")), false)).isTrue();
        assertThat(isFile(projectProvider.getDir(Path.fromPortableString("included_dir")), true)).isFalse();

        assertThat(isFile(projectProvider.getProject(), true)).isFalse();
        assertThat(isFile(projectProvider.getProject(), false)).isTrue();
    }

    @Test
    public void testParentExcludedProperty() {
        assertThat(isExcludedViaInheritance(projectProvider.getDir(Path.fromPortableString("excluded_dir")), false)).isTrue();
        assertThat(isExcludedViaInheritance(projectProvider.getDir(Path.fromPortableString("excluded_dir")), true)).isFalse();

        assertThat(isExcludedViaInheritance(projectProvider.getDir(Path.fromPortableString("included_dir")), false)).isTrue();
        assertThat(isExcludedViaInheritance(projectProvider.getDir(Path.fromPortableString("included_dir")), true)).isFalse();

        assertThat(isExcludedViaInheritance(projectProvider.getProject(), true)).isFalse();
        assertThat(isExcludedViaInheritance(projectProvider.getProject(), false)).isTrue();
    }

    private boolean isIncluded(final IResource element, final boolean expected) {
        return tester.test(element, RedXmlForNavigatorPropertyTester.IS_INCLUDED, null, expected);
    }

    private boolean isExcluded(final IResource element, final boolean expected) {
        return tester.test(element, RedXmlForNavigatorPropertyTester.IS_EXCLUDED, null, expected);
    }

    private boolean isInternalFolder(final IResource element, final boolean expected) {
        return tester.test(element, RedXmlForNavigatorPropertyTester.IS_INTERNAL_FOLDER, null, expected);
    }

    private boolean isFile(final IResource element, final boolean expected) {
        return tester.test(element, RedXmlForNavigatorPropertyTester.IS_FILE, null, expected);
    }

    private boolean isExcludedViaInheritance(final IResource element, final boolean expected) {
        return tester.test(element, RedXmlForNavigatorPropertyTester.PARENT_EXCLUDED, null, expected);
    }
}
