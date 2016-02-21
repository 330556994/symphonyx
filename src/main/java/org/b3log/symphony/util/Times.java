/*
 * Copyright (c) 2012-2016, b3log.org & hacpai.com & fangstar.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.symphony.util;

import java.util.Calendar;
import java.util.Date;
import org.b3log.latke.ioc.LatkeBeanManager;
import org.b3log.latke.ioc.LatkeBeanManagerImpl;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.LangPropsServiceImpl;

/**
 * Time utilities.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.3.2.0, Feb 20, 2016
 * @since 1.3.0
 */
public final class Times {

    /**
     * Minute unit.
     */
    private static final long MINUTE_UNIT = 60 * 1000;

    /**
     * Hour unit.
     */
    private static final long HOUR_UNIT = 60 * MINUTE_UNIT;

    /**
     * Day unit.
     */
    private static final long DAY_UNIT = 24 * HOUR_UNIT;

    /**
     * Week unit.
     */
    private static final long WEEK_UNIT = 7 * DAY_UNIT;

    /**
     * Month unit.
     */
    private static final long MONTH_UNIT = 31 * DAY_UNIT;

    /**
     * Year unit.
     */
    private static final long YEAR_UNIT = 12 * MONTH_UNIT;

    /**
     * Gets the display name of the specified week day.
     *
     * @param weekDay the specified week day (1-7)
     * @return display name
     */
    public static String getWeekDayName(final int weekDay) {
        final LatkeBeanManager beanManager = LatkeBeanManagerImpl.getInstance();
        final LangPropsService langService = beanManager.getReference(LangPropsServiceImpl.class);

        switch (weekDay) {
            case 1:
                return langService.get("monLabel");
            case 2:
                return langService.get("tueLabel");
            case 3:
                return langService.get("wenLabel");
            case 4:
                return langService.get("thuLabel");
            case 5:
                return langService.get("friLabel");
            case 6:
                return langService.get("satLabel");
            case 7:
                return langService.get("sunLabel");
            default:
                return langService.get("monLabel");
        }
    }

    /**
     * Gets time ago format text.
     *
     * @param time the specified time.
     * @return time ago format text
     */
    public static String getTimeAgo(final long time) {
        final LatkeBeanManager beanManager = LatkeBeanManagerImpl.getInstance();
        final LangPropsService langService = beanManager.getReference(LangPropsServiceImpl.class);

        final long diff = System.currentTimeMillis() - time;
        long r = 0;

        if (diff > YEAR_UNIT) {
            r = diff / YEAR_UNIT;

            return r + " " + langService.get("yearsAgoLabel");
        }

        if (diff > MONTH_UNIT) {
            r = diff / MONTH_UNIT;

            return r + " " + langService.get("monthsAgoLabel");
        }

        if (diff > WEEK_UNIT) {
            r = diff / WEEK_UNIT;

            return r + " " + langService.get("weeksAgoLabel");
        }

        if (diff > DAY_UNIT) {
            r = diff / DAY_UNIT;

            return r + " " + langService.get("daysAgoLabel");
        }

        if (diff > HOUR_UNIT) {
            r = diff / HOUR_UNIT;

            return r + " " + langService.get("hoursAgoLabel");
        }

        if (diff > MINUTE_UNIT) {
            r = diff / MINUTE_UNIT;

            return r + " " + langService.get("minutesAgoLabel");
        }

        return langService.get("justNowLabel");
    }

    /**
     * Determines whether the specified date1 is the same day with the specified date2.
     *
     * @param date1 the specified date1
     * @param date2 the specified date2
     * @return {@code true} if it is the same day, returns {@code false} otherwise
     */
    public static boolean isSameDay(final Date date1, final Date date2) {
        final Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        final Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) && cal1.get(Calendar.DATE) == cal2.get(Calendar.DATE);
    }

    /**
     * Determines whether the specified date1 is the same week with the specified date2.
     *
     * @param date1 the specified date1
     * @param date2 the specified date2
     * @return {@code true} if it is the same week, returns {@code false} otherwise
     */
    public static boolean isSameWeek(final Date date1, final Date date2) {
        final Calendar cal1 = Calendar.getInstance();
        cal1.setFirstDayOfWeek(Calendar.MONDAY);
        cal1.setTime(date1);
        
        final Calendar cal2 = Calendar.getInstance();
        cal2.setFirstDayOfWeek(Calendar.MONDAY);
        cal2.setTime(date2);

        return cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA)
                && cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR);
    }

    /**
     * Gets the day start time with the specified time.
     *
     * @param time the specified time
     * @return day start time
     */
    public static long getDayStartTime(final long time) {
        final Calendar start = Calendar.getInstance();

        start.setTimeInMillis(time);
        start.set(Calendar.HOUR, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        return start.getTime().getTime();
    }

    /**
     * Gets the day end time with the specified time.
     *
     * @param time the specified time
     * @return day end time
     */
    public static long getDayEndTime(final long time) {
        final Calendar end = Calendar.getInstance();

        end.setTimeInMillis(time);
        end.set(Calendar.HOUR, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        return end.getTime().getTime();
    }

    /**
     * Gets the week day with the specified time.
     * 
     * @param time the specified time
     * @return week day
     */
    public static int getWeekDay(final long time) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int ret = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (ret <= 0) {
            ret = 7;
        }

        return ret;
    }

    /**
     * Gets the week start time with the specified time.
     *
     * @param time the specified time
     * @return week start time
     */
    public static long getWeekStartTime(final long time) {
        final Calendar start = Calendar.getInstance();

        start.setFirstDayOfWeek(Calendar.MONDAY);

        start.setTimeInMillis(time);
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        start.set(Calendar.HOUR, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        return start.getTime().getTime();
    }

    /**
     * Gets the week end time with the specified time.
     *
     * @param time the specified time
     * @return week end time
     */
    public static long getWeekEndTime(final long time) {
        final Calendar end = Calendar.getInstance();

        end.setFirstDayOfWeek(Calendar.MONDAY);

        end.setTimeInMillis(time);
        end.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        end.set(Calendar.HOUR, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        return end.getTime().getTime();
    }

    /**
     * Private constructor.
     */
    private Times() {
    }
}
