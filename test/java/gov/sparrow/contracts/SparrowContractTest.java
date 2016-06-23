package gov.sparrow.contracts;

import android.net.Uri;
import gov.sparrow.SparrowTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SparrowTestRunner.class)
public class SparrowContractTest {

    @Test
    public void buildUri_createsUriForAccessNoteContent() {
        Uri uri = NoteContract.Note.CONTENT_URI;

        assertThat(uri.toString()).isEqualTo("content://gov.sparrow.provider.SparrowProvider/notes");
    }

}