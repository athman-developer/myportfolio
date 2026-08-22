package com.trewx.qiblah;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public final class PrayerTimes {
    private PrayerTimes() { }

    public static final class DayTimes {
        public final Calendar fajr;
        public final Calendar sunrise;
        public final Calendar dhuhr;
        public final Calendar asr;
        public final Calendar maghrib;
        public final Calendar isha;

        DayTimes(Calendar fajr, Calendar sunrise, Calendar dhuhr, Calendar asr, Calendar maghrib, Calendar isha) {
            this.fajr = fajr;
            this.sunrise = sunrise;
            this.dhuhr = dhuhr;
            this.asr = asr;
            this.maghrib = maghrib;
            this.isha = isha;
        }

        public Map<String, Calendar> obligatory() {
            LinkedHashMap<String, Calendar> map = new LinkedHashMap<>();
            map.put("Fajr", fajr);
            map.put("Dhuhr", dhuhr);
            map.put("Asr", asr);
            map.put("Maghrib", maghrib);
            map.put("Isha", isha);
            return map;
        }
    }

    // Muslim World League: Fajr 18°, Isha 17°. Asr uses the standard (Shafi'i/Maliki/Hanbali) shadow factor 1.
    public static DayTimes calculate(Date date, double latitude, double longitude, TimeZone tz) {
        Calendar base = Calendar.getInstance(tz, Locale.US);
        base.setTime(date);
        base.set(Calendar.HOUR_OF_DAY, 12);
        base.set(Calendar.MINUTE, 0);
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);

        int dayOfYear = base.get(Calendar.DAY_OF_YEAR);
        double daysInYear = base.getActualMaximum(Calendar.DAY_OF_YEAR);
        double gamma = (2.0 * Math.PI / daysInYear) * (dayOfYear - 1);

        double eqTime = 229.18 * (
                0.000075
                        + 0.001868 * Math.cos(gamma)
                        - 0.032077 * Math.sin(gamma)
                        - 0.014615 * Math.cos(2 * gamma)
                        - 0.040849 * Math.sin(2 * gamma));

        double decl = 0.006918
                - 0.399912 * Math.cos(gamma)
                + 0.070257 * Math.sin(gamma)
                - 0.006758 * Math.cos(2 * gamma)
                + 0.000907 * Math.sin(2 * gamma)
                - 0.002697 * Math.cos(3 * gamma)
                + 0.001480 * Math.sin(3 * gamma);

        double zoneHours = tz.getOffset(base.getTimeInMillis()) / 3600000.0;
        double solarNoon = 720.0 - 4.0 * longitude - eqTime + zoneHours * 60.0;

        double fajrMin = solarNoon - hourAngleMinutes(latitude, decl, -18.0);
        double sunriseMin = solarNoon - hourAngleMinutes(latitude, decl, -0.833);
        double dhuhrMin = solarNoon + 1.0;

        double latRad = Math.toRadians(latitude);
        double asrAltitude = Math.toDegrees(Math.atan(1.0 / (1.0 + Math.tan(Math.abs(latRad - decl)))));
        double asrMin = solarNoon + hourAngleMinutes(latitude, decl, asrAltitude);

        double sunsetMin = solarNoon + hourAngleMinutes(latitude, decl, -0.833);
        double ishaMin = solarNoon + hourAngleMinutes(latitude, decl, -17.0);

        return new DayTimes(
                atMinutes(base, fajrMin),
                atMinutes(base, sunriseMin),
                atMinutes(base, dhuhrMin),
                atMinutes(base, asrMin),
                atMinutes(base, sunsetMin),
                atMinutes(base, ishaMin));
    }

    private static double hourAngleMinutes(double latitude, double declinationRad, double altitudeDeg) {
        double lat = Math.toRadians(latitude);
        double altitude = Math.toRadians(altitudeDeg);
        double denominator = Math.cos(lat) * Math.cos(declinationRad);
        if (Math.abs(denominator) < 1e-9) return 0.0;
        double cosH = (Math.sin(altitude) - Math.sin(lat) * Math.sin(declinationRad)) / denominator;
        cosH = Math.max(-1.0, Math.min(1.0, cosH));
        return Math.toDegrees(Math.acos(cosH)) * 4.0;
    }

    private static Calendar atMinutes(Calendar base, double minutesFromMidnight) {
        int dayShift = 0;
        while (minutesFromMidnight < 0) {
            minutesFromMidnight += 1440.0;
            dayShift--;
        }
        while (minutesFromMidnight >= 1440.0) {
            minutesFromMidnight -= 1440.0;
            dayShift++;
        }
        int totalSeconds = (int) Math.round(minutesFromMidnight * 60.0);
        int hour = totalSeconds / 3600;
        int minute = (totalSeconds % 3600) / 60;
        int second = totalSeconds % 60;

        Calendar out = (Calendar) base.clone();
        out.add(Calendar.DAY_OF_MONTH, dayShift);
        out.set(Calendar.HOUR_OF_DAY, hour);
        out.set(Calendar.MINUTE, minute);
        out.set(Calendar.SECOND, second);
        out.set(Calendar.MILLISECOND, 0);
        return out;
    }
}
