package edu.umn.amicus.summary;

import edu.umn.amicus.AnalysisPiece;
import edu.umn.amicus.PreAnnotation;

import java.util.Iterator;
import java.util.List;

/**
 * Created by greg on 2/11/17.
 */
public abstract class MicroSummarizer implements AnalysisPiece {

    // todo: is there a more elegant way to deal with these? It doesn't happen for any other kind of piece...
    protected String[] viewNames;
    protected String[] typeNames;
    protected String[] fieldNames;

    public String[] getViewNames() {
        return viewNames;
    }

    public void setViewNames(String[] viewNames) {
        this.viewNames = viewNames;
    }

    public String[] getTypeNames() {
        return typeNames;
    }

    public void setTypeNames(String[] typeNames) {
        this.typeNames = typeNames;
    }

    public String[] getFieldNames() {
        return fieldNames;
    }

    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

    public String getFileExtension() {
        return "txt";
    }

    public abstract String exportContents(Iterator<List<PreAnnotation>> annotIterator);

}