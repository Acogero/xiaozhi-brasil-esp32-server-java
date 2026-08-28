package com.xiaozhi.utils;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    public static String dayOfMonthStart() {
        // Início do mês atual
        Calendar thisMonthFirstDateCal = Calendar.getInstance();
        // Obter mês anterior
        // thisMonthFirstDateCal.add(Calendar.MONTH, -1);
        thisMonthFirstDateCal.set(Calendar.DAY_OF_MONTH, thisMonthFirstDateCal.getActualMinimum(Calendar.DAY_OF_MONTH));
        String thisMonthFirstTime = format.format(thisMonthFirstDateCal.getTime()) + " 00:00:00";
        return thisMonthFirstTime;
    }

    public static String dayOfMonthEnd() {
        Calendar thisMonthEndDateCal = Calendar.getInstance();
        // Obter mês anterior
        // thisMonthEndDateCal.add(Calendar.MONTH, -1);
        thisMonthEndDateCal.set(Calendar.DAY_OF_MONTH, thisMonthEndDateCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        String thisMonthEndTime = format.format(thisMonthEndDateCal.getTime()) + " 23:59:59";
        return thisMonthEndTime;
    }

    /**
     * Calcula a diferença de tempo e retorna em segundos, com precisão de três casas decimais
     *
     * @param startTime tempo inicial (milissegundos)
     * @param endTime   tempo final (milissegundos)
     * @return diferença de tempo (segundos), com precisão de três casas decimais
     */
    public static Double deltaTime(long startTime, long endTime) {
        double deltaTime = (endTime - startTime) / 1000.0; // Converte milissegundos para segundos
        DecimalFormat decimalFormat = new DecimalFormat("0.###"); // Mantém 3 casas decimais
        String formattedTime = decimalFormat.format(deltaTime); // Formata como string
        return Double.parseDouble(formattedTime); // Converte para Double
    }

    /**
     * Converte uma string em um objeto de data
     * @param date
     * @param strFormat
     * @return
     */
    public static Date toDate(String date, String strFormat) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(strFormat);
            df.setLenient(false);
            Date objDate = df.parse(date);
            return objDate;
        } catch (Exception var4) {
            return null;
        }
    }
}