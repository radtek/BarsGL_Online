// $ANTLR 3.2 Sep 23, 2009 12:02:23 Criteria.g 2014-08-12 20:46:08


package ru.rbt.barsgl.common.security.antlr;

import org.antlr.runtime.*;
import ru.rbt.barsgl.shared.criteria.Criteria;
import ru.rbt.barsgl.shared.criteria.CriteriaLogic;
import ru.rbt.barsgl.shared.criteria.Criterion;
import ru.rbt.barsgl.shared.criteria.CriterionColumn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class CriteriaParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "AND", "OR", "AtomString", "FIELD", "SIMPLE_OPER", "PARAM_VALUE", "WS", "BRACKETS_LEFT", "BRACKETS_RIGHT", "SINGLE_QUOTES", "'('", "')'"
    };
    public static final int SINGLE_QUOTES=13;
    public static final int WS=10;
    public static final int T__15=15;
    public static final int BRACKETS_RIGHT=12;
    public static final int AtomString=6;
    public static final int T__14=14;
    public static final int OR=5;
    public static final int FIELD=7;
    public static final int SIMPLE_OPER=8;
    public static final int BRACKETS_LEFT=11;
    public static final int AND=4;
    public static final int EOF=-1;
    public static final int PARAM_VALUE=9;

    // delegates
    // delegators


        public CriteriaParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public CriteriaParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        

    public String[] getTokenNames() { return CriteriaParser.tokenNames; }
    public String getGrammarFileName() { return "Criteria.g"; }


        private List<RecognitionException> errors = new ArrayList<>();
        public static final ResourceBundle res = ResourceBundle.getBundle("ru.rbt.barsgl.common.security.antlr.errors");

        @Override
        public void reportError(RecognitionException e) {
            super.reportError(e);
            errors.add(e);
            System.err.println(e);
        }

        public List<RecognitionException> getErrors() {
            return errors;
        }

        public String getErrorMessage() {
            if (errors.isEmpty()) {
                return null;
            } else {
                StringBuilder res = new StringBuilder();
                for (RecognitionException e : errors) {
                    res.append(getErrorHeader(e)).append(" ").append(getErrorMessage(e, getTokenNames())).append("\n");
                }
                return res.toString();
            }
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public String getErrorMessage(RecognitionException e, String[] tokenNames) {
            String msg = e.getMessage();
            if ( e instanceof UnwantedTokenException ) {
                UnwantedTokenException ute = (UnwantedTokenException)e;
                String tokenName="<unknown>";
                if ( ute.expecting== Token.EOF ) {
                    tokenName = getResourceMessage("EOF");
                }
                else {
                    tokenName = tokenNames[ute.expecting];
                }
    //            msg = "extraneous input "+getTokenErrorDisplay(ute.getUnexpectedToken())+
    //                    " " + getResourceMessage("expecting") + " "+tokenName;
                msg = getResourceMessage("extraneous_input") +getTokenErrorDisplay(ute.getUnexpectedToken())+
                        " " + getResourceMessage("expecting") + " "+tokenName;
            }
            else if ( e instanceof MissingTokenException ) {
                MissingTokenException mte = (MissingTokenException)e;
                String tokenName="<unknown>";
                if ( mte.expecting== Token.EOF ) {
                    tokenName = getResourceMessage("EOF");
                }
                else {
                    tokenName = tokenNames[mte.expecting];
                }
                msg = getResourceMessage("missing") + " " +tokenName+ " " + getResourceMessage("at") +" " +getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof MismatchedTokenException ) {
                MismatchedTokenException mte = (MismatchedTokenException)e;
                String tokenName="<unknown>";
                if ( mte.expecting== Token.EOF ) {
                    tokenName = getResourceMessage("EOF");
                }
                else {
                    tokenName = tokenNames[mte.expecting];
                }
                msg = getResourceMessage("mismatched_input") +" " + getTokenErrorDisplay(e.token)+
                        " " + getResourceMessage("expecting") + " "+tokenName;
            }
            else if ( e instanceof MismatchedTreeNodeException ) {
                MismatchedTreeNodeException mtne = (MismatchedTreeNodeException)e;
                String tokenName="<unknown>";
                if ( mtne.expecting==Token.EOF ) {
                    tokenName = getResourceMessage("EOF");
                }
                else {
                    tokenName = tokenNames[mtne.expecting];
                }
                msg = getResourceMessage("mismatched_tree_node") + ": " +mtne.node+
                        " " + getResourceMessage("expecting") + " "+tokenName;
            }
            else if ( e instanceof NoViableAltException ) {
                //NoViableAltException nvae = (NoViableAltException)e;
                // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
                // and "(decision="+nvae.decisionNumber+") and
                // "state "+nvae.stateNumber
                //msg = "no viable alternative at input "+getTokenErrorDisplay(e.token);
                msg = getResourceMessage("no_viable_alternative_at_input") + " " + getTokenErrorDisplay(e.token)  + " " + getResourceMessage("expecting_symb");
            }
            else if ( e instanceof EarlyExitException ) {
                //EarlyExitException eee = (EarlyExitException)e;
                // for development, can add "(decision="+eee.decisionNumber+")"
                msg = "required (...)+ loop did not match anything at input "+
                        getTokenErrorDisplay(e.token);
            }
            else if ( e instanceof MismatchedSetException ) {
                MismatchedSetException mse = (MismatchedSetException)e;
                msg = getResourceMessage("mismatched_input") + " "+getTokenErrorDisplay(e.token)+
                        " " + getResourceMessage("expecting") + " " + mse.expecting;
            }
            else if ( e instanceof MismatchedNotSetException ) {
                MismatchedNotSetException mse = (MismatchedNotSetException)e;
                msg = getResourceMessage("mismatched_input") + getTokenErrorDisplay(e.token)+
                        getResourceMessage("expecting") + mse.expecting;
            }
            else if ( e instanceof FailedPredicateException ) {
                FailedPredicateException fpe = (FailedPredicateException)e;
                msg = "rule "+fpe.ruleName+" failed predicate: {"+
                        fpe.predicateText+"}?";
            }
            return msg;
        }

        private String getResourceMessage(String key) {
            return " " + res.getString(key) + " ";
        }




    // $ANTLR start "buildCriteria"
    // Criteria.g:218:1: buildCriteria returns [Criteria value] : exp= logicExp ;
    public final Criteria buildCriteria() throws RecognitionException {
        Criteria value = null;

        Criteria exp = null;


        try {
            // Criteria.g:219:5: (exp= logicExp )
            // Criteria.g:219:11: exp= logicExp
            {
            pushFollow(FOLLOW_logicExp_in_buildCriteria56);
            exp=logicExp();

            state._fsp--;

            value = exp;

            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return value;
    }
    // $ANTLR end "buildCriteria"


    // $ANTLR start "logicExp"
    // Criteria.g:222:1: logicExp returns [Criteria value] : a1= atomExp ( AND a2= atomExp | OR a2= atomExp )* ;
    public final Criteria logicExp() throws RecognitionException {
        Criteria value = null;

        Criterion a1 = null;

        Criterion a2 = null;


        try {
            // Criteria.g:223:5: (a1= atomExp ( AND a2= atomExp | OR a2= atomExp )* )
            // Criteria.g:223:10: a1= atomExp ( AND a2= atomExp | OR a2= atomExp )*
            {
            pushFollow(FOLLOW_atomExp_in_logicExp84);
            a1=atomExp();

            state._fsp--;

            value =  new Criteria(CriteriaLogic.AND, Arrays.<Criterion>asList(a1));
            // Criteria.g:224:19: ( AND a2= atomExp | OR a2= atomExp )*
            loop1:
            do {
                int alt1=3;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==AND) ) {
                    alt1=1;
                }
                else if ( (LA1_0==OR) ) {
                    alt1=2;
                }


                switch (alt1) {
            	case 1 :
            	    // Criteria.g:224:21: AND a2= atomExp
            	    {
            	    match(input,AND,FOLLOW_AND_in_logicExp114); 
            	    pushFollow(FOLLOW_atomExp_in_logicExp118);
            	    a2=atomExp();

            	    state._fsp--;

            	    value = new Criteria(CriteriaLogic.AND, Arrays.<Criterion>asList(value, a2));

            	    }
            	    break;
            	case 2 :
            	    // Criteria.g:225:21: OR a2= atomExp
            	    {
            	    match(input,OR,FOLLOW_OR_in_logicExp142); 
            	    pushFollow(FOLLOW_atomExp_in_logicExp146);
            	    a2=atomExp();

            	    state._fsp--;

            	    value = new Criteria(CriteriaLogic.OR,  Arrays.<Criterion>asList(value, a2));

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return value;
    }
    // $ANTLR end "logicExp"


    // $ANTLR start "atomExp"
    // Criteria.g:229:1: atomExp returns [Criterion value] : (n= AtomString | '(' exp= logicExp ')' );
    public final Criterion atomExp() throws RecognitionException {
        Criterion value = null;

        Token n=null;
        Criteria exp = null;


        try {
            // Criteria.g:230:5: (n= AtomString | '(' exp= logicExp ')' )
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==AtomString) ) {
                alt2=1;
            }
            else if ( (LA2_0==14) ) {
                alt2=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 2, 0, input);

                throw nvae;
            }
            switch (alt2) {
                case 1 :
                    // Criteria.g:230:10: n= AtomString
                    {
                    n=(Token)match(input,AtomString,FOLLOW_AtomString_in_atomExp195); 
                    value = CriterionColumn.parseCriteria((n!=null?n.getText():null));

                    }
                    break;
                case 2 :
                    // Criteria.g:231:19: '(' exp= logicExp ')'
                    {
                    match(input,14,FOLLOW_14_in_atomExp232); 
                    pushFollow(FOLLOW_logicExp_in_atomExp236);
                    exp=logicExp();

                    state._fsp--;

                    match(input,15,FOLLOW_15_in_atomExp238); 
                    value = exp;

                    }
                    break;

            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
        }
        finally {
        }
        return value;
    }
    // $ANTLR end "atomExp"

    // Delegated rules


 

    public static final BitSet FOLLOW_logicExp_in_buildCriteria56 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atomExp_in_logicExp84 = new BitSet(new long[]{0x0000000000000032L});
    public static final BitSet FOLLOW_AND_in_logicExp114 = new BitSet(new long[]{0x0000000000004040L});
    public static final BitSet FOLLOW_atomExp_in_logicExp118 = new BitSet(new long[]{0x0000000000000032L});
    public static final BitSet FOLLOW_OR_in_logicExp142 = new BitSet(new long[]{0x0000000000004040L});
    public static final BitSet FOLLOW_atomExp_in_logicExp146 = new BitSet(new long[]{0x0000000000000032L});
    public static final BitSet FOLLOW_AtomString_in_atomExp195 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_14_in_atomExp232 = new BitSet(new long[]{0x0000000000004040L});
    public static final BitSet FOLLOW_logicExp_in_atomExp236 = new BitSet(new long[]{0x0000000000008000L});
    public static final BitSet FOLLOW_15_in_atomExp238 = new BitSet(new long[]{0x0000000000000002L});

}