package org.robotframework.ide.core.testData.model.table.setting.mapping;

import java.util.List;
import java.util.Stack;

import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.RobotFileOutput;
import org.robotframework.ide.core.testData.model.table.SettingTable;
import org.robotframework.ide.core.testData.model.table.mapping.ElementsUtility;
import org.robotframework.ide.core.testData.model.table.mapping.IParsingMapper;
import org.robotframework.ide.core.testData.model.table.setting.SuiteDocumentation;
import org.robotframework.ide.core.testData.text.read.ParsingState;
import org.robotframework.ide.core.testData.text.read.RobotLine;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;

import com.google.common.annotations.VisibleForTesting;


public class SettingDocumentationTextMapper implements IParsingMapper {

    private final ElementsUtility utility;


    public SettingDocumentationTextMapper() {
        this.utility = new ElementsUtility();
    }


    @Override
    public RobotToken map(RobotLine currentLine,
            Stack<ParsingState> processingState,
            RobotFileOutput robotFileOutput, RobotToken rt, FilePosition fp,
            String text) {
        rt.setType(RobotTokenType.SETTING_DOCUMENTATION_TEXT);
        rt.setText(new StringBuilder(text));

        SettingTable settings = robotFileOutput.getFileModel()
                .getSettingTable();
        List<SuiteDocumentation> documentation = settings.getDocumentation();
        if (!documentation.isEmpty()) {
            documentation.get(documentation.size() - 1)
                    .addDocumentationText(rt);
        } else {
            // FIXME: some error
        }
        processingState.push(ParsingState.SETTING_DOCUMENTATION_TEXT);

        return rt;
    }


    @Override
    public boolean checkIfCanBeMapped(RobotFileOutput robotFileOutput,
            RobotLine currentLine, RobotToken rt, String text,
            Stack<ParsingState> processingState) {
        boolean result = false;
        ParsingState state = utility.getCurrentStatus(processingState);
        if (state == ParsingState.SETTING_DOCUMENTATION
                || state == ParsingState.SETTING_DOCUMENTATION_TEXT) {
            result = true;
        }

        return result;
    }


    @VisibleForTesting
    protected boolean isIncludedInSettingTable(final RobotLine line,
            final Stack<ParsingState> processingState) {
        boolean result;
        if (!processingState.isEmpty()) {
            result = (processingState.get(processingState.size() - 1) == ParsingState.SETTING_TABLE_INSIDE);
        } else {
            result = false;
        }

        return result;
    }
}
