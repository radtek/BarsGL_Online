package ru.rbt.barsgl.ejbtest;

//import org.antlr.runtime.RecognitionException;

/**
 * Created by Ivan Sevastyanov on 02.03.2016.
 */
public class CriteriaParserTest {

/*    @Test
    public void testParsefilter() throws RecognitionException, ParseException {
        SomeAcc account1 = new SomeAcc(1, "One", new Date(), "num1");
        SomeAcc account2 = new SomeAcc(2, "Two", new Date(), "num2");
        SomeAcc account3 = new SomeAcc(3, "Three", new Date(), "num3");
        account3.setPreSumDouble(900d);
        SomeAcc account4 = new SomeAcc(4, "Four", null, "num4");
        SomeAcc account5 = new SomeAcc(5, "Пятый", null, "num5");
        account1.setForeign(true);
        SomeAcc account6 = new SomeAcc(6, "ПёЁьЬыЫъЪ", null, "num6");
        account6.setPreSumDouble(123.097787d);
        SomeAcc account7 = new SomeAcc(7, "Новый берег1", null, "num7");
        account7.setPreSumDouble(0.9029d);
        SomeAcc account8 = new SomeAcc(8, "", DateUtils.parseDate("01.01.2011", "dd.MM.yyyy"), "num8");
        account8.setForeign(false);

        List<SomeAcc> orig = Arrays.asList(account1, account2, account3, account4, account5, account6, account7, account8);

        List<SomeAcc> filtered = filter(orig, parseCriteriaString("((    owner =              'One' ) or owner='Three')"), SomeAcc.class);
        org.junit.Assert.assertEquals(2, filtered.size());
        org.junit.Assert.assertTrue(filtered.contains(account1) && filtered.contains(account3));

        filtered = filter(orig, parseCriteriaString("((    owner =              'Пятый' ) or owner='Three')"), SomeAcc.class);
        org.junit.Assert.assertEquals(2, filtered.size());
        org.junit.Assert.assertTrue(filtered.contains(account5) && filtered.contains(account3));

        filtered = filter(orig, parseCriteriaString("((owner = 'ПёЁьЬыЫъЪ' ) or owner='Three')"), SomeAcc.class);
        org.junit.Assert.assertEquals(2, filtered.size());
        org.junit.Assert.assertTrue(filtered.contains(account6) && filtered.contains(account3));

        filtered = filter(orig, parseCriteriaString("(owner like 'Пя%')"), SomeAcc.class);
        org.junit.Assert.assertEquals(1, filtered.size());
        org.junit.Assert.assertTrue(filtered.contains(account5));

        filtered = filter(orig, parseCriteriaString("(owner like '_ят%')"), SomeAcc.class);
        org.junit.Assert.assertEquals(1, filtered.size());
        org.junit.Assert.assertTrue(filtered.contains(account5));

        filtered = filter(orig, parseCriteriaString("owner like 'Новый берег%'"), SomeAcc.class);
        org.junit.Assert.assertEquals(1, filtered.size());
        org.junit.Assert.assertTrue(filtered.contains(account7));

        filtered = filter(orig, parseCriteriaString("dateOpen = [01.01.2011] or     owner =              'Пятый' "), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account8));
        org.junit.Assert.assertTrue(filtered.contains(account5));
        org.junit.Assert.assertEquals(2, filtered.size());

        filtered = filter(orig, parseCriteriaString("dateOpen = [01.01.2011] and     accNum   =              'num8' "), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account8));
        org.junit.Assert.assertEquals(1, filtered.size());

        filtered = filter(orig, parseCriteriaString("id=1i or id=8i"), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account1));
        org.junit.Assert.assertTrue(filtered.contains(account8));
        org.junit.Assert.assertEquals(2, filtered.size());

        filtered = filter(orig, parseCriteriaString("(preSumDouble=123.097787d or preSumDouble=0.9029d) or preSumDouble=900d"), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account6));
        org.junit.Assert.assertTrue(filtered.contains(account7));
        org.junit.Assert.assertTrue(filtered.contains(account3));
        org.junit.Assert.assertEquals(3, filtered.size());

        filtered = filter(orig, parseCriteriaString("foreign = true"), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account1));
        org.junit.Assert.assertEquals(1, filtered.size());

        filtered = filter(orig, parseCriteriaString("foreign = false and id = 8i"), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account8));
        org.junit.Assert.assertEquals(1, filtered.size());

        filtered = filter(orig, parseCriteriaString("foreign = false and id = 8i"), SomeAcc.class);
        org.junit.Assert.assertTrue(filtered.contains(account8));
        org.junit.Assert.assertEquals(1, filtered.size());

    }*/

/*
    private Criteria parseCriteriaString(String criteriaString) throws RecognitionException {
        AntlrCriteriaBuilder criteriaBuilder = new AntlrCriteriaBuilder(criteriaString);
        if (criteriaBuilder.hasErrors()) {
            throw new IllegalArgumentException(criteriaBuilder.getErrorMessage());
        }
        return criteriaBuilder.getResult();
    }
*/
}
