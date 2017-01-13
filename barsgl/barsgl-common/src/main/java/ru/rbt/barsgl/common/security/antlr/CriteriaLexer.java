// $ANTLR 3.2 Sep 23, 2009 12:02:23 Criteria.g 2014-08-12 20:46:08

package ru.rbt.barsgl.common.security.antlr;

import org.antlr.runtime.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CriteriaLexer extends Lexer {
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



    // delegates
    // delegators

    public CriteriaLexer() {;} 
    public CriteriaLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public CriteriaLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "Criteria.g"; }

    // $ANTLR start "T__14"
    public final void mT__14() throws RecognitionException {
        try {
            int _type = T__14;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:84:7: ( '(' )
            // Criteria.g:84:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__14"

    // $ANTLR start "T__15"
    public final void mT__15() throws RecognitionException {
        try {
            int _type = T__15;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:85:7: ( ')' )
            // Criteria.g:85:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__15"

    // $ANTLR start "AtomString"
    public final void mAtomString() throws RecognitionException {
        try {
            int _type = AtomString;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:236:5: ( FIELD SIMPLE_OPER PARAM_VALUE | FIELD 'like' ( ' ' )* PARAM_VALUE )
            int alt2=2;
            alt2 = dfa2.predict(input);
            switch (alt2) {
                case 1 :
                    // Criteria.g:237:9: FIELD SIMPLE_OPER PARAM_VALUE
                    {
                    mFIELD(); 
                    mSIMPLE_OPER(); 
                    mPARAM_VALUE(); 

                    }
                    break;
                case 2 :
                    // Criteria.g:238:15: FIELD 'like' ( ' ' )* PARAM_VALUE
                    {
                    mFIELD(); 
                    match("like"); 

                    // Criteria.g:238:28: ( ' ' )*
                    loop1:
                    do {
                        int alt1=2;
                        int LA1_0 = input.LA(1);

                        if ( (LA1_0==' ') ) {
                            alt1=1;
                        }


                        switch (alt1) {
                    	case 1 :
                    	    // Criteria.g:238:29: ' '
                    	    {
                    	    match(' '); 

                    	    }
                    	    break;

                    	default :
                    	    break loop1;
                        }
                    } while (true);

                    mPARAM_VALUE(); 

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AtomString"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:245:5: ( ( ' ' | '\\t' | '\\r' | '\\n' )+ )
            // Criteria.g:245:9: ( ' ' | '\\t' | '\\r' | '\\n' )+
            {
            // Criteria.g:245:9: ( ' ' | '\\t' | '\\r' | '\\n' )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='\t' && LA3_0<='\n')||LA3_0=='\r'||LA3_0==' ') ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // Criteria.g:
            	    {
            	    if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);

            skip();

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "AND"
    public final void mAND() throws RecognitionException {
        try {
            int _type = AND;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:249:5: ( ' and ' )
            // Criteria.g:249:7: ' and '
            {
            match(" and "); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "AND"

    // $ANTLR start "OR"
    public final void mOR() throws RecognitionException {
        try {
            int _type = OR;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:253:5: ( ( ' ' )* ' or ' ( ' ' )* )
            // Criteria.g:253:7: ( ' ' )* ' or ' ( ' ' )*
            {
            // Criteria.g:253:7: ( ' ' )*
            loop4:
            do {
                int alt4=2;
                int LA4_0 = input.LA(1);

                if ( (LA4_0==' ') ) {
                    int LA4_1 = input.LA(2);

                    if ( (LA4_1==' ') ) {
                        alt4=1;
                    }


                }


                switch (alt4) {
            	case 1 :
            	    // Criteria.g:253:8: ' '
            	    {
            	    match(' '); 

            	    }
            	    break;

            	default :
            	    break loop4;
                }
            } while (true);

            match(" or "); 

            // Criteria.g:253:21: ( ' ' )*
            loop5:
            do {
                int alt5=2;
                int LA5_0 = input.LA(1);

                if ( (LA5_0==' ') ) {
                    alt5=1;
                }


                switch (alt5) {
            	case 1 :
            	    // Criteria.g:253:22: ' '
            	    {
            	    match(' '); 

            	    }
            	    break;

            	default :
            	    break loop5;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "OR"

    // $ANTLR start "FIELD"
    public final void mFIELD() throws RecognitionException {
        try {
            int _type = FIELD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:257:5: ( ( ' ' )* ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+ ( ' ' )* )
            // Criteria.g:258:9: ( ' ' )* ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+ ( ' ' )*
            {
            // Criteria.g:258:9: ( ' ' )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( (LA6_0==' ') ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // Criteria.g:258:10: ' '
            	    {
            	    match(' '); 

            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            // Criteria.g:258:16: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' )+
            int cnt7=0;
            loop7:
            do {
                int alt7=2;
                int LA7_0 = input.LA(1);

                if ( ((LA7_0>='0' && LA7_0<='9')||(LA7_0>='A' && LA7_0<='Z')||(LA7_0>='a' && LA7_0<='z')) ) {
                    alt7=1;
                }


                switch (alt7) {
            	case 1 :
            	    // Criteria.g:
            	    {
            	    if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt7 >= 1 ) break loop7;
                        EarlyExitException eee =
                            new EarlyExitException(7, input);
                        throw eee;
                }
                cnt7++;
            } while (true);

            // Criteria.g:258:50: ( ' ' )*
            loop8:
            do {
                int alt8=2;
                int LA8_0 = input.LA(1);

                if ( (LA8_0==' ') ) {
                    alt8=1;
                }


                switch (alt8) {
            	case 1 :
            	    // Criteria.g:258:51: ' '
            	    {
            	    match(' '); 

            	    }
            	    break;

            	default :
            	    break loop8;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "FIELD"

    // $ANTLR start "PARAM_VALUE"
    public final void mPARAM_VALUE() throws RecognitionException {
        try {
            int _type = PARAM_VALUE;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:262:5: ( ( 'true' | 'false' ) | ( ( ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+ ) ) ( 'i' | 'l' | 'd' | 'f' ) | BRACKETS_LEFT ( '0' .. '3' '0' .. '9' '\\.' ( '0' | '1' ) '0' .. '9' '\\.' '1' .. '2' '0' .. '9' '0' .. '9' '0' .. '9' ) BRACKETS_RIGHT | SINGLE_QUOTES ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '\\u0410' .. '\\u042F' | '\\u0430' .. '\\u044F' | '\\u0451' | '\\u0401' | '%' | '_' | '\\.' )+ ( ' ' )* )+ SINGLE_QUOTES )
            int alt17=4;
            switch ( input.LA(1) ) {
            case 'f':
            case 't':
                {
                alt17=1;
                }
                break;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                {
                alt17=2;
                }
                break;
            case '[':
                {
                alt17=3;
                }
                break;
            case '\'':
                {
                alt17=4;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 17, 0, input);

                throw nvae;
            }

            switch (alt17) {
                case 1 :
                    // Criteria.g:264:10: ( 'true' | 'false' )
                    {
                    // Criteria.g:264:10: ( 'true' | 'false' )
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0=='t') ) {
                        alt9=1;
                    }
                    else if ( (LA9_0=='f') ) {
                        alt9=2;
                    }
                    else {
                        NoViableAltException nvae =
                            new NoViableAltException("", 9, 0, input);

                        throw nvae;
                    }
                    switch (alt9) {
                        case 1 :
                            // Criteria.g:264:11: 'true'
                            {
                            match("true"); 


                            }
                            break;
                        case 2 :
                            // Criteria.g:264:20: 'false'
                            {
                            match("false"); 


                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // Criteria.g:266:12: ( ( ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+ ) ) ( 'i' | 'l' | 'd' | 'f' )
                    {
                    // Criteria.g:266:12: ( ( ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+ ) )
                    int alt13=2;
                    alt13 = dfa13.predict(input);
                    switch (alt13) {
                        case 1 :
                            // Criteria.g:266:13: ( ( '0' .. '9' )+ )
                            {
                            // Criteria.g:266:13: ( ( '0' .. '9' )+ )
                            // Criteria.g:266:14: ( '0' .. '9' )+
                            {
                            // Criteria.g:266:14: ( '0' .. '9' )+
                            int cnt10=0;
                            loop10:
                            do {
                                int alt10=2;
                                int LA10_0 = input.LA(1);

                                if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                                    alt10=1;
                                }


                                switch (alt10) {
                            	case 1 :
                            	    // Criteria.g:266:15: '0' .. '9'
                            	    {
                            	    matchRange('0','9'); 

                            	    }
                            	    break;

                            	default :
                            	    if ( cnt10 >= 1 ) break loop10;
                                        EarlyExitException eee =
                                            new EarlyExitException(10, input);
                                        throw eee;
                                }
                                cnt10++;
                            } while (true);


                            }


                            }
                            break;
                        case 2 :
                            // Criteria.g:266:29: ( ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+ )
                            {
                            // Criteria.g:266:29: ( ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+ )
                            // Criteria.g:266:30: ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+
                            {
                            // Criteria.g:266:30: ( '0' .. '9' )+
                            int cnt11=0;
                            loop11:
                            do {
                                int alt11=2;
                                int LA11_0 = input.LA(1);

                                if ( ((LA11_0>='0' && LA11_0<='9')) ) {
                                    alt11=1;
                                }


                                switch (alt11) {
                            	case 1 :
                            	    // Criteria.g:266:31: '0' .. '9'
                            	    {
                            	    matchRange('0','9'); 

                            	    }
                            	    break;

                            	default :
                            	    if ( cnt11 >= 1 ) break loop11;
                                        EarlyExitException eee =
                                            new EarlyExitException(11, input);
                                        throw eee;
                                }
                                cnt11++;
                            } while (true);

                            // Criteria.g:266:42: ( '.' )
                            // Criteria.g:266:43: '.'
                            {
                            match('.'); 

                            }

                            // Criteria.g:266:48: ( '0' .. '9' )+
                            int cnt12=0;
                            loop12:
                            do {
                                int alt12=2;
                                int LA12_0 = input.LA(1);

                                if ( ((LA12_0>='0' && LA12_0<='9')) ) {
                                    alt12=1;
                                }


                                switch (alt12) {
                            	case 1 :
                            	    // Criteria.g:266:49: '0' .. '9'
                            	    {
                            	    matchRange('0','9'); 

                            	    }
                            	    break;

                            	default :
                            	    if ( cnt12 >= 1 ) break loop12;
                                        EarlyExitException eee =
                                            new EarlyExitException(12, input);
                                        throw eee;
                                }
                                cnt12++;
                            } while (true);


                            }


                            }
                            break;

                    }

                    if ( input.LA(1)=='d'||input.LA(1)=='f'||input.LA(1)=='i'||input.LA(1)=='l' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;
                case 3 :
                    // Criteria.g:269:12: BRACKETS_LEFT ( '0' .. '3' '0' .. '9' '\\.' ( '0' | '1' ) '0' .. '9' '\\.' '1' .. '2' '0' .. '9' '0' .. '9' '0' .. '9' ) BRACKETS_RIGHT
                    {
                    mBRACKETS_LEFT(); 
                    // Criteria.g:269:26: ( '0' .. '3' '0' .. '9' '\\.' ( '0' | '1' ) '0' .. '9' '\\.' '1' .. '2' '0' .. '9' '0' .. '9' '0' .. '9' )
                    // Criteria.g:269:27: '0' .. '3' '0' .. '9' '\\.' ( '0' | '1' ) '0' .. '9' '\\.' '1' .. '2' '0' .. '9' '0' .. '9' '0' .. '9'
                    {
                    matchRange('0','3'); 
                    matchRange('0','9'); 
                    match('.'); 
                    if ( (input.LA(1)>='0' && input.LA(1)<='1') ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}

                    matchRange('0','9'); 
                    match('.'); 
                    matchRange('1','2'); 
                    matchRange('0','9'); 
                    matchRange('0','9'); 
                    matchRange('0','9'); 

                    }

                    mBRACKETS_RIGHT(); 

                    }
                    break;
                case 4 :
                    // Criteria.g:272:12: SINGLE_QUOTES ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '\\u0410' .. '\\u042F' | '\\u0430' .. '\\u044F' | '\\u0451' | '\\u0401' | '%' | '_' | '\\.' )+ ( ' ' )* )+ SINGLE_QUOTES
                    {
                    mSINGLE_QUOTES(); 
                    // Criteria.g:272:26: ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '\\u0410' .. '\\u042F' | '\\u0430' .. '\\u044F' | '\\u0451' | '\\u0401' | '%' | '_' | '\\.' )+ ( ' ' )* )+
                    int cnt16=0;
                    loop16:
                    do {
                        int alt16=2;
                        int LA16_0 = input.LA(1);

                        if ( (LA16_0=='%'||LA16_0=='.'||(LA16_0>='0' && LA16_0<='9')||(LA16_0>='A' && LA16_0<='Z')||LA16_0=='_'||(LA16_0>='a' && LA16_0<='z')||LA16_0=='\u0401'||(LA16_0>='\u0410' && LA16_0<='\u044F')||LA16_0=='\u0451') ) {
                            alt16=1;
                        }


                        switch (alt16) {
                    	case 1 :
                    	    // Criteria.g:272:27: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '\\u0410' .. '\\u042F' | '\\u0430' .. '\\u044F' | '\\u0451' | '\\u0401' | '%' | '_' | '\\.' )+ ( ' ' )*
                    	    {
                    	    // Criteria.g:272:27: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '\\u0410' .. '\\u042F' | '\\u0430' .. '\\u044F' | '\\u0451' | '\\u0401' | '%' | '_' | '\\.' )+
                    	    int cnt14=0;
                    	    loop14:
                    	    do {
                    	        int alt14=2;
                    	        int LA14_0 = input.LA(1);

                    	        if ( (LA14_0=='%'||LA14_0=='.'||(LA14_0>='0' && LA14_0<='9')||(LA14_0>='A' && LA14_0<='Z')||LA14_0=='_'||(LA14_0>='a' && LA14_0<='z')||LA14_0=='\u0401'||(LA14_0>='\u0410' && LA14_0<='\u044F')||LA14_0=='\u0451') ) {
                    	            alt14=1;
                    	        }


                    	        switch (alt14) {
                    	    	case 1 :
                    	    	    // Criteria.g:
                    	    	    {
                    	    	    if ( input.LA(1)=='%'||input.LA(1)=='.'||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z')||input.LA(1)=='\u0401'||(input.LA(1)>='\u0410' && input.LA(1)<='\u044F')||input.LA(1)=='\u0451' ) {
                    	    	        input.consume();

                    	    	    }
                    	    	    else {
                    	    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	    	        recover(mse);
                    	    	        throw mse;}


                    	    	    }
                    	    	    break;

                    	    	default :
                    	    	    if ( cnt14 >= 1 ) break loop14;
                    	                EarlyExitException eee =
                    	                    new EarlyExitException(14, input);
                    	                throw eee;
                    	        }
                    	        cnt14++;
                    	    } while (true);

                    	    // Criteria.g:272:144: ( ' ' )*
                    	    loop15:
                    	    do {
                    	        int alt15=2;
                    	        int LA15_0 = input.LA(1);

                    	        if ( (LA15_0==' ') ) {
                    	            alt15=1;
                    	        }


                    	        switch (alt15) {
                    	    	case 1 :
                    	    	    // Criteria.g:272:145: ' '
                    	    	    {
                    	    	    match(' '); 

                    	    	    }
                    	    	    break;

                    	    	default :
                    	    	    break loop15;
                    	        }
                    	    } while (true);


                    	    }
                    	    break;

                    	default :
                    	    if ( cnt16 >= 1 ) break loop16;
                                EarlyExitException eee =
                                    new EarlyExitException(16, input);
                                throw eee;
                        }
                        cnt16++;
                    } while (true);

                    mSINGLE_QUOTES(); 

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PARAM_VALUE"

    // $ANTLR start "SIMPLE_OPER"
    public final void mSIMPLE_OPER() throws RecognitionException {
        try {
            int _type = SIMPLE_OPER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:276:5: ( ( ' ' )* ( '=' | '>' | '<' | '<=' | '>=' | '!=' ) ( ' ' )* )
            // Criteria.g:277:9: ( ' ' )* ( '=' | '>' | '<' | '<=' | '>=' | '!=' ) ( ' ' )*
            {
            // Criteria.g:277:9: ( ' ' )*
            loop18:
            do {
                int alt18=2;
                int LA18_0 = input.LA(1);

                if ( (LA18_0==' ') ) {
                    alt18=1;
                }


                switch (alt18) {
            	case 1 :
            	    // Criteria.g:277:10: ' '
            	    {
            	    match(' '); 

            	    }
            	    break;

            	default :
            	    break loop18;
                }
            } while (true);

            // Criteria.g:277:16: ( '=' | '>' | '<' | '<=' | '>=' | '!=' )
            int alt19=6;
            switch ( input.LA(1) ) {
            case '=':
                {
                alt19=1;
                }
                break;
            case '>':
                {
                int LA19_2 = input.LA(2);

                if ( (LA19_2=='=') ) {
                    alt19=5;
                }
                else {
                    alt19=2;}
                }
                break;
            case '<':
                {
                int LA19_3 = input.LA(2);

                if ( (LA19_3=='=') ) {
                    alt19=4;
                }
                else {
                    alt19=3;}
                }
                break;
            case '!':
                {
                alt19=6;
                }
                break;
            default:
                NoViableAltException nvae =
                    new NoViableAltException("", 19, 0, input);

                throw nvae;
            }

            switch (alt19) {
                case 1 :
                    // Criteria.g:277:17: '='
                    {
                    match('='); 

                    }
                    break;
                case 2 :
                    // Criteria.g:277:23: '>'
                    {
                    match('>'); 

                    }
                    break;
                case 3 :
                    // Criteria.g:277:29: '<'
                    {
                    match('<'); 

                    }
                    break;
                case 4 :
                    // Criteria.g:277:35: '<='
                    {
                    match("<="); 


                    }
                    break;
                case 5 :
                    // Criteria.g:277:42: '>='
                    {
                    match(">="); 


                    }
                    break;
                case 6 :
                    // Criteria.g:277:49: '!='
                    {
                    match("!="); 


                    }
                    break;

            }

            // Criteria.g:277:55: ( ' ' )*
            loop20:
            do {
                int alt20=2;
                int LA20_0 = input.LA(1);

                if ( (LA20_0==' ') ) {
                    alt20=1;
                }


                switch (alt20) {
            	case 1 :
            	    // Criteria.g:277:56: ' '
            	    {
            	    match(' '); 

            	    }
            	    break;

            	default :
            	    break loop20;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SIMPLE_OPER"

    // $ANTLR start "SINGLE_QUOTES"
    public final void mSINGLE_QUOTES() throws RecognitionException {
        try {
            int _type = SINGLE_QUOTES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:281:5: ( ( '\\'' ) )
            // Criteria.g:282:9: ( '\\'' )
            {
            // Criteria.g:282:9: ( '\\'' )
            // Criteria.g:282:10: '\\''
            {
            match('\''); 

            }


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SINGLE_QUOTES"

    // $ANTLR start "BRACKETS_LEFT"
    public final void mBRACKETS_LEFT() throws RecognitionException {
        try {
            int _type = BRACKETS_LEFT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:286:5: ( '[' )
            // Criteria.g:287:9: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BRACKETS_LEFT"

    // $ANTLR start "BRACKETS_RIGHT"
    public final void mBRACKETS_RIGHT() throws RecognitionException {
        try {
            int _type = BRACKETS_RIGHT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // Criteria.g:291:5: ( ']' )
            // Criteria.g:292:9: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "BRACKETS_RIGHT"

    public void mTokens() throws RecognitionException {
        // Criteria.g:1:8: ( T__14 | T__15 | AtomString | WS | AND | OR | FIELD | PARAM_VALUE | SIMPLE_OPER | SINGLE_QUOTES | BRACKETS_LEFT | BRACKETS_RIGHT )
        int alt21=12;
        alt21 = dfa21.predict(input);
        switch (alt21) {
            case 1 :
                // Criteria.g:1:10: T__14
                {
                mT__14(); 

                }
                break;
            case 2 :
                // Criteria.g:1:16: T__15
                {
                mT__15(); 

                }
                break;
            case 3 :
                // Criteria.g:1:22: AtomString
                {
                mAtomString(); 

                }
                break;
            case 4 :
                // Criteria.g:1:33: WS
                {
                mWS(); 

                }
                break;
            case 5 :
                // Criteria.g:1:36: AND
                {
                mAND(); 

                }
                break;
            case 6 :
                // Criteria.g:1:40: OR
                {
                mOR(); 

                }
                break;
            case 7 :
                // Criteria.g:1:43: FIELD
                {
                mFIELD(); 

                }
                break;
            case 8 :
                // Criteria.g:1:49: PARAM_VALUE
                {
                mPARAM_VALUE(); 

                }
                break;
            case 9 :
                // Criteria.g:1:61: SIMPLE_OPER
                {
                mSIMPLE_OPER(); 

                }
                break;
            case 10 :
                // Criteria.g:1:73: SINGLE_QUOTES
                {
                mSINGLE_QUOTES(); 

                }
                break;
            case 11 :
                // Criteria.g:1:87: BRACKETS_LEFT
                {
                mBRACKETS_LEFT(); 

                }
                break;
            case 12 :
                // Criteria.g:1:101: BRACKETS_RIGHT
                {
                mBRACKETS_RIGHT(); 

                }
                break;

        }

    }


    protected DFA2 dfa2 = new DFA2(this);
    protected DFA13 dfa13 = new DFA13(this);
    protected DFA21 dfa21 = new DFA21(this);
    static final String DFA2_eotS =
        "\20\uffff\2\6\2\uffff\1\6\1\uffff\1\6";
    static final String DFA2_eofS =
        "\27\uffff";
    static final String DFA2_minS =
        "\5\40\2\uffff\20\40";
    static final String DFA2_maxS =
        "\3\172\1\154\1\172\2\uffff\3\172\1\164\14\172";
    static final String DFA2_acceptS =
        "\5\uffff\1\1\1\2\20\uffff";
    static final String DFA2_specialS =
        "\27\uffff}>";
    static final String[] DFA2_transitionS = {
            "\1\1\17\uffff\12\2\7\uffff\32\2\6\uffff\32\2",
            "\1\1\17\uffff\12\2\7\uffff\32\2\6\uffff\32\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\4\16\2",
            "\1\3\1\5\32\uffff\3\5\55\uffff\1\6",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\10"+
            "\2\1\7\2\2\1\4\16\2",
            "",
            "",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\12"+
            "\2\1\10\1\4\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\4"+
            "\2\1\11\6\2\1\4\16\2",
            "\1\12\1\5\5\uffff\1\6\10\uffff\12\15\2\uffff\3\5\2\uffff\32"+
            "\2\1\6\5\uffff\5\2\1\14\5\2\1\4\7\2\1\13\6\2",
            "\1\12\1\5\5\uffff\1\6\10\uffff\12\6\2\uffff\3\5\34\uffff\1"+
            "\6\12\uffff\1\6\5\uffff\1\6\7\uffff\1\6",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\4\5\2\1\16\10\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\1"+
            "\17\12\2\1\4\16\2",
            "\1\3\1\5\14\uffff\1\6\1\uffff\12\15\2\uffff\3\5\2\uffff\32"+
            "\2\6\uffff\3\2\1\21\1\2\1\21\2\2\1\21\2\2\1\20\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\4\10\2\1\22\5\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\23\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\10"+
            "\2\1\7\2\2\1\4\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\4\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\4"+
            "\2\1\24\6\2\1\4\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\10"+
            "\2\1\7\2\2\1\4\6\2\1\25\7\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\4\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\4"+
            "\2\1\26\6\2\1\4\16\2",
            "\1\3\1\5\16\uffff\12\2\2\uffff\3\5\2\uffff\32\2\6\uffff\13"+
            "\2\1\4\16\2"
    };

    static final short[] DFA2_eot = DFA.unpackEncodedString(DFA2_eotS);
    static final short[] DFA2_eof = DFA.unpackEncodedString(DFA2_eofS);
    static final char[] DFA2_min = DFA.unpackEncodedStringToUnsignedChars(DFA2_minS);
    static final char[] DFA2_max = DFA.unpackEncodedStringToUnsignedChars(DFA2_maxS);
    static final short[] DFA2_accept = DFA.unpackEncodedString(DFA2_acceptS);
    static final short[] DFA2_special = DFA.unpackEncodedString(DFA2_specialS);
    static final short[][] DFA2_transition;

    static {
        int numStates = DFA2_transitionS.length;
        DFA2_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA2_transition[i] = DFA.unpackEncodedString(DFA2_transitionS[i]);
        }
    }

    class DFA2 extends DFA {

        public DFA2(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 2;
            this.eot = DFA2_eot;
            this.eof = DFA2_eof;
            this.min = DFA2_min;
            this.max = DFA2_max;
            this.accept = DFA2_accept;
            this.special = DFA2_special;
            this.transition = DFA2_transition;
        }
        public String getDescription() {
            return "235:1: AtomString : ( FIELD SIMPLE_OPER PARAM_VALUE | FIELD 'like' ( ' ' )* PARAM_VALUE );";
        }
    }
    static final String DFA13_eotS =
        "\4\uffff";
    static final String DFA13_eofS =
        "\4\uffff";
    static final String DFA13_minS =
        "\1\60\1\56\2\uffff";
    static final String DFA13_maxS =
        "\1\71\1\154\2\uffff";
    static final String DFA13_acceptS =
        "\2\uffff\1\2\1\1";
    static final String DFA13_specialS =
        "\4\uffff}>";
    static final String[] DFA13_transitionS = {
            "\12\1",
            "\1\2\1\uffff\12\1\52\uffff\1\3\1\uffff\1\3\2\uffff\1\3\2\uffff"+
            "\1\3",
            "",
            ""
    };

    static final short[] DFA13_eot = DFA.unpackEncodedString(DFA13_eotS);
    static final short[] DFA13_eof = DFA.unpackEncodedString(DFA13_eofS);
    static final char[] DFA13_min = DFA.unpackEncodedStringToUnsignedChars(DFA13_minS);
    static final char[] DFA13_max = DFA.unpackEncodedStringToUnsignedChars(DFA13_maxS);
    static final short[] DFA13_accept = DFA.unpackEncodedString(DFA13_acceptS);
    static final short[] DFA13_special = DFA.unpackEncodedString(DFA13_specialS);
    static final short[][] DFA13_transition;

    static {
        int numStates = DFA13_transitionS.length;
        DFA13_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA13_transition[i] = DFA.unpackEncodedString(DFA13_transitionS[i]);
        }
    }

    class DFA13 extends DFA {

        public DFA13(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 13;
            this.eot = DFA13_eot;
            this.eof = DFA13_eof;
            this.min = DFA13_min;
            this.max = DFA13_max;
            this.accept = DFA13_accept;
            this.special = DFA13_special;
            this.transition = DFA13_transition;
        }
        public String getDescription() {
            return "266:12: ( ( ( '0' .. '9' )+ ) | ( ( '0' .. '9' )+ ( '.' ) ( '0' .. '9' )+ ) )";
        }
    }
    static final String DFA21_eotS =
        "\3\uffff\1\5\1\22\1\uffff\3\22\1\31\1\32\2\uffff\2\22\1\5\2\22"+
        "\2\uffff\4\22\3\uffff\6\22\1\47\3\22\1\52\1\47\1\uffff\2\22\1\uffff"+
        "\6\22\2\23\2\22\1\23\1\22\1\23";
    static final String DFA21_eofS =
        "\70\uffff";
    static final String DFA21_minS =
        "\1\11\2\uffff\2\40\1\uffff\3\40\1\60\1\45\2\uffff\5\40\2\uffff"+
        "\4\40\3\uffff\14\40\1\uffff\2\40\1\uffff\15\40";
    static final String DFA21_maxS =
        "\1\172\2\uffff\2\172\1\uffff\3\172\1\63\1\u0451\2\uffff\4\172\1"+
        "\154\2\uffff\4\172\3\uffff\6\172\1\154\3\172\2\154\1\uffff\2\172"+
        "\1\uffff\1\164\14\172";
    static final String DFA21_acceptS =
        "\1\uffff\1\1\1\2\2\uffff\1\4\5\uffff\1\11\1\14\5\uffff\1\7\1\3"+
        "\4\uffff\1\10\1\13\1\12\14\uffff\1\6\2\uffff\1\5\15\uffff";
    static final String DFA21_specialS =
        "\70\uffff}>";
    static final String[] DFA21_transitionS = {
            "\2\5\2\uffff\1\5\22\uffff\1\3\1\13\5\uffff\1\12\1\1\1\2\6\uffff"+
            "\12\7\2\uffff\3\13\2\uffff\32\10\1\11\1\uffff\1\14\3\uffff\5"+
            "\10\1\6\15\10\1\4\6\10",
            "",
            "",
            "\1\17\1\13\16\uffff\12\10\2\uffff\3\13\2\uffff\32\10\6\uffff"+
            "\1\15\15\10\1\16\13\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\5\10\1\20\10\10",
            "",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\1\25\12\10\1\24\16\10",
            "\1\21\1\23\14\uffff\1\30\1\uffff\12\7\2\uffff\3\23\2\uffff"+
            "\32\10\6\uffff\3\10\1\27\1\10\1\27\2\10\1\27\2\10\1\26\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\16\10",
            "\4\30",
            "\1\30\10\uffff\1\30\1\uffff\12\30\7\uffff\32\30\4\uffff\1"+
            "\30\1\uffff\32\30\u0386\uffff\1\30\16\uffff\100\30\1\uffff\1"+
            "\30",
            "",
            "",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\1\10\1\33\14\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\5\10\1\34\10\10",
            "\1\17\1\13\16\uffff\12\10\2\uffff\3\13\2\uffff\32\10\6\uffff"+
            "\16\10\1\16\13\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\10\10\1\35\5\10",
            "\1\21\1\23\32\uffff\3\23\55\uffff\1\23",
            "",
            "",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\10\10\1\36\2\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\37\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\10\10\1\36\2\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\16\10",
            "",
            "",
            "",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\3\10\1\40\7\10\1\24\16\10",
            "\1\41\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\4\10\1\42\6\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\12\10\1\43\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\10\10\1\36\2\10\1\24\6\10\1\44\7\10",
            "\1\45\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\16\10",
            "\1\46\1\23\32\uffff\3\23\55\uffff\1\23",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\4\10\1\50\6\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\4\10\1\51\6\10\1\24\16\10",
            "\1\21\1\23\32\uffff\3\23\55\uffff\1\23",
            "\1\46\1\23\32\uffff\3\23\55\uffff\1\23",
            "",
            "\1\53\1\23\5\uffff\1\23\10\uffff\12\56\2\uffff\3\23\2\uffff"+
            "\32\10\1\23\5\uffff\5\10\1\55\5\10\1\24\7\10\1\54\6\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\16\10",
            "",
            "\1\53\1\23\5\uffff\1\23\10\uffff\12\23\2\uffff\3\23\34\uffff"+
            "\1\23\12\uffff\1\23\5\uffff\1\23\7\uffff\1\23",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\5\10\1\57\10\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\1\60\12\10\1\24\16\10",
            "\1\21\1\23\14\uffff\1\23\1\uffff\12\56\2\uffff\3\23\2\uffff"+
            "\32\10\6\uffff\3\10\1\62\1\10\1\62\2\10\1\62\2\10\1\61\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\24\10\10\1\63\5\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\13\10\1\64\16\10",
            "\1\21\17\uffff\12\10\7\uffff\32\10\6\uffff\10\10\1\36\2\10"+
            "\1\24\16\10",
            "\1\21\17\uffff\12\10\7\uffff\32\10\6\uffff\13\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\4\10\1\65\6\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\10\10\1\36\2\10\1\24\6\10\1\66\7\10",
            "\1\21\17\uffff\12\10\7\uffff\32\10\6\uffff\13\10\1\24\16\10",
            "\1\21\1\23\16\uffff\12\10\2\uffff\3\23\2\uffff\32\10\6\uffff"+
            "\4\10\1\67\6\10\1\24\16\10",
            "\1\21\17\uffff\12\10\7\uffff\32\10\6\uffff\13\10\1\24\16\10"
    };

    static final short[] DFA21_eot = DFA.unpackEncodedString(DFA21_eotS);
    static final short[] DFA21_eof = DFA.unpackEncodedString(DFA21_eofS);
    static final char[] DFA21_min = DFA.unpackEncodedStringToUnsignedChars(DFA21_minS);
    static final char[] DFA21_max = DFA.unpackEncodedStringToUnsignedChars(DFA21_maxS);
    static final short[] DFA21_accept = DFA.unpackEncodedString(DFA21_acceptS);
    static final short[] DFA21_special = DFA.unpackEncodedString(DFA21_specialS);
    static final short[][] DFA21_transition;

    static {
        int numStates = DFA21_transitionS.length;
        DFA21_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA21_transition[i] = DFA.unpackEncodedString(DFA21_transitionS[i]);
        }
    }

    class DFA21 extends DFA {

        public DFA21(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 21;
            this.eot = DFA21_eot;
            this.eof = DFA21_eof;
            this.min = DFA21_min;
            this.max = DFA21_max;
            this.accept = DFA21_accept;
            this.special = DFA21_special;
            this.transition = DFA21_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__14 | T__15 | AtomString | WS | AND | OR | FIELD | PARAM_VALUE | SIMPLE_OPER | SINGLE_QUOTES | BRACKETS_LEFT | BRACKETS_RIGHT );";
        }
    }
 

}