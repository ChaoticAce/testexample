package gov.sparrow.models;

import android.graphics.Typeface;
import android.text.style.StyleSpan;
import gov.sparrow.SparrowTestRunner;
import gov.sparrow.models.factories.BoldSpanFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class BoldSpanFactoryTest {

    private BoldSpanFactory subject;

    @Before
    public void setUp() throws Exception {
        subject = new BoldSpanFactory();
    }

    @Test
    public void makeSpan_returnsStyleSpan() throws Exception {
        assertThat(subject.makeSpan()).isInstanceOf(StyleSpan.class);
    }

    @Test
    public void makeSpan_hasBoldTypefaceFlag() throws Exception {
        StyleSpan boldSpan = (StyleSpan) subject.makeSpan();
        assertThat(boldSpan.getStyle()).isEqualTo(Typeface.BOLD);
    }

}