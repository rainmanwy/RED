/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.rf.ide.core.dryrun.RobotDryRunKeywordSource;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.hyperlink.KeywordInLibrarySourceHyperlinkTest;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.red.junit.ProjectProvider;

import com.google.common.collect.ImmutableMap;

public class SourceOpeningSupportTest {

    @Rule
    public ProjectProvider projectProvider = new ProjectProvider(KeywordInLibrarySourceHyperlinkTest.class);

    private final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

    private final RobotModel model = new RobotModel();

    private IFile library;

    private LibrarySpecification libSpec;

    private KeywordSpecification kwSpec;

    private RobotProject project;

    private RobotDryRunKeywordSource kwSource;

    @Before
    public void before() throws Exception {
        final ReferencedLibrary lib = new ReferencedLibrary();
        lib.setType(LibraryType.PYTHON.toString());
        lib.setName("testlib");
        lib.setPath(projectProvider.getProject().getName());

        final RobotProjectConfig config = new RobotProjectConfig();
        config.addReferencedLibrary(lib);

        library = projectProvider.createFile("testlib.py", "#comment", "def keyword():", "  print(\"kw\")");
        projectProvider.configure(config);

        kwSpec = new KeywordSpecification();
        kwSpec.setFormat("ROBOT");
        kwSpec.setName("keyword");
        kwSpec.setArguments(new ArrayList<String>());
        kwSpec.setDocumentation("");

        libSpec = new LibrarySpecification();
        libSpec.setName("testlib");
        libSpec.getKeywords().add(kwSpec);
        libSpec.setSourceFile(library);

        project = model.createRobotProject(projectProvider.getProject());
        project.setStandardLibraries(new HashMap<String, LibrarySpecification>());
        project.setReferencedLibraries(ImmutableMap.of(lib, libSpec));

        kwSource = new RobotDryRunKeywordSource();
        kwSource.setFilePath(library.getRawLocation().toOSString());
        kwSource.setLibraryName(libSpec.getName());
        kwSource.setName(kwSpec.getName());
        kwSource.setLine(1);
        kwSource.setOffset(4);
        kwSource.setLength(7);
    }

    @After
    public void after() {
        page.closeAllEditors(false);
    }

    @Test
    public void testIfLibraryIsOpened() throws Exception {
        assertThat(page.getEditorReferences()).isEmpty();

        SourceOpeningSupport.open(page, model, project.getProject(), libSpec);

        assertThat(page.getEditorReferences()).hasSize(1);

        final IFileEditorInput editorInput = (IFileEditorInput) page.getEditorReferences()[0].getEditorInput();
        assertThat(editorInput.getFile()).isEqualTo(projectProvider.getFile("testlib.py"));

        final TextEditor editor = (TextEditor) page.getEditorReferences()[0].getEditor(false);
        final TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
        assertThat(selection.getText()).isEqualTo("");
        assertThat(selection.getStartLine()).isEqualTo(0);
        assertThat(selection.getOffset()).isEqualTo(0);
        assertThat(selection.getLength()).isEqualTo(0);
    }

    @Test
    public void testIfLibraryIsOpened_whenKeywordNotFound() throws Exception {
        assertThat(page.getEditorReferences()).isEmpty();

        SourceOpeningSupport.open(page, model, project.getProject(), libSpec, kwSpec);

        assertThat(page.getEditorReferences()).hasSize(1);

        final IFileEditorInput editorInput = (IFileEditorInput) page.getEditorReferences()[0].getEditorInput();
        assertThat(editorInput.getFile()).isEqualTo(projectProvider.getFile("testlib.py"));

        final TextEditor editor = (TextEditor) page.getEditorReferences()[0].getEditor(false);
        final TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
        assertThat(selection.getText()).isEqualTo("");
        assertThat(selection.getStartLine()).isEqualTo(0);
        assertThat(selection.getOffset()).isEqualTo(0);
        assertThat(selection.getLength()).isEqualTo(0);
    }

    @Test
    public void testIfLibraryIsOpenedAndTextIsSelected_whenKeywordFound() throws Exception {
        assertThat(page.getEditorReferences()).isEmpty();

        project.updateKeywordSources(Collections.singletonList(kwSource));

        SourceOpeningSupport.open(page, model, project.getProject(), libSpec, kwSpec);

        assertThat(page.getEditorReferences()).hasSize(1);

        final IFileEditorInput editorInput = (IFileEditorInput) page.getEditorReferences()[0].getEditorInput();
        assertThat(editorInput.getFile()).isEqualTo(projectProvider.getFile("testlib.py"));

        final TextEditor editor = (TextEditor) page.getEditorReferences()[0].getEditor(false);
        final TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
        assertThat(selection.getText()).isEqualTo("keyword");
        assertThat(selection.getStartLine()).isEqualTo(1);
        assertThat(selection.getOffset()).isEqualTo(13);
        assertThat(selection.getLength()).isEqualTo(7);
    }

    @Test
    public void testIfFileIsOpenedInEditor() throws Exception {
        assertThat(page.getEditorReferences()).isEmpty();

        SourceOpeningSupport.tryToOpenInEditor(page, library);

        assertThat(page.getEditorReferences()).hasSize(1);

        final IFileEditorInput editorInput = (IFileEditorInput) page.getEditorReferences()[0].getEditorInput();
        assertThat(editorInput.getFile()).isEqualTo(projectProvider.getFile("testlib.py"));

        final TextEditor editor = (TextEditor) page.getEditorReferences()[0].getEditor(false);
        final TextSelection selection = (TextSelection) editor.getSelectionProvider().getSelection();
        assertThat(selection.getText()).isEqualTo("");
        assertThat(selection.getStartLine()).isEqualTo(0);
        assertThat(selection.getOffset()).isEqualTo(0);
        assertThat(selection.getLength()).isEqualTo(0);
    }

    @Test
    public void testIfLibraryLocationIsExtracted() throws Exception {
        final IPath location = SourceOpeningSupport.extractLibraryLocation(model, project.getProject(), libSpec);
        assertThat(location.lastSegment()).isEqualTo("testlib.py");
    }
}