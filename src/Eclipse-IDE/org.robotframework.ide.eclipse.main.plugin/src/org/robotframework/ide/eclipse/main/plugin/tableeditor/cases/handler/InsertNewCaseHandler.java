package org.robotframework.ide.eclipse.main.plugin.tableeditor.cases.handler;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCase;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCasesSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFileSection;
import org.robotframework.ide.eclipse.main.plugin.model.cmd.CreateFreshCaseCommand;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotEditorCommandsStack;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.cases.handler.InsertNewCaseHandler.E4InsertNewCaseHandler;
import org.robotframework.red.viewers.Selections;

public class InsertNewCaseHandler extends DIHandler<E4InsertNewCaseHandler> {

    public InsertNewCaseHandler() {
        super(E4InsertNewCaseHandler.class);
    }

    public static class E4InsertNewCaseHandler {

        @Inject
        private RobotEditorCommandsStack stack;

        @Execute
        public Object addNewTestCase(@Named(Selections.SELECTION) final IStructuredSelection selection) {
            final RobotElement selectedElement = Selections.getSingleElement(selection, RobotElement.class);

            RobotSuiteFileSection section = null;
            RobotCase testCase = null;
            if (selectedElement instanceof RobotKeywordCall) {
                testCase = (RobotCase) selectedElement.getParent();
                section = ((RobotKeywordCall) selectedElement).getSection();
            } else if (selectedElement instanceof RobotCase) {
                testCase = (RobotCase) selectedElement;
                section = (RobotSuiteFileSection) testCase.getParent();
            }

            if (section == null || testCase == null) {
                return null;
            }

            final int index = section.getChildren().indexOf(testCase);
            stack.execute(new CreateFreshCaseCommand((RobotCasesSection) section, index));
            return null;
        }
    }
}
