package gov.sparrow.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.TimeZone;

import static org.fest.assertions.api.Assertions.assertThat;

public class TimeUtilTest {

    private TimeUtil subject;
    private TimeZone timeZone;

    @Before
    public void setUp() {
        timeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("EST"));
        subject = new TimeUtil();
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(timeZone);
    }

    @Test
    public void getUpdateTime_shouldReturnTimeInLocalTimeZone() throws Exception {
        String updatedTime = subject.getUpdatedTime("1999-02-22 11:55:14.123");
        assertThat(updatedTime).isEqualTo("22 February 1999 06:55");
    }

    @Test
    public void getFormattedDate_whenGivenDateString_shouldReturnDateInCorrectFormat() throws Exception {
        String formattedDate = subject.getFormattedDate("1999-02-22 00:00:00.000");
        assertThat(formattedDate).isEqualTo("22 FEB");
    }

    @Test
    public void getFormattedDate_whenGivenNull_shouldReturnNull() throws Exception {
        String formattedDate = subject.getFormattedDate(null);
        assertThat(formattedDate).isNull();
    }

    @Test
    public void getFormattedBackupDate_whenGivenADateInMilliseconds_shouldReturnDate(){
        String expectedDate = "13:56 on 12 Nov 2018";
        assertThat(subject.getFormattedBackupDate(1542048970000L)).isEqualTo(expectedDate);
    }

}