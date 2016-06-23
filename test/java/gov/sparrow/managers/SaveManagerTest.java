package gov.sparrow.managers;

import android.text.Editable;
import android.text.style.StyleSpan;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.models.spans.ActionLinkSpan;
import gov.sparrow.repository.NoteRepository;
import gov.sparrow.util.TimeUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class SaveManagerTest {

    @Mock TimeUtil timeUtil;
    @Mock ScheduledExecutorService scheduler;
    @Mock NoteRepository noteRepository;
    @Mock Editable title;
    @Mock Editable body;
    @Mock SaveManager.SaveCompleteListener saveListener;
    @Mock ScheduledFuture scheduledFuture;
    private SaveManager subject;

    @Before
    public void setUp() {
        initMocks(this);

        when(timeUtil.getTimeNow())
                .thenReturn("test time");

        when(scheduler.schedule(any(SaveTask.class), anyLong(), any(TimeUnit.class)))
                .thenReturn(scheduledFuture);

        when(body.length()).thenReturn(10);
        when(body.getSpans(0, 10, ActionLinkSpan.class))
                .thenReturn(new ActionLinkSpan[]{});
        when(body.getSpans(0, 10, StyleSpan.class))
                .thenReturn(new StyleSpan[]{});

        subject = new SaveManager(timeUtil, scheduler, noteRepository);
        subject.setEnabled(true);

    }

    @Test
    public void onSave_shouldSubmitSaveTask() {
        subject.scheduleSave(1L, title, body, saveListener);
        subject.save(1L, title, body, mock(SaveManager.SaveCompleteListener.class));
        verify(scheduler).submit(any(SaveTask.class));
    }

    @Test
    public void onSave_shouldCancelPreviouslyScheduledSave() {
        subject.scheduleSave(1L, title, body, saveListener);
        subject.save(1L, title, body, saveListener);
        verify(scheduledFuture).cancel(false);
    }

    @Test
    public void onScheduleSave_shouldCallScheduler_withSaveTask() {
        subject.scheduleSave(1L, title, body, saveListener);
        verify(scheduler).schedule(any(SaveTask.class), eq(SaveManager.DELAY), eq(SaveManager.TIME_UNIT));
    }

    @Test
    public void onScheduleSave_shouldCancelPreviouslyScheduledSave() {
        subject.scheduleSave(1L, title, body, saveListener);
        subject.scheduleSave(1L, title, body, saveListener);

        verify(scheduledFuture).cancel(false);
        verify(scheduler, times(2)).schedule(any(SaveTask.class), eq(SaveManager.DELAY), eq(SaveManager.TIME_UNIT));
    }

    @Test
    public void onSave_shouldNotSave_whenNoChangesDetected() {
        subject.save(1L, title, body, saveListener);
        verifyZeroInteractions(scheduler);
    }

    @Test
    public void onSave_shouldNotSave_whenNoChangesDetected_andCallOnSaveComplete() {
        subject.save(1L, title, body, saveListener);
        verify(saveListener).onSaveComplete();
    }

    @Test
    public void onSave_shouldNotSave_whenScheduledSaveIsDone() {
        subject.scheduleSave(1L, title, body, saveListener);
        when(scheduledFuture.isDone()).thenReturn(true);

        reset(scheduler);
        subject.save(1L, title, body, saveListener);
        verifyZeroInteractions(scheduler);
    }

    @Test
    public void onSave_shouldNotSave_whenScheduledSaveIsDone_andCallOnSaveComplete() {
        subject.scheduleSave(1L, title, body, saveListener);
        when(scheduledFuture.isDone()).thenReturn(true);

        subject.save(1L, title, body, saveListener);
        verify(saveListener).onSaveComplete();
    }

    @Test
    public void setEnabled_whenFalse_shouldDisableSave() throws Exception {
        subject.setEnabled(false);
        subject.save(1L, title, body, saveListener);

        verify(saveListener, never()).onSaveComplete();
    }

    @Test
    public void setEnabled_whenFalse_shouldDisableScheduleSave() throws Exception {
        subject.setEnabled(false);
        subject.scheduleSave(1L, title, body, saveListener);

        verify(saveListener, never()).onSaveComplete();
    }

    @Test
    public void setEnabled_whenFalse_shouldCancelPendingSaves() throws Exception {
        subject.scheduleSave(1L, title, body, saveListener);
        subject.setEnabled(false);
        verify(scheduledFuture).cancel(true);
    }

    @Test
    public void setEnabled_whenFalse_shouldDisregardNullFuture() throws Exception {
        subject.setEnabled(false);
        verifyZeroInteractions(scheduledFuture);
    }
}