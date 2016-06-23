package gov.sparrow.models;

import gov.sparrow.R;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.models.factories.ActionLinkSpanFactory;
import gov.sparrow.models.spans.ActionLinkSpan;
import gov.sparrow.util.UUIDGenerator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class ActionLinkSpanFactoryTest {

    @Mock private UUIDGenerator uuidGenerator;
    private ActionLinkSpanFactory subject;

    @Before
    public void setUp() {
        initMocks(this);
        when(uuidGenerator.getUUID()).thenReturn("test uuid");
        subject = new ActionLinkSpanFactory(RuntimeEnvironment.application, uuidGenerator);
    }

    @Test
    public void makeSpan_makesActionLinkSpan() throws Exception {
        assertThat(subject.makeSpan()).isInstanceOf(ActionLinkSpan.class);
    }

    @Test
    public void onMakeSpan_shouldSetNoteId_andActionLinkId() {
        ActionLinkSpan linkSpan = (ActionLinkSpan) subject.makeSpan();
        assertThat(linkSpan.getBackgroundColor()).isEqualTo(RuntimeEnvironment.application.getResources().getColor(R.color.action_dark_yellow));
        assertThat(linkSpan.getUuid()).isEqualTo("test uuid");
    }

}