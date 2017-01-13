grammar Criteria;

@header {

package custis.banking.server.security.antlr;

import custis.banking.core.data.criteria.Criteria;
import custis.banking.core.data.criteria.CriteriaLogic;
import custis.banking.core.data.criteria.Criterion;
import custis.banking.core.data.criteria.CriterionColumn;
import java.util.Arrays;
import java.util.ResourceBundle;

}

@lexer::header{
package custis.banking.server.security.antlr;
import java.util.ResourceBundle;
}

@parser::members {
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

}

@lexer::members {
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

    @Override
    public String getErrorMessage(RecognitionException e, String[] tokenNames) {
        String msg = null;
        if ( e instanceof MismatchedTokenException ) {
            MismatchedTokenException mte = (MismatchedTokenException)e;
            msg = getResourceMessage("mismatched_character") + getCharErrorDisplay(e.c) + getResourceMessage("expecting") + getCharErrorDisplay(mte.expecting);
        }
        else if ( e instanceof NoViableAltException ) {
            NoViableAltException nvae = (NoViableAltException)e;
            // for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
            // and "(decision="+nvae.decisionNumber+") and
            // "state "+nvae.stateNumber
            msg = getResourceMessage("no_viable_alternative_at_character") +getCharErrorDisplay(e.c) + getResourceMessage("expecting_symb");
        }
        else if ( e instanceof EarlyExitException ) {
            EarlyExitException eee = (EarlyExitException)e;
            // for development, can add "(decision="+eee.decisionNumber+")"
            msg = "required (...)+ loop did not match anything at character "+getCharErrorDisplay(e.c);
        }
        else if ( e instanceof MismatchedNotSetException ) {
            MismatchedNotSetException mse = (MismatchedNotSetException)e;
            msg = getResourceMessage("mismatched_character") + getCharErrorDisplay(e.c)
                + getResourceMessage("expecting") + mse.expecting;
        }
        else if ( e instanceof MismatchedSetException ) {
            MismatchedSetException mse = (MismatchedSetException)e;
            msg = getResourceMessage("mismatched_character") + getCharErrorDisplay(e.c)
                + getResourceMessage("expecting") + mse.expecting;
        }
        else if ( e instanceof MismatchedRangeException ) {
            MismatchedRangeException mre = (MismatchedRangeException)e;
            msg = getResourceMessage("mismatched_character") + getCharErrorDisplay(e.c)
                + getResourceMessage("expecting") + getCharErrorDisplay(mre.a)+".."+getCharErrorDisplay(mre.b);
        }
        else {
            msg = super.getErrorMessage(e, tokenNames);
        }
        return msg;
    }

    private String getResourceMessage(String key) {
        return " " + res.getString(key) + " ";
    }

}

buildCriteria returns [Criteria value]
    :     exp=logicExp {$value = $exp.value;}
    ;

logicExp returns [Criteria value]
    :    a1=atomExp       {$value =  new Criteria(CriteriaLogic.AND, Arrays.<Criterion>asList($a1.value));}
                  ( AND a2=atomExp {$value = new Criteria(CriteriaLogic.AND, Arrays.<Criterion>asList($value, $a2.value));}
                  | OR a2=atomExp {$value = new Criteria(CriteriaLogic.OR,  Arrays.<Criterion>asList($value, $a2.value));}
                  )*
    ;

atomExp returns [Criterion value]
    :    n=AtomString                {$value = CriterionColumn.parseCriteria($n.text);}
             |    '(' exp=logicExp ')' {$value = $exp.value;}

    ;

AtomString
    :
        FIELD SIMPLE_OPER PARAM_VALUE
            | FIELD 'like' (' ')* PARAM_VALUE

    ;

/* We're going to ignore all white space characters */
WS
    //:   (' ' | '\t' | '\r'| '\n') {$channel=HIDDEN;}
    :   (' ' | '\t' | '\r'| '\n')+ {skip();}
    ;

AND
    : ' and '
    ;

OR
    : (' ')* ' or ' (' ')*
    ;

FIELD
    :
        (' ')* ('a'..'z' | 'A'..'Z' | '0'..'9')+ (' ')*
    ;

PARAM_VALUE
    :
         // booleans
         ('true' | 'false')
         // numbers
         | ((('0'..'9')+) | (('0'..'9')+ ('.') ('0'..'9')+)) ('i' | 'l' | 'd' | 'f')

         // dates
         | BRACKETS_LEFT ('0'..'3' '0'..'9' '\.' ('0'|'1') '0'..'9' '\.' '1'..'2' '0'..'9' '0'..'9' '0'..'9') BRACKETS_RIGHT

         // strings
         | SINGLE_QUOTES (('a'..'z' | 'A'..'Z' | '0'..'9' | '\u0410'..'\u042F' | '\u0430'..'\u044F' | '\u0451' | '\u0401' | '%' | '_' | '\.')+ (' ')*)+ SINGLE_QUOTES
    ;

SIMPLE_OPER
    :
        (' ')* ('=' | '>' | '<' | '<=' | '>=' | '!=') (' ')*
    ;

SINGLE_QUOTES
    :
        ('\'')
    ;

BRACKETS_LEFT
    :
        '['
    ;

BRACKETS_RIGHT
    :
        ']'
    ;