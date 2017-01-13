package ru.rbt.barsgl.common.security.antlr;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import ru.rbt.barsgl.shared.criteria.Criteria;

import java.util.ArrayList;
import java.util.List;

public class AntlrCriteriaBuilder {

    private final Criteria result;
    private String errorMessage;
    private final List<RecognitionException> errors = new ArrayList<>();

    public AntlrCriteriaBuilder(String criteriaString) throws RecognitionException {
        ANTLRStringStream in = new ANTLRStringStream(criteriaString);
        CriteriaLexer lexer = new CriteriaLexer(in);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CriteriaParser parser = new CriteriaParser(tokens);
        result = parser.buildCriteria();
        if (lexer.hasErrors()) {
            errorMessage = lexer.getErrorMessage();
            errors.addAll(lexer.getErrors());
        }
        if (parser.hasErrors()) {
            errorMessage += (lexer.hasErrors() ? "\n" : "")  +parser.getErrorMessage();
            errors.addAll(parser.getErrors());
        }
    }

    public Criteria getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

}
