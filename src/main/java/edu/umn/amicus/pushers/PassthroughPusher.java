package edu.umn.amicus.pushers;

import edu.umn.amicus.util.ANA;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.cas.FSArray;
import edu.umn.amicus.AmicusException;

import java.util.ArrayList;
import java.util.List;


/**
 * Set an Annotation as pulled from another CAS. Effectively a copier when using PassthroughPuller.
 *
 * Created by greg on 2/24/17.
 */
public class PassthroughPusher extends Pusher {

    public PassthroughPusher(String typeName, String fieldNamesDelimited) throws AmicusException {
	super(typeName, null);
    }

    public void push(JCas jCas, ANA<Object> ana) throws AmicusException {

	try {
	    setterMethods = new ArrayList<>();

	    Annotation annotation = annotationConstructor.newInstance(jCas, ana.getBegin(), ana.getEnd());
	    Type t = annotation.getType();
	    Class<? extends Annotation> annotationClass = getClassFromName(t.getName());
            for (Feature f : t.getFeatures()) {
		String name = f.getShortName();
                if ("".equals(name)) {
                    setterMethods.add(null);
                } else {
		    if(!"sofa".equals(name)){
			setterMethods.add(getSetterForField(annotationClass, name));
		    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AmicusException(e);
        }

        super.push(jCas, ana);

        // todo: test!!! does this actually copy to another jCas, or do we need to do something else??
        // ((Annotation) ana.getValue()).addToIndexes(jCas);
    }

}
