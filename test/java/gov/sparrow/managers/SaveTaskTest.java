package gov.sparrow.managers;

import android.os.Handler;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.models.Note;
import gov.sparrow.models.builders.NoteBuilder;
import gov.sparrow.models.links.ActionLink;
import gov.sparrow.models.links.StyleLink;
import gov.sparrow.repository.NoteRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SaveTaskTest {

    @Mock NoteRepository noteRepository;
    @Mock Handler handler;
    @Mock SaveManager.SaveCompleteListener saveListener;
    @Captor ArgumentCaptor<Runnable> taskCaptor;
    private SaveTask subject;

    @Before
    public void setUp() {
        initMocks(this);

        Note note = NoteBuilder.noteBuilder()
                .id(1L)
                .title("test title")
                .body("test body")
                .lastSaved("test time")
                .actionLinks(asList(new ActionLink("123", 0, 25, false)))
                .styleLinks(asList(new StyleLink(StyleLink.BOLD_STYLE, 26, 29)))
                .build();

        subject = new SaveTask(noteRepository, note, handler, saveListener);
    }

    @Test
    public void onRun_shouldCallUpdate_onNoteRepository() throws Exception {
        subject.run();

        verify(handler).post(taskCaptor.capture());
        taskCaptor.getValue().run();

        verify(noteRepository).updateNote(
                eq(1L),
                eq("test title"),
                eq("test body"),
                eq("test time"),
                eq(asList(new ActionLink("123", 0, 25, false))),
                eq(asList(new StyleLink(StyleLink.BOLD_STYLE, 26, 29))));
    }

    @Test
    public void onRun_shouldCallSaveCompleteListener() throws Exception {
        subject.run();

        verify(handler).post(taskCaptor.capture());
        taskCaptor.getValue().run();

        verify(noteRepository).updateNote(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                anyListOf(ActionLink.class),
                anyListOf(StyleLink.class));

        verify(saveListener).onSaveComplete();
    }
}