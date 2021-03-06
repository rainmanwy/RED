/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.wizards;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;

class WizardNewRobotPythonFilePage extends WizardNewFileCreationPage {

    private final Map<Template, Button> buttons = new LinkedHashMap<>();

    private IStructuredSelection currentSelection;

    WizardNewRobotPythonFilePage(final String pageName, final IStructuredSelection selection) {
        super(pageName, selection);
        currentSelection = selection;
        setFileExtension("py");
    }

    @Override
    protected InputStream getInitialContents() {
        Template chosenTemplate = null;
        for (final Entry<Template, Button> entry : buttons.entrySet()) {
            if (entry.getValue().getSelection()) {
                chosenTemplate = entry.getKey();
                break;
            }
        }
        final String name = getFileName().endsWith(".py") ? getFileName().substring(0, getFileName().length() - 3)
                : getFileName();
        final String formattedContent = String.format(chosenTemplate.content, name);
        return new ByteArrayInputStream(formattedContent.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void createAdvancedControls(final Composite parent) {
        final Composite composite = new Composite(parent, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(composite);
        GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(true).applyTo(composite);

        for (final Template template : EnumSet.allOf(Template.class)) {
            final Button button = new Button(composite, SWT.RADIO);
            button.setText(template.label);
            buttons.put(template, button);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(button);
        }
        new Label(parent, SWT.NONE);
        buttons.get(Template.EMPTY).setSelection(true);

        super.createAdvancedControls(parent);
    }

    @Override
    protected boolean validatePage() {
        boolean isProjectavailable = false;
        Object[] selection = currentSelection.toArray();
        final boolean isValid = super.validatePage();
        if (!(selection.length == 0)) {
            for (Object project : selection) {
                while (project instanceof IFolder || project instanceof IFile) {
                    project = ((IResource) project).getParent();
                }
                if (project instanceof IProject) {
                    if (((IProject) project).isOpen()) {
                        isProjectavailable = true;
                        break;
                    }
                }
            }
        }
        if (!isProjectavailable && !isValid) {
            setErrorMessage("Action impossible to finish: No project available");
            return false;
        }

        if (!isValid) {
            return false;
        }
        String name = getFileName().contains(".py") ? getFileName() : getFileName().concat(".py");

        IPath resourcePath = getContainerFullPath().append(getFileName() + getFileExtension());
        IFile file = createFileHandle(resourcePath);

        final IContainer container = file.getParent();
        if (!container.exists()) {
            setErrorMessage("Folder '" + container.getFullPath().toString() + "' does not exists.");
            return false;
        }

        try {
            for (final IResource resource : container.members()) {
                if (name.equalsIgnoreCase(resource.getName())) {
                    setErrorMessage("'" + name + "' already exists.");
                    return false;
                }
            }
        } catch (final CoreException e) {
            ErrorDialog.openError(getShell(), "Problem occurred", "Error when validating wizard page", e.getStatus());
        }
        return true;
    }

    private static enum Template {
        VARIABLES_GLOBAL("Variables file", PythonTemplates.variables),
        LIBRARY("Library", PythonTemplates.library),
        VARIABLES_CLASS("Variables file with class", PythonTemplates.variablesWithClass),
        LIBRARY_DYNAMIC("Dynamic API library", PythonTemplates.dynamicLibrary),
        EMPTY("Empty content", "");

        private final String label;
        private final String content;

        private Template(final String label, final String content) {
            this.label = label;
            this.content = content;
        }
    }
}
