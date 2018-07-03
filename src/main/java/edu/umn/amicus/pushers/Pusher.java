package edu.umn.amicus.pushers;

import edu.umn.amicus.util.ANA;
import edu.umn.amicus.Amicus;
import edu.umn.amicus.AmicusException;
import edu.umn.amicus.util.AnalysisPiece;
import edu.umn.amicus.uimacomponents.Util;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Basic class for creating annotations on JCas objects.
 * Can work with lists of objects or single objects and with single or multiple fields on the annotation.
 * (But in the case of a lists of objects AND fields, they should be the same length.
 *      Nulls are allowable in the lists and will be skipped.)
 *
 * Created by gpfinley on 1/20/17.
 */
public class Pusher implements AnalysisPiece {

    protected String typeName;
    protected List<String> fieldNames;

    protected Constructor<? extends Annotation> annotationConstructor;
    protected List<Method> setterMethods;

    protected Pusher() {}

    public Pusher(String typeName, String fieldNamesDelimited) throws AmicusException {
        this.typeName = typeName;
        setterMethods = new ArrayList<>();
        try {
            Class<? extends Annotation> annotationClass = getClassFromName(typeName);
            annotationConstructor = getAnnotationConstructor(annotationClass);
            fieldNames = fieldNamesDelimited == null ? new ArrayList<String>() :
                    Arrays.asList(fieldNamesDelimited.split(Amicus.ANNOTATION_FIELD_DELIMITER, -1));
            setterMethods = new ArrayList<>();
            for (String f : fieldNames) {
                if ("".equals(f)) {
                    setterMethods.add(null);
                } else {
                    setterMethods.add(getSetterForField(annotationClass, f));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AmicusException(e);
        }
    }

    /**
     * ...todo: doc
     * Overriding methods might not call this.
     * @param jCas
     * @param ana
     */
    public void push(JCas jCas, ANA<Object> ana) throws AmicusException {
        if (setterMethods == null) {
            throw new AmicusException("Need to provide setters for included Pusher implementations; " +
                    "check configuration.");
        }
        Annotation annotation;
        try {
            annotation = annotationConstructor.newInstance(jCas, ana.getBegin(), ana.getEnd());
        } catch (ReflectiveOperationException e) {
            throw new AmicusException(e);
        }
        Object value = ana.getValue();
        if (setterMethods.size() > 1) {
            List toSet = value instanceof List ? (List) value : buildListFromString(value.toString());
            if (toSet.size() != setterMethods.size()) {
                throw new AmicusException("Different number of values and setters for \"%s\". Check configuration.",
                        typeName);
            }
            for (int i=0; i<setterMethods.size(); i++) {
                if (setterMethods.get(i) != null) {
                    try {
                        setterMethods.get(i).invoke(annotation, toSet.get(i));
                    } catch (InvocationTargetException | IllegalAccessException e) {
                        throw new AmicusException(e);
                    }
                }
            }
        } else {
            Object toSet = value instanceof List ? buildStringFromList((List) value) : value;
            try {
                setterMethods.get(0).invoke(annotation, toSet);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new AmicusException(e);
            }
        }
        annotation.addToIndexes();
    }


    // utility methods for subclasses to use

    protected static Class<? extends Annotation> getClassFromName(String name) throws ClassNotFoundException {
        return (Class<? extends Annotation>) Class.forName(name);
    }

    protected static Constructor<? extends Annotation> getAnnotationConstructor(Class<? extends Annotation> annotationClass) throws NoSuchMethodException {
        return annotationClass.getConstructor(JCas.class, int.class, int.class);
    }

    protected static Method getSetterForField(Class clazz, String name) throws NoSuchMethodException {
        String setterName = Util.getSetterFor(name);
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(setterName) && method.getParameters().length == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException(setterName);
    }

    // static methods

    public static String buildStringFromList(List list) {
        if (list.size() == 0) return "";
        StringBuilder builder = new StringBuilder();
        builder.append(list.get(0));
        for (int i=1; i<list.size(); i++) {
            builder.append(Amicus.LIST_AS_STRING_DELIMITER).append(list.get(i));
        }
        return builder.toString();
    }

    public static List<String> buildListFromString(String string) {
        return Arrays.asList(string.split(Pattern.quote(Amicus.LIST_AS_STRING_DELIMITER), -1));
    }

}
