package gov.sparrow.listeners;

import android.text.Editable;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.models.factories.SpanFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(SparrowTestRunner.class)
public class AddSpanClickListenerTest {

    @Mock private View view;
    @Mock private EditText editText;
    @Mock private Editable editable;
    @Mock private SpanFactory spanFactory;
    private Object span;
    private AddSpanClickListener subject;

    @Before
    public void setUp() {
        initMocks(this);

        when(editText.getText()).thenReturn(editable);
        when(editText.getContext()).thenReturn(RuntimeEnvironment.application.getApplicationContext());

        when(editable.toString()).thenReturn("test text with some other stuff");
        when(editable.length()).thenReturn(123);

        when(spanFactory.makeSpan()).thenReturn(span);
        subject = new AddSpanClickListener(spanFactory, editText);
    }

    @Test
    public void creatingSpan_shouldReturnFocusToEditText() {
        when(editText.getSelectionStart()).thenReturn(0);
        when(editText.getSelectionEnd()).thenReturn(9);

        subject.onClick(view);

        verify(editText).clearFocus();
        verify(editText).requestFocus();
    }

    @Test
    public void onAddSpanClicked_shouldHandleBackwardsSelectionAnchors() {
        when(editText.getSelectionStart()).thenReturn(14);
        when(editText.getSelectionEnd()).thenReturn(5);

        subject.onClick(view);

        verify(editable).setSpan(eq(span), eq(5), eq(14), eq(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE));
    }

    @Test
    public void onAddSpanClicked_shouldSetSpan() {
        when(editText.getSelectionStart()).thenReturn(0);
        when(editText.getSelectionEnd()).thenReturn(9);

        subject.onClick(view);

        verify(editable).setSpan(eq(span), eq(0), eq(9), eq(Spanned.SPAN_EXCLUSIVE_EXCLUSIVE));
    }

    @Test
    public void onAddSpanClicked_whenNoSelection_doNothing() {
        when(editText.getSelectionStart()).thenReturn(0);
        when(editText.getSelectionEnd()).thenReturn(0);

        subject.onClick(view);

        verifyZeroInteractions(editable);
    }

}