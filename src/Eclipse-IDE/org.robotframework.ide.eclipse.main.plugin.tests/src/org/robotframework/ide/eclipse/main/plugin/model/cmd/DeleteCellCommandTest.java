package org.robotframework.ide.eclipse.main.plugin.model.cmd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.Before;
import org.junit.Test;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.robotframework.ide.eclipse.main.plugin.mockmodel.RobotSuiteFileCreator;
import org.robotframework.ide.eclipse.main.plugin.model.IRobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCase;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCasesSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordDefinition;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSettingsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.EditorCommand;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class DeleteCellCommandTest {

    private IEventBroker eventBroker;

    @Before
    public void beforeTest() {
        eventBroker = mock(IEventBroker.class);
    }

    @Test
    public void deleteCell_atDifferentCallPositions_inTestCases() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Test Cases ***")
                .appendLine("case")
                .appendLine("  call  single arg")
                .appendLine("  call  1  2  #comment  sth")
                .build();
        final RobotCase robotCase = model.findSection(RobotCasesSection.class).get().getChildren().get(0);
        for (final RobotKeywordCall call : robotCase.getChildren()) {
            final int index = call.getIndex();
            final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
            final List<String> allLabels = callTokens.stream().map(RobotToken::getText).collect(Collectors.toList());
            final int tokensNumber = callTokens.size();
            for (int i = 0; i < tokensNumber; i++) {
                final DeleteCellCommand command = new DeleteCellCommand(call, i);
                command.setEventBroker(eventBroker);
                command.execute();

                final RobotKeywordCall callAfter = robotCase.getChildren().get(index);
                assertThat_valueAtPosition_wasDeleted(i, allLabels, callAfter);
                undo_andAssertThat_valueReturned_afterUndo(command, allLabels, callAfter);
            }
            verify(eventBroker, times(tokensNumber)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                    eq(ImmutableMap
                            .<String, Object>of(IEventBroker.DATA, robotCase, RobotModelEvents.ADDITIONAL_DATA, call)));
        }
    }

    @Test
    public void deleteCell_toCreateEmptyLine_inTestCases() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Test Cases ***")
                .appendLine("case")
                .appendLine("  call")
                .build();
        final RobotCase robotCase = model.findSection(RobotCasesSection.class).get().getChildren().get(0);
        final RobotKeywordCall call = robotCase.getChildren().get(0);
        final DeleteCellCommand command = new DeleteCellCommand(call, 0);
        command.setEventBroker(eventBroker);
        command.execute();

        final RobotKeywordCall callAfter = robotCase.getChildren().get(0);

        assertThat(callAfter.getLinkedElement().getElementTokens().stream().map(RobotToken::getText)
                .collect(Collectors.toList())).containsExactly("");

        for (final EditorCommand toUndo : command.getUndoCommands()) {
            toUndo.execute();
        }
        final List<String> currentLabels = robotCase.getChildren().get(0).getLinkedElement().getElementTokens()
                .stream().map(RobotToken::getText).collect(Collectors.toList());

        assertThat(currentLabels).containsExactly("call");

        verify(eventBroker, times(1)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                eq(ImmutableMap
                        .<String, Object>of(IEventBroker.DATA, robotCase, RobotModelEvents.ADDITIONAL_DATA, call)));
    }

    @Test
    public void deleteNonFirstCell_inCommentLineOnly_inTestCases() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Test Cases ***")
                .appendLine("case")
                .appendLine("  #comment  line  only")
                .build();
        final RobotCase robotCase = model.findSection(RobotCasesSection.class).get().getChildren().get(0);
        final RobotKeywordCall call = robotCase.getChildren().get(0);

        final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
        final List<String> allLabelsWithFirstEmpty = callTokens.stream().map(RobotToken::getText)
                .collect(Collectors.toList());
        for (int i = 1; i < 3; i++) {
            final DeleteCellCommand command = new DeleteCellCommand(call, i);
            command.setEventBroker(eventBroker);
            command.execute();

            final RobotKeywordCall callAfter = robotCase.getChildren().get(0);
            assertThat_valueAtPosition_wasDeleted(i + 1, allLabelsWithFirstEmpty, callAfter);

            final IRobotCodeHoldingElement parent = call.getParent();
            final int index = call.getIndex();
            for (final EditorCommand toUndo : command.getUndoCommands()) {
                toUndo.execute();
            }
            final List<String> currentLabels = parent.getChildren().get(index).getLinkedElement().getElementTokens()
                    .stream().map(RobotToken::getText).collect(Collectors.toList());

            assertThat(currentLabels)
                    .containsExactlyElementsOf(allLabelsWithFirstEmpty);
        }

        verify(eventBroker, times(2)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                eq(ImmutableMap
                        .<String, Object>of(IEventBroker.DATA, robotCase, RobotModelEvents.ADDITIONAL_DATA, call)));
    }

    @Test
    public void deleteFirstCell_inCommentLineOnly_inTestCases() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Test Cases ***")
                .appendLine("case")
                .appendLine("  #comment  line  only")
                .build();
        final RobotCase robotCase = model.findSection(RobotCasesSection.class).get().getChildren().get(0);
        final RobotKeywordCall call = robotCase.getChildren().get(0);

        final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
        final List<String> allLabels = callTokens.stream().map(RobotToken::getText)
                .collect(Collectors.toList());
        allLabels.remove(0); // first artificial comment token is empty and not visible for the user
        final DeleteCellCommand command = new DeleteCellCommand(call, 0);
        command.setEventBroker(eventBroker);
        command.execute();

        final RobotKeywordCall callAfter = robotCase.getChildren().get(0);
        assertThat_valueAtPosition_wasDeleted(0, allLabels, callAfter);

        final IRobotCodeHoldingElement parent = call.getParent();
        final int index = call.getIndex();
        for (final EditorCommand toUndo : command.getUndoCommands()) {
            toUndo.execute();
        }
        final List<String> currentLabels = parent.getChildren().get(index).getLinkedElement().getElementTokens()
                .stream().map(RobotToken::getText).collect(Collectors.toList());

        allLabels.add(0, ""); // re-add artificial empty token just for testing purposes

        assertThat(currentLabels)
                .containsExactlyElementsOf(allLabels);

        verify(eventBroker, times(1)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                eq(ImmutableMap
                        .<String, Object>of(IEventBroker.DATA, robotCase, RobotModelEvents.ADDITIONAL_DATA, call)));
    }

    @Test
    public void deleteCell_atDifferentCallPositions_inKeywords() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Keyword ***")
                .appendLine("kw")
                .appendLine("  call  single arg")
                .appendLine("  call  1  2  #comment  sth")
                .build();
        final RobotKeywordDefinition robotKeyword = model.findSection(RobotKeywordsSection.class).get().getChildren()
                .get(0);
        for (final RobotKeywordCall call : robotKeyword.getChildren()) {
            final int index = call.getIndex();
            final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
            final List<String> allLabels = callTokens.stream().map(RobotToken::getText).collect(Collectors.toList());
            final int tokensNumber = callTokens.size();
            for (int i = 0; i < tokensNumber; i++) {
                final DeleteCellCommand command = new DeleteCellCommand(call, i);
                command.setEventBroker(eventBroker);
                command.execute();

                final RobotKeywordCall callAfter = robotKeyword.getChildren().get(index);
                assertThat_valueAtPosition_wasDeleted(i, allLabels, callAfter);
                undo_andAssertThat_valueReturned_afterUndo(command, allLabels, callAfter);
            }
            verify(eventBroker, times(tokensNumber)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                    eq(ImmutableMap
                            .<String, Object>of(IEventBroker.DATA, robotKeyword, RobotModelEvents.ADDITIONAL_DATA,
                                    call)));
        }
    }

    @Test
    public void deleteCell_toCreateEmptyLine_inKeywords() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Keywords ***")
                .appendLine("kw")
                .appendLine("  call")
                .build();
        final RobotKeywordDefinition robotKeyword = model.findSection(RobotKeywordsSection.class).get().getChildren()
                .get(0);
        final RobotKeywordCall call = robotKeyword.getChildren().get(0);
        final DeleteCellCommand command = new DeleteCellCommand(call, 0);
        command.setEventBroker(eventBroker);
        command.execute();

        final RobotKeywordCall callAfter = robotKeyword.getChildren().get(0);

        assertThat(callAfter.getLinkedElement().getElementTokens().stream().map(RobotToken::getText)
                .collect(Collectors.toList())).containsExactly("");

        for (final EditorCommand toUndo : command.getUndoCommands()) {
            toUndo.execute();
        }
        final List<String> currentLabels = robotKeyword.getChildren().get(0).getLinkedElement().getElementTokens()
                .stream().map(RobotToken::getText).collect(Collectors.toList());

        assertThat(currentLabels).containsExactly("call");

        verify(eventBroker, times(1)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                eq(ImmutableMap
                        .<String, Object>of(IEventBroker.DATA, robotKeyword, RobotModelEvents.ADDITIONAL_DATA, call)));
    }

    @Test
    public void deleteNonFirstCell_inCommentLineOnly_inKeywords() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Keywords ***")
                .appendLine("kw")
                .appendLine("  #comment  line  only")
                .build();
        final RobotKeywordDefinition robotKeyword = model.findSection(RobotKeywordsSection.class).get().getChildren()
                .get(0);
        final RobotKeywordCall call = robotKeyword.getChildren().get(0);

        final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
        final List<String> allLabelsWithFirstEmpty = callTokens.stream().map(RobotToken::getText)
                .collect(Collectors.toList());
        for (int i = 1; i < 3; i++) {
            final DeleteCellCommand command = new DeleteCellCommand(call, i);
            command.setEventBroker(eventBroker);
            command.execute();

            final RobotKeywordCall callAfter = robotKeyword.getChildren().get(0);
            assertThat_valueAtPosition_wasDeleted(i + 1, allLabelsWithFirstEmpty, callAfter);

            final IRobotCodeHoldingElement parent = call.getParent();
            final int index = call.getIndex();
            for (final EditorCommand toUndo : command.getUndoCommands()) {
                toUndo.execute();
            }
            final List<String> currentLabels = parent.getChildren().get(index).getLinkedElement().getElementTokens()
                    .stream().map(RobotToken::getText).collect(Collectors.toList());

            assertThat(currentLabels)
                    .containsExactlyElementsOf(allLabelsWithFirstEmpty);
        }

        verify(eventBroker, times(2)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                eq(ImmutableMap
                        .<String, Object>of(IEventBroker.DATA, robotKeyword, RobotModelEvents.ADDITIONAL_DATA, call)));
    }

    @Test
    public void deleteFirstCell_inCommentLineOnly_inKeywords() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Keywords ***")
                .appendLine("kw")
                .appendLine("  #comment  line  only")
                .build();
        final RobotKeywordDefinition robotKeyword = model.findSection(RobotKeywordsSection.class).get().getChildren()
                .get(0);
        final RobotKeywordCall call = robotKeyword.getChildren().get(0);

        final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
        final List<String> allLabels = callTokens.stream().map(RobotToken::getText)
                .collect(Collectors.toList());
        allLabels.remove(0); // first artificial comment token is empty and not visible for the user
        final DeleteCellCommand command = new DeleteCellCommand(call, 0);
        command.setEventBroker(eventBroker);
        command.execute();

        final RobotKeywordCall callAfter = robotKeyword.getChildren().get(0);
        assertThat_valueAtPosition_wasDeleted(0, allLabels, callAfter);

        final IRobotCodeHoldingElement parent = call.getParent();
        final int index = call.getIndex();
        for (final EditorCommand toUndo : command.getUndoCommands()) {
            toUndo.execute();
        }
        final List<String> currentLabels = parent.getChildren().get(index).getLinkedElement().getElementTokens()
                .stream().map(RobotToken::getText).collect(Collectors.toList());

        allLabels.add(0, ""); // re-add artificial empty token just for testing purposes

        assertThat(currentLabels)
                .containsExactlyElementsOf(allLabels);

        verify(eventBroker, times(1)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                eq(ImmutableMap
                        .<String, Object>of(IEventBroker.DATA, robotKeyword, RobotModelEvents.ADDITIONAL_DATA, call)));
    }

    @Test
    public void deleteCell_atDifferentCallPositions_inImports() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Settings ***")
                .appendLine("Library  lib  arg1  arg2  arg3")
                .appendLine("Resource  res.robot  arg1  arg2  arg3")
                .appendLine("Variables  var.py  arg1  arg2  arg3")
                .build();
        final RobotSettingsSection robotImports = model.findSection(RobotSettingsSection.class).get();
        for (final RobotKeywordCall call : robotImports.getChildren()) {
            final int index = call.getIndex();
            final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
            final List<String> allLabels = callTokens.stream().map(RobotToken::getText).collect(Collectors.toList());
            final int tokensNumber = callTokens.size();
            for (int i = 2; i < tokensNumber; i++) {
                final DeleteCellCommand command = new DeleteCellCommand(call, i);
                command.setEventBroker(eventBroker);
                command.execute();

                final RobotKeywordCall callAfter = robotImports.getChildren().get(index);
                assertThat_valueAtPosition_wasDeleted(i, allLabels, callAfter);
                undo_andAssertThat_valueReturned_afterUndo(command, allLabels, callAfter);
            }
            verify(eventBroker, times(tokensNumber - 2)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                    eq(ImmutableMap
                            .<String, Object>of(IEventBroker.DATA, robotImports, RobotModelEvents.ADDITIONAL_DATA,
                                    call)));
        }
    }

    @Test
    public void deleteCell_atDifferentCallPositions_inGeneralSettings() {
        final RobotSuiteFile model = new RobotSuiteFileCreator()
                .appendLine("*** Settings ***")
                .appendLine("Suite Setup  kw  arg1  arg2")
                .appendLine("Suite Teardown  kw  arg1  arg2")
                .appendLine("Test Setup  kw  arg1  arg2")
                .appendLine("Test Teardown  kw  arg1  arg2")
                .appendLine("Test Template  kw  arg1  arg2")
                .appendLine("Test Timeout  12")
                .appendLine("Force Tags  tag1  tag2  tag3")
                .appendLine("Default Tags  tag1  tag2  tag3")
                .build();
        final RobotSettingsSection robotImports = model.findSection(RobotSettingsSection.class).get();
        for (final RobotKeywordCall call : robotImports.getChildren()) {
            final int index = call.getIndex();
            final List<RobotToken> callTokens = call.getLinkedElement().getElementTokens();
            final List<String> allLabels = callTokens.stream().map(RobotToken::getText).collect(Collectors.toList());
            final int tokensNumber = callTokens.size();
            for (int i = 2; i < tokensNumber; i++) {
                final DeleteCellCommand command = new DeleteCellCommand(call, i);
                command.setEventBroker(eventBroker);
                command.execute();

                final RobotKeywordCall callAfter = robotImports.getChildren().get(index);
                assertThat_valueAtPosition_wasDeleted(i, allLabels, callAfter);
                undo_andAssertThat_valueReturned_afterUndo(command, allLabels, callAfter);
            }
            verify(eventBroker, times(tokensNumber - 2)).send(eq(RobotModelEvents.ROBOT_KEYWORD_CALL_CONVERTED),
                    eq(ImmutableMap
                            .<String, Object>of(IEventBroker.DATA, robotImports, RobotModelEvents.ADDITIONAL_DATA,
                                    call)));
        }
    }

    private void assertThat_valueAtPosition_wasDeleted(final int deletedPosition, final List<String> allLabels,
            final RobotKeywordCall call) {
        final List<String> currentLabels = call.getLinkedElement().getElementTokens().stream().map(RobotToken::getText)
                .collect(Collectors.toList());
        final List<String> oneLessLabels = Lists.newArrayList(allLabels);
        oneLessLabels.remove(deletedPosition);

        assertThat(currentLabels).containsExactlyElementsOf(oneLessLabels);
    }

    private void undo_andAssertThat_valueReturned_afterUndo(final DeleteCellCommand executed,
            final List<String> allLabels, final RobotKeywordCall call) {
        final IRobotCodeHoldingElement parent = call.getParent();
        final int index = call.getIndex();
        for (final EditorCommand command : executed.getUndoCommands()) {
            command.execute();
        }
        final List<String> currentLabels = parent.getChildren().get(index).getLinkedElement().getElementTokens()
                .stream().map(RobotToken::getText).collect(Collectors.toList());

        assertThat(currentLabels).containsExactlyElementsOf(allLabels);
    }
}
