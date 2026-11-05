package com.example.cvforge.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cvforge.data.model.Application;

import org.junit.Test;

public class ApplicationTest {

    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test
    public void shortlisted_staysInTheAppliedColumn() {
        Application application = new Application();
        application.changeStatus(Application.STATUS_APPLIED, 1000);
        application.changeStatus(Application.STATUS_SHORTLISTED, 2000);
        assertEquals(Application.STATUS_APPLIED, application.column());
    }

    @Test
    public void rejected_staysWhereItWasRejected() {
        Application application = new Application();
        application.changeStatus(Application.STATUS_APPLIED, 1000);
        application.changeStatus(Application.STATUS_INTERVIEW, 2000);
        application.changeStatus(Application.STATUS_REJECTED, 3000);
        assertEquals(Application.STATUS_INTERVIEW, application.column());

        Application early = new Application();
        early.changeStatus(Application.STATUS_APPLIED, 1000);
        early.changeStatus(Application.STATUS_REJECTED, 2000);
        assertEquals(Application.STATUS_APPLIED, early.column());
    }

    @Test
    public void savedInterviewAndOffer_haveTheirOwnColumns() {
        assertEquals(Application.STATUS_SAVED, Application.columnOf(Application.STATUS_SAVED));
        assertEquals(Application.STATUS_INTERVIEW, Application.columnOf(Application.STATUS_INTERVIEW));
        assertEquals(Application.STATUS_OFFER, Application.columnOf(Application.STATUS_OFFER));
    }

    @Test
    public void noReplyForSevenDays_isGhosted() {
        Application application = new Application();
        application.changeStatus(Application.STATUS_APPLIED, 1000);
        assertFalse(application.isGhosted(1000 + 6 * DAY));
        assertTrue(application.isGhosted(1000 + 7 * DAY));

        application.changeStatus(Application.STATUS_SHORTLISTED, 1000 + 8 * DAY);
        assertFalse(application.isGhosted(1000 + 20 * DAY));
    }
}
