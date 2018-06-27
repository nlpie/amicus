package edu.umn.amicus.pullers;

import edu.umn.amicus.Amicus;
import edu.umn.amicus.AmicusException;
import edu.umn.amicus.util.AnalysisPiece;
import edu.umn.amicus.uimacomponents.Util;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Transform an annotation, probably of known type, into a non-UIMA output of type T (almost certainly String).
 * Extend this class and override pull(...) to process the Annotation values.
 * Created by gpfinley on 10/20/16.
 */
public class Puller implements AnalysisPiece {

    protected final String[] fieldNames;

    public Puller(String delimitedFieldnames) {
        fieldNames = delimitedFieldnames == null ? null :
                delimitedFieldnames.split(Amicus.ANNOTATION_FIELD_DELIMITER, -1);
    }

    /**
     * Call this from implementing subclasses that don't make use of the field name.
     */
    protected Puller() {
        this.fieldNames = null;
    }

    /**
     * Main method to get information out of an annotation.
     * Unless you have a compelling reason to do otherwise, most implementations of pull() should call this.
     * Will return a List if there are multiple getters.
     * @param annotation
     * @return
     */
    public Object pull(Annotation annotation) throws AmicusException {
        try {
            if (fieldNames.length > 1) {
                List<Object> objectList = new ArrayList<>();
                for (String fieldName : fieldNames) {
                    objectList.add(callThisGetter(fieldName, annotation));
                }
                for (Object obj : objectList) {
                    if (obj != null) {
                        return objectList;
                    }
                }
                return null;
            }
            return callThisGetter(fieldNames[0], annotation);
        } catch (ReflectiveOperationException e) {
            throw new AmicusException(e);
        }
    }

    /**
     * Static version if not using the fieldName class variable.
     * @param fieldName
     * @param annotation
     * @return
     */
    protected Object callThisGetter(String fieldName, Annotation annotation) throws ReflectiveOperationException {
        Class<? extends Annotation> clazz = annotation.getClass();
        Method toCall = clazz.getMethod(Util.getGetterFor(fieldName));
        return toCall.invoke(annotation);
    }

    protected static Class<? extends Annotation> getClassFromName(String name) throws ClassNotFoundException {
        return (Class<? extends Annotation>) Class.forName(name);
    }

}
