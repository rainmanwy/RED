package org.robotframework.ide.eclipse.main.plugin.tableeditor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ProblemCategory.Severity;


class SuiteFileMarkersContainerMock implements SuiteFileMarkersContainer {

    private final Map<RobotFileInternalElement, Severity> markers = new HashMap<>();

    @Override
    public Optional<Severity> getHighestSeverityMarkerFor(final Optional<? extends RobotFileInternalElement> element) {
        if (element.isPresent() && markers.containsKey(element.get())) {
            return Optional.of(markers.get(element.get()));
        }
        return Optional.empty();
    }

    @Override
    public List<String> getMarkersMessagesFor(final Optional<? extends RobotFileInternalElement> element) {
        return null;
    }

    public void registerMarker(final RobotFileInternalElement element, final Severity highestSeverity) {
        markers.put(element, highestSeverity);
    }
}
