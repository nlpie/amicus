package edu.umn.amicus.pullers;

import org.apache.uima.jcas.tcas.Annotation;
import java.util.ArrayList;
import java.util.List;
import org.apache.uima.jcas.JCas;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.tcas.Annotation;
import edu.umn.amicus.AmicusException;


/**
 * This is a true null Puller: it doesn't even call the getter, just passes the Annotation object through.
 * This could be used if your Distiller class uses annotation-specific logic.
 * (A more elegant approach for some situations may be to simply access the same annotation type multiple times,
 * with a different getter each time.)
 * Created by gpfinley on 12/19/16.
 */
public class PassthroughPuller extends Puller {

    public PassthroughPuller() {
    }

    @Override
    public Object pull(Annotation annotation) throws AmicusException {
	try {
	    Type t = annotation.getType();
	    Class<? extends Annotation> annotationClass = getClassFromName(t.getName());
	    List<Object> objectList = new ArrayList<>();

	    for (Feature f : t.getFeatures()) {
		String name = f.getShortName();
		if ("".equals(name)) {
		    objectList.add(null);
		} else {
		    if(!"sofa".equals(name)){
			objectList.add(callThisGetter(name, annotation));
		    }
		}
	    }
	    return objectList;
	} catch (ReflectiveOperationException e) {
	    throw new AmicusException(e);
	}

    }

}
