package com.wix.restaurants.availability;

//import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class AvailabilityIteratorTest {
	private static final Calendar cal = Calendar.getInstance();

	@Before
	public void setUp() {
		cal.setLenient(false);
		cal.clear();
	}
	
	@Test
	public void testNoExceptions() {
		final List<WeeklyTimeWindow> weekly = Arrays.asList(new WeeklyTimeWindow[] {
				new WeeklyTimeWindow(WeeklyTimeWindow.SUNDAY, WeeklyTimeWindow.MINUTES_IN_WEEK)
		});
		@SuppressWarnings("unchecked")
		final List<DateTimeWindow> exceptions = (List<DateTimeWindow>) Collections.EMPTY_LIST;
		
		final StatusIteratorTester tester = new StatusIteratorTester(
				new AvailabilityIterator(cal, new Availability(weekly, exceptions)));
		
		tester.assertLastStatus(Status.STATUS_AVAILABLE);
		tester.assertDone();
	}
	
	@Test
	public void testRedundantException() {
		cal.set(2010, Calendar.DECEMBER, 13, 0, 0, 0);
		final List<WeeklyTimeWindow> weekly = Arrays.asList(new WeeklyTimeWindow[] {
				new WeeklyTimeWindow(WeeklyTimeWindow.SUNDAY, WeeklyTimeWindow.MINUTES_IN_WEEK)
		});
		final List<DateTimeWindow> exceptions = Arrays.asList(new DateTimeWindow[] {
				when(cal, Calendar.DAY_OF_MONTH, 1, Boolean.TRUE)
		});
		
		cal.add(Calendar.DAY_OF_MONTH, -1);
		final StatusIteratorTester tester = new StatusIteratorTester(
				new AvailabilityIterator(cal, new Availability(weekly, exceptions)));
		
		tester.assertLastStatus(Status.STATUS_AVAILABLE);
		tester.assertDone();
	}
	
	@Test
	public void testNotRedundantException() {
		cal.set(2010, Calendar.DECEMBER, 13, 0, 0, 0);
		final List<WeeklyTimeWindow> weekly = Arrays.asList(new WeeklyTimeWindow[] {
				new WeeklyTimeWindow(WeeklyTimeWindow.SUNDAY, WeeklyTimeWindow.MINUTES_IN_WEEK)
		});
		final List<DateTimeWindow> exceptions = Arrays.asList(new DateTimeWindow[] {
				when(cal, Calendar.DAY_OF_MONTH, 1, Boolean.FALSE)
		});
		
		cal.add(Calendar.DAY_OF_MONTH, -1);
		final StatusIteratorTester tester = new StatusIteratorTester(
				new AvailabilityIterator(cal, new Availability(weekly, exceptions)), cal);
		
		tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertLastStatus(Status.STATUS_AVAILABLE);
		tester.assertDone();
	}
	
	@Test
	public void testWeeklyAndException() {
		final List<WeeklyTimeWindow> weekly = Arrays.asList(new WeeklyTimeWindow[] {
				new WeeklyTimeWindow(WeeklyTimeWindow.SUNDAY, WeeklyTimeWindow.DAY),
				new WeeklyTimeWindow(WeeklyTimeWindow.TUESDAY, 2*WeeklyTimeWindow.DAY)
		});
		
		cal.set(2010, Calendar.DECEMBER, 14, 12, 0, 0);
		final List<DateTimeWindow> exceptions = Arrays.asList(new DateTimeWindow[] {
				when(cal, Calendar.HOUR_OF_DAY, 12, Boolean.FALSE)
		});
		
		cal.set(2010, Calendar.DECEMBER, 10, 0, 0, 0);
		final StatusIteratorTester tester = new StatusIteratorTester(
				new AvailabilityIterator(cal, new Availability(weekly, exceptions)), cal);
		
		tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.DAY_OF_MONTH, 2);
		tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.HOUR_OF_DAY, 12);
		tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.HOUR_OF_DAY, 12);
		tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.DAY_OF_MONTH, 3);
		tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.DAY_OF_MONTH, 1);
		tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.DAY_OF_MONTH, 2);
	}

	@Test
	public void testFullWeeklyAndExceptionUntilForever() {
		cal.set(2010, Calendar.DECEMBER, 13, 0, 0, 0);
		final List<DateTimeWindow> exceptions = Arrays.asList(new DateTimeWindow[] {
				when(cal, Calendar.DAY_OF_MONTH, 1, Boolean.FALSE)
		});
		exceptions.get(0).end = null;

		final StatusIteratorTester tester = new StatusIteratorTester(
				new AvailabilityIterator(cal, new Availability(null, exceptions)), cal);

		tester.assertLastStatus(Status.STATUS_UNAVAILABLE);
		tester.assertDone();
	}

	@Test
	public void testPartialWeeklyAndExceptionUntilForever() {
		cal.set(2010, Calendar.DECEMBER, 13, 0, 0, 0);
		final List<WeeklyTimeWindow> weekly = Arrays.asList(new WeeklyTimeWindow[] {
				new WeeklyTimeWindow(WeeklyTimeWindow.SUNDAY, WeeklyTimeWindow.DAY)
		});
		final List<DateTimeWindow> exceptions = Arrays.asList(new DateTimeWindow[] {
				when(cal, Calendar.DAY_OF_MONTH, 1, Boolean.FALSE)
		});
		exceptions.get(0).end = null;

		final StatusIteratorTester tester = new StatusIteratorTester(
				new AvailabilityIterator(cal, new Availability(weekly, exceptions)), cal);

		tester.assertLastStatus(Status.STATUS_UNAVAILABLE);
		tester.assertDone();
	}

    @Test
    public void testSupportsOverlappingExceptionsByLastOneWinsRule() {
        final Calendar yesterday = (Calendar) cal.clone();
        yesterday.set(2010, Calendar.DECEMBER, 12, 0, 0, 0);

        final Calendar today = (Calendar) cal.clone();
        today.set(2010, Calendar.DECEMBER, 13, 0, 0, 0);

        final Calendar tomorrow = (Calendar) cal.clone();
        tomorrow.set(2010, Calendar.DECEMBER, 14, 0, 0, 0);

        final List<WeeklyTimeWindow> weekly = Arrays.asList(new WeeklyTimeWindow[] {
                new WeeklyTimeWindow(WeeklyTimeWindow.SUNDAY, WeeklyTimeWindow.MINUTES_IN_WEEK)
        });
        final List<DateTimeWindow> exceptions = Arrays.asList(new DateTimeWindow[] {
                when(today, Calendar.DAY_OF_MONTH, 2, Boolean.FALSE),
                when(tomorrow, Calendar.DAY_OF_MONTH, 1, Boolean.TRUE)
        });

        final StatusIteratorTester tester = new StatusIteratorTester(
                new AvailabilityIterator(yesterday, new Availability(weekly, exceptions)), yesterday);

        tester.assertNextStatus(Status.STATUS_AVAILABLE, Calendar.DAY_OF_MONTH, 1);
        tester.assertNextStatus(Status.STATUS_UNAVAILABLE, Calendar.DAY_OF_MONTH, 1);
        tester.assertLastStatus(Status.STATUS_AVAILABLE);
        tester.assertDone();
    }

	private static DateTimeWindow when(Calendar start, int field, int amount, Boolean available) {
		final Calendar end = (Calendar) start.clone();
		end.add(field, amount);
		return new DateTimeWindow(start, end, available);
	}
}
