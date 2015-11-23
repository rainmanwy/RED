/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.testCases.mapping;

import java.util.List;
import java.util.Stack;

import org.rf.ide.core.testdata.model.RobotFileOutput;
import org.rf.ide.core.testdata.model.table.mapping.IParsingMapper;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.RobotLine;
import org.rf.ide.core.testdata.text.read.columnSeparators.Separator.SeparatorType;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;


public abstract class ATestCaseSettingDeclarationMapper implements
        IParsingMapper {

    private final IRobotTokenType mappedType;
    protected final TestCaseFinder finder;


    protected ATestCaseSettingDeclarationMapper(final IRobotTokenType mappedType) {
        this.mappedType = mappedType;
        this.finder = new TestCaseFinder();
    }


    @Override
    public boolean checkIfCanBeMapped(RobotFileOutput robotFileOutput,
            RobotLine currentLine, RobotToken rt, String text,
            Stack<ParsingState> processingState) {
        boolean result = false;

        if (rt.getTypes().get(0) == mappedType) {
            List<IRobotLineElement> lineElements = currentLine
                    .getLineElements();
            int size = lineElements.size();
            if (size == 1) {
                List<IRobotTokenType> types = lineElements.get(0).getTypes();
                result = (types.contains(SeparatorType.PIPE) || types
                        .contains(SeparatorType.TABULATOR_OR_DOUBLE_SPACE));
            } else {
                for (IRobotLineElement elem : lineElements) {
                    List<IRobotTokenType> types = elem.getTypes();
                    if (types.contains(SeparatorType.PIPE)
                            || types.contains(SeparatorType.TABULATOR_OR_DOUBLE_SPACE)) {
                        continue;
                    } else if (types.contains(RobotTokenType.TEST_CASE_NAME)) {
                        result = true;
                        break;
                    } else {
                        result = false;
                        break;
                    }
                }
            }
        }

        return result;
    }

}
