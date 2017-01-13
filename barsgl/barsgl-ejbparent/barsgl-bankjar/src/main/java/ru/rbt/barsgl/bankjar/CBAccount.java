package ru.rbt.barsgl.bankjar;

import org.apache.log4j.Logger;

/**
 * Created by Ivan Sevastyanov
 */
public class CBAccount
{

    public boolean isCorrectControlNumber(String bic, String account)
    {
        String sCode = String.valueOf(bic) + String.valueOf(account);
        int k = 0;
        for(int i = 0; i < sCode.length(); i++)
        {
            String simbol = sCode.substring(i, i + 1);
            int iSimbol = 0;
            try
            {
                iSimbol = Integer.valueOf(simbol).intValue();
            }
            catch(NumberFormatException ex)
            {
                iSimbol = code6.indexOf(simbol);
            }
            k += (coeficient[i % 3] * iSimbol) % 10;
        }

        return k % 10 == 0;
    }

    public String calculateControlNumber(String bic, String account)
    {
        String sCode = String.valueOf(bic) + String.valueOf(account);
        int k = 0;
        for(int i = 0; i < sCode.length(); i++)
        {
            if(i == 11)
                continue;
            String simbol = sCode.substring(i, i + 1);
            int iSimbol = 0;
            try
            {
                iSimbol = Integer.valueOf(simbol).intValue();
            }
            catch(NumberFormatException ex)
            {
                iSimbol = code6.indexOf(simbol);
            }
            k += (coeficient[i % 3] * iSimbol) % 10;
        }

        int controlNumber = ((k % 10) * 3) % 10;
        return String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(account.substring(0, 8))))).append(String.valueOf(controlNumber)).append(account.substring(9))));
    }

    public int getBicControlNumber(String account)
    {
        String sCode = account;
        int k = 0;
        for(int i = 0; i < sCode.length(); i++)
        {
            String simbol = sCode.substring(i, i + 1);
            int iSimbol = 0;
            try
            {
                iSimbol = Integer.valueOf(simbol).intValue();
            }
            catch(NumberFormatException ex)
            {
                iSimbol = code6.indexOf(simbol);
            }
            k += (coeficient[i % 3] * iSimbol) % 10;
        }

        int kBic = (10 - k % 10) % 10;
        return kBic;
    }

    public String calculateControlNumber(int bicControlNumber, String account)
    {
        String sCode = account;
        int k = bicControlNumber;
        for(int i = 0; i < sCode.length(); i++)
        {
            if(i == 8)
                continue;
            String simbol = sCode.substring(i, i + 1);
            int iSimbol = 0;
            try
            {
                iSimbol = Integer.valueOf(simbol).intValue();
            }
            catch(NumberFormatException ex)
            {
                iSimbol = code6.indexOf(simbol);
            }
            k += (coeficient[i % 3] * iSimbol) % 10;
        }

        int controlNumber = ((k % 10) * 3) % 10;
        return String.valueOf(String.valueOf((new StringBuffer(String.valueOf(String.valueOf(account.substring(0, 8))))).append(String.valueOf(controlNumber)).append(account.substring(9))));
    }

    public static void main(String str[])
    {
        try
        {
            CBAccount cb = new CBAccount();
            int bicCRL = cb.getBicControlNumber("70301810400301109275");
            String pairAccount = cb.calculateControlNumber(bicCRL, "70401810400301109275");
            System.out.println(pairAccount);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    Logger logger;
    final String code6 = new String("ABCEHKMPTX");
    static int coeficient[] = {
            7, 1, 3
    };

}
